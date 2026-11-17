package io.github.doguomer.esign.desktop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.dss.testing.TestPki;
import io.github.doguomer.esign.validation.Verdict;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Masaüstü iş mantığı: imzala → doğrula, çıktı adları, ayar denetimi. */
class DesktopServiceTest {

    private static TestPki pki;
    private final DesktopService service = new DesktopService();
    @TempDir Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        pki = new TestPki();
    }

    private TrustSettings trust() {
        return TrustSettings.of(List.of(pki.root), null, pki.crlSource(), null);
    }

    private Path write(String name, byte[] content) throws Exception {
        Path p = dir.resolve(name);
        Files.write(p, content);
        return p;
    }

    private static SignOptions opts(Path in, SignOptions.Format f, boolean detached, boolean visible) {
        return new SignOptions(in, f, SignatureProfile.B, detached, visible, "Onay", "", Optional.empty());
    }

    @Test
    void signsTextAsCadesAndVerifies() throws Exception {
        Path in = write("rapor.txt", "Merhaba".getBytes(StandardCharsets.UTF_8));
        Path out = service.sign(pki.token(), TestPki.ALIAS, opts(in, SignOptions.Format.OTOMATIK, false, false));
        assertEquals("rapor.txt.imz", out.getFileName().toString());
        var r = service.verify(out, Optional.empty(), trust());
        assertEquals(Verdict.VALID, r.report().overall());
        assertTrue(r.text().contains("GEÇERLİ"));
    }

    @Test
    void detachedNeedsOriginal() throws Exception {
        Path in = write("veri.bin", new byte[] {1, 2, 3});
        Path out = service.sign(pki.token(), TestPki.ALIAS, opts(in, SignOptions.Format.CADES, true, false));
        assertEquals("veri.bin.p7s", out.getFileName().toString());
        assertEquals(Verdict.VALID, service.verify(out, Optional.of(in), trust()).report().overall());
    }

    @Test
    void visiblePdfWithTimestamp() throws Exception {
        byte[] pdf;
        try (PDDocument d = new PDDocument(); var o = new ByteArrayOutputStream()) {
            d.addPage(new PDPage());
            d.save(o);
            pdf = o.toByteArray();
        }
        Path in = write("sozlesme.pdf", pdf);
        var o = new SignOptions(in, SignOptions.Format.OTOMATIK, SignatureProfile.T, false, true, "Onay", null,
                Optional.empty());
        var settings = new SignatureSettings(SignatureProfile.T, Timestamping.of(pki.tsa, "test"), TrustSettings.none());
        Path out = service.sign(pki.token(), TestPki.ALIAS, o, settings);
        assertEquals("sozlesme_imzali.pdf", out.getFileName().toString());
        var r = service.verify(out, Optional.empty(), trust());
        assertEquals(Verdict.VALID, r.report().overall());
        assertTrue(r.report().signatures().get(0).timestampTime().isPresent());
    }

    @Test
    void xmlAsXades() throws Exception {
        Path in = write("yazi.xml", "<a>ğüş</a>".getBytes(StandardCharsets.UTF_8));
        Path out = service.sign(pki.token(), TestPki.ALIAS, opts(in, SignOptions.Format.OTOMATIK, false, false));
        assertEquals("yazi_imzali.xml", out.getFileName().toString());
        assertEquals(Verdict.VALID, service.verify(out, Optional.empty(), trust()).report().overall());
    }

    @Test
    void neverOverwrites() throws Exception {
        Path in = write("a.txt", new byte[] {1});
        write("a.txt.imz", new byte[] {9});
        Path out = service.sign(pki.token(), TestPki.ALIAS, opts(in, SignOptions.Format.OTOMATIK, false, false));
        assertEquals("a.txt (2).imz", out.getFileName().toString());
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(dir.resolve("a.txt.imz")));
    }

    @Test
    void invalidCombinationsAreRejectedBeforePin() throws Exception {
        Path txt = write("b.txt", new byte[] {1});
        Path pdf = write("b.pdf", new byte[] {1});
        assertThrows(IllegalArgumentException.class, () -> opts(txt, SignOptions.Format.OTOMATIK, false, true));
        assertThrows(IllegalArgumentException.class, () -> opts(pdf, SignOptions.Format.OTOMATIK, true, false));
        var t = new SignOptions(txt, null, SignatureProfile.T, false, false, null, "", Optional.empty());
        var e = assertThrows(IllegalArgumentException.class, () -> service.settings(t));
        assertTrue(e.getMessage().contains("zaman damgası adresi"), e.getMessage());
        var lt = new SignOptions(txt, null, SignatureProfile.LT, false, false, null, "http://tsa", Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.settings(lt));
    }

    @Test
    void commonNameFromDn() {
        assertEquals("AHMET YILMAZ", SignPanel.cn("SERIALNUMBER=123,CN=AHMET YILMAZ,C=TR"));
        assertEquals("A, B", SignPanel.cn("CN=A\\, B,O=X"));
        assertEquals("O=X", SignPanel.cn("O=X"));
    }
}
