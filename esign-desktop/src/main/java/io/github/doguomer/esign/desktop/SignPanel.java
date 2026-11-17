package io.github.doguomer.esign.desktop;

import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.pkcs11.Pkcs11Config;
import io.github.doguomer.esign.pkcs11.Pkcs11LibraryLocator;
import io.github.doguomer.esign.pkcs11.Pkcs11SigningToken;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/** "İmzala" sekmesi: dosya, kart, biçim, profil, PIN, imza. */
final class SignPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DesktopService service;
    private final JTextField file = new JTextField(28);
    private final JTextField library = new JTextField(28);
    private final JSpinner slot = new JSpinner(new SpinnerNumberModel(0, 0, 16, 1));
    private final JComboBox<SignOptions.Format> format = new JComboBox<>(SignOptions.Format.values());
    private final JComboBox<String> profile = new JComboBox<>(new String[] {
        "BES — temel", "T — zaman damgalı", "LT — uzun dönem (OCSP/CRL)", "LTA — arşiv"});
    private final JCheckBox detached = new JCheckBox("Ayrık imza (.p7s, yalnızca CAdES)");
    private final JCheckBox visible = new JCheckBox("Görünür imza kutusu (yalnızca PDF)");
    private final JTextField reason = new JTextField(28);
    private final JTextField tsa = new JTextField(28);
    private final JTextField trust = new JTextField(28);
    private final JTextArea log = new JTextArea(8, 40);
    private final JButton signButton = new JButton("İmzala");
    private final JButton certsButton = new JButton("Karttaki sertifikalar");

    SignPanel(DesktopService service) {
        super(new BorderLayout(8, 8));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // Son kullanılan kütüphane yolu hatırlanır (Windows'ta kayıt defteri, Linux/macOS'ta kullanıcı tercihleri);
        // yoksa ESIGN_PKCS11_LIB veya bilinen AKİS yolları. Remembers the last library path.
        String remembered = prefs().get(PREF_LIB, "");
        if (!remembered.isBlank() && Files.isRegularFile(Path.of(remembered))) {
            library.setText(remembered);
        } else {
            new Pkcs11LibraryLocator().locate().ifPresent(p -> library.setText(p.toString()));
        }
        String envTsa = System.getenv("ESIGN_TSA_URL");
        if (envTsa != null) {
            tsa.setText(envTsa.strip());
        }
        String envTrust = System.getenv("ESIGN_TRUST_DIR");
        if (envTrust != null) {
            trust.setText(envTrust.strip());
        }

        JPanel form = new JPanel(new GridBagLayout());
        int r = 0;
        Ui.add(form, r++, "İmzalanacak dosya:", file, Ui.chooser(this, file, false));
        Ui.add(form, r++, "Kart kütüphanesi (PKCS#11):", library, Ui.chooser(this, library, false));
        Ui.add(form, r++, "Kart okuyucu sırası (slot):", slot, null);
        Ui.add(form, r++, "Biçim:", format, null);
        Ui.add(form, r++, "Profil:", profile, null);
        Ui.add(form, r++, "", detached, null);
        Ui.add(form, r++, "", visible, null);
        Ui.add(form, r++, "İmza nedeni (PDF):", reason, null);
        Ui.add(form, r++, "Zaman damgası adresi (T/LT/LTA):", tsa, null);
        Ui.add(form, r++, "Güvenilir kökler (LT/LTA):", trust, Ui.chooser(this, trust, true));
        add(form, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(certsButton);
        buttons.add(signButton);
        add(buttons, BorderLayout.SOUTH);

        signButton.addActionListener(e -> startSigning());
        certsButton.addActionListener(e -> showCertificates());
        say("Kartı takın, dosyayı seçin ve \"İmzala\"ya basın. PIN yalnızca kart için kullanılır, saklanmaz.");
    }

    private void say(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private SignatureProfile selectedProfile() {
        return SignatureProfile.values()[profile.getSelectedIndex()];
    }

    private Path libraryPath() {
        Path p = Ui.path(library).orElseThrow(() -> new IllegalArgumentException(
                "Kart kütüphanesini seçin (AKİS: akisp11.dll / libakisp11.so / libakisp11.dylib)."));
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("Kart kütüphanesi bulunamadı: " + p);
        }
        return p;
    }

    private void showCertificates() {
        showCertificates(null);
    }

    /** Bazı kartlar sertifikaları yalnızca PIN ile gösterir; o durumda PIN sorup yeniden dener. */
    private void showCertificates(char[] pin) {
        Path lib;
        try {
            lib = libraryPath();
            rememberLibrary(lib);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            return;
        }
        int s = (Integer) slot.getValue();
        busy(true);
        new SwingWorker<List<CertificateInfo>, Void>() {
            @Override
            protected List<CertificateInfo> doInBackground() throws Exception {
                try (var t = Pkcs11SigningToken.open(new Pkcs11Config(lib, s), pin)) {
                    return t.listCertificates();
                } finally {
                    if (pin != null) {
                        Arrays.fill(pin, '\0');
                    }
                }
            }

            @Override
            protected void done() {
                busy(false);
                try {
                    List<CertificateInfo> list = get();
                    say(list.isEmpty() ? "Kartta sertifika bulunamadı." : "Karttaki sertifikalar:");
                    list.forEach(c -> say("  • " + describe(c)));
                } catch (Exception ex) {
                    String m = cause(ex);
                    if (pin == null && m.contains("--login")) {
                        say("Bu kart sertifikaları göstermek için PIN istiyor.");
                        char[] p = askPin();
                        if (p != null) {
                            showCertificates(p);
                        }
                        return;
                    }
                    error(m);
                }
            }
        }.execute();
    }

    private static final String PREF_LIB = "pkcs11Library";

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(SignPanel.class);
    }

    private static void rememberLibrary(Path lib) {
        try {
            prefs().put(PREF_LIB, lib.toAbsolutePath().toString());
        } catch (RuntimeException ignored) {
            // tercih deposu yazılamazsa imza yine sürer / best effort
        }
    }

    private void startSigning() {
        SignOptions o;
        Path lib;
        try {
            Path in = Ui.path(file).orElseThrow(() -> new IllegalArgumentException("İmzalanacak dosyayı seçin."));
            if (!Files.isRegularFile(in)) {
                throw new IllegalArgumentException("Dosya bulunamadı: " + in);
            }
            o = new SignOptions(in, (SignOptions.Format) format.getSelectedItem(), selectedProfile(),
                    detached.isSelected(), visible.isSelected(), reason.getText(), tsa.getText(), Ui.path(trust));
            service.settings(o);
            lib = libraryPath();
        } catch (Exception ex) {
            error(cause(ex));
            return;
        }
        char[] pin = askPin();
        if (pin == null) {
            return;
        }
        int s = (Integer) slot.getValue();
        busy(true);
        say("Kart açılıyor…");
        new SwingWorker<Path, String>() {
            @Override
            protected Path doInBackground() throws Exception {
                try (var token = Pkcs11SigningToken.open(new Pkcs11Config(lib, s), pin)) {
                    List<CertificateInfo> c = service.signingCandidates(token);
                    if (c.isEmpty()) {
                        throw new IllegalStateException("Kartta imzaya uygun (nitelikli, süresi geçerli) sertifika yok.");
                    }
                    CertificateInfo chosen = c.size() == 1 ? c.get(0) : choose(c);
                    if (chosen == null) {
                        return null;
                    }
                    publish("Sertifika: " + describe(chosen));
                    publish("İmzalanıyor (" + o.effectiveFormat().name() + "-" + o.profile().label() + ")…");
                    return service.sign(token, chosen.alias(), o);
                } finally {
                    Arrays.fill(pin, '\0');
                }
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(SignPanel.this::say);
            }

            @Override
            protected void done() {
                busy(false);
                try {
                    Path out = get();
                    if (out == null) {
                        say("İptal edildi.");
                        return;
                    }
                    say("✔ İmzalandı: " + out);
                    JOptionPane.showMessageDialog(SignPanel.this, "İmzalı dosya kaydedildi:\n" + out,
                            "İmza tamamlandı", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    error(cause(ex));
                }
            }
        }.execute();
    }

    /** Arka plan iş parçacığından EDT'de sertifika seçimi ister. */
    private CertificateInfo choose(List<CertificateInfo> list) throws Exception {
        String[] labels = list.stream().map(SignPanel::describe).toArray(String[]::new);
        Object[] picked = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> picked[0] = JOptionPane.showInputDialog(this,
                "Kullanılacak imza sertifikasını seçin:", "Sertifika seçimi", JOptionPane.QUESTION_MESSAGE,
                null, labels, labels[0]));
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(picked[0])) {
                return list.get(i);
            }
        }
        return null;
    }

    private char[] askPin() {
        JPasswordField pf = new JPasswordField(12);
        int r = JOptionPane.showConfirmDialog(this, new Object[] {"Kart PIN'i:", pf}, "PIN",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        char[] pin = pf.getPassword();
        pf.setText("");
        if (r != JOptionPane.OK_OPTION || pin.length == 0) {
            Arrays.fill(pin, '\0');
            return null;
        }
        return pin;
    }

    static String describe(CertificateInfo c) {
        return cn(c.subject()) + " — " + cn(c.issuer()) + " — son geçerlilik "
                + DATE.format(c.notAfter().atZone(ZoneId.systemDefault()))
                + (c.isSigningCandidate() ? " (imza)" : "");
    }

    static String cn(String dn) {
        var m = java.util.regex.Pattern.compile("(?:^|,)\\s*CN=((?:\\\\.|[^,])+)").matcher(dn);
        return m.find() ? m.group(1).replace("\\,", ",").strip() : dn;
    }

    private void busy(boolean b) {
        signButton.setEnabled(!b);
        certsButton.setEnabled(!b);
        setCursor(b ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR) : null);
    }

    private void error(String msg) {
        say("✖ " + msg);
        JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
    }

    static String cause(Throwable t) {
        Throwable c = t;
        while ((c instanceof java.util.concurrent.ExecutionException || c.getMessage() == null) && c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }
}
