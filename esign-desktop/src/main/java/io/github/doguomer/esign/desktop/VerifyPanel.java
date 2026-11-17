package io.github.doguomer.esign.desktop;

import io.github.doguomer.esign.validation.Verdict;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** "Doğrula" sekmesi. */
final class VerifyPanel extends JPanel {

    private final DesktopService service;
    private final JTextField file = new JTextField(28);
    private final JTextField original = new JTextField(28);
    private final JTextField trust = new JTextField(28);
    private final JCheckBox offline = new JCheckBox("Çevrim dışı (OCSP/CRL sorgulama)");
    private final JLabel verdict = new JLabel(" ");
    private final JTextArea result = new JTextArea(14, 40);
    private final JButton verifyButton = new JButton("Doğrula");

    VerifyPanel(DesktopService service) {
        super(new BorderLayout(8, 8));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String envTrust = System.getenv("ESIGN_TRUST_DIR");
        if (envTrust != null) {
            trust.setText(envTrust.strip());
        }
        JPanel form = new JPanel(new GridBagLayout());
        Ui.add(form, 0, "İmzalı dosya (.imz, .p7s, PDF, XML):", file, Ui.chooser(this, file, false));
        Ui.add(form, 1, "Orijinal dosya (yalnızca ayrık .p7s):", original, Ui.chooser(this, original, false));
        Ui.add(form, 2, "Güvenilir kökler (dosya/klasör):", trust, Ui.chooser(this, trust, true));
        Ui.add(form, 3, "", offline, null);
        add(form, BorderLayout.NORTH);

        verdict.setFont(verdict.getFont().deriveFont(Font.BOLD, 16f));
        result.setEditable(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(verdict, BorderLayout.NORTH);
        center.add(new JScrollPane(result), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(verifyButton);
        add(buttons, BorderLayout.SOUTH);
        verifyButton.addActionListener(e -> start());
    }

    private void start() {
        Optional<Path> signed = Ui.path(file);
        if (signed.isEmpty() || !Files.isRegularFile(signed.get())) {
            JOptionPane.showMessageDialog(this, "İmzalı dosyayı seçin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Optional<Path> orig = Ui.path(original);
        Optional<Path> anchors = Ui.path(trust);
        boolean off = offline.isSelected();
        verifyButton.setEnabled(false);
        verdict.setText("Doğrulanıyor…");
        verdict.setForeground(Color.DARK_GRAY);
        result.setText("");
        new SwingWorker<DesktopService.VerifyOutcome, Void>() {
            @Override
            protected DesktopService.VerifyOutcome doInBackground() throws Exception {
                return service.verify(signed.get(), orig, anchors, off);
            }

            @Override
            protected void done() {
                verifyButton.setEnabled(true);
                try {
                    var o = get();
                    Verdict v = o.report().overall();
                    verdict.setText("Sonuç: " + v.label());
                    verdict.setForeground(switch (v) {
                        case VALID -> new Color(0x1B7F3B);
                        case INDETERMINATE -> new Color(0xB26A00);
                        case INVALID -> new Color(0xB00020);
                    });
                    result.setText(o.text());
                    result.setCaretPosition(0);
                } catch (Exception ex) {
                    verdict.setText("Doğrulanamadı");
                    verdict.setForeground(new Color(0xB00020));
                    result.setText(SignPanel.cause(ex));
                }
            }
        }.execute();
    }
}
