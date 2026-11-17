package io.github.doguomer.esign.desktop;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Türkçe masaüstü arayüzü (Linux ve macOS). Turkish desktop UI for Linux and macOS. */
public final class DesktopApp {

    private DesktopApp() {}

    public static void main(String[] args) {
        launch();
    }

    /** Grafik ortam yoksa false döner. */
    public static boolean launch() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        System.setProperty("apple.awt.application.name", "esign");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Varsayılan görünüm kullanılır.
            }
            JFrame f = new JFrame("esign — Elektronik İmza");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            DesktopService service = new DesktopService();
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("İmzala", new SignPanel(service));
            tabs.addTab("Doğrula", new VerifyPanel(service));
            tabs.addTab("Hakkında", about());
            f.setContentPane(tabs);
            f.setMinimumSize(new Dimension(760, 560));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
        return true;
    }

    static String version() {
        String v = DesktopApp.class.getPackage().getImplementationVersion();
        return v == null ? "geliştirme" : v;
    }

    private static JScrollPane about() {
        JEditorPane p = new JEditorPane("text/html", """
                <html><body style='font-family:sans-serif; padding:8px'>
                <h2>esign %s</h2>
                <p>AKİS ve diğer PKCS#11 kartlarıyla CAdES, PAdES ve XAdES elektronik imza ve doğrulama.
                İmza motoru: EU DSS (Avrupa Komisyonu).</p>
                <ul>
                <li>Özel anahtar kartın dışına çıkmaz; PIN saklanmaz ve kullanımdan sonra bellekten silinir.</li>
                <li>Hiçbir dosyanın üzerine yazılmaz; imzalı dosya yeni adla kaydedilir.</li>
                <li>Güvenli elektronik imzanın hukuki geçerliliği (5070 sayılı Kanun) nitelikli sertifika ve
                güvenli imza oluşturma aracı gerektirir; bu yazılım BTK veya TÜBİTAK onaylı değildir.</li>
                <li>İmzager ile karşılıklı uyumluluk testleri henüz tamamlanmadı.</li>
                </ul>
                <p>Lisans: GNU GPL-3.0-or-later. Kaynak kodu:
                github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a</p>
                </body></html>""".formatted(version()));
        p.setEditable(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        return new JScrollPane(p);
    }
}
