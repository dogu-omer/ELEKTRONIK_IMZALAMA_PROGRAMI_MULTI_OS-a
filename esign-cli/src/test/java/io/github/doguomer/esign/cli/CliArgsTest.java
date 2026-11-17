package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliArgsTest {

    @Test
    void parsesListCerts() {
        var a = CliArgs.parse(List.of("list-certs", "--lib", "/x.so", "--slot", "2", "--login"));
        assertEquals("list-certs", a.command());
        assertEquals(Path.of("/x.so"), a.library().orElseThrow());
        assertEquals(2, a.slot());
        assertTrue(a.login());
    }

    @Test
    void emptyMeansHelp() {
        assertEquals("help", CliArgs.parse(List.of()).command());
    }

    @Test
    void pinArgumentIsRejected() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(List.of("list-certs", "--pin", "1234")));
        assertFalse(e.getMessage().contains("1234"));
    }

    @Test
    void badSlotRejected() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("list-certs", "--slot", "-1")));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("list-certs", "--slot")));
    }

    @Test
    void signDefaultsToImzOutput() {
        var a = CliArgs.parse(List.of("sign", "--in", "/tmp/rapor.docx"));
        assertEquals(Path.of("/tmp/rapor.docx.imz"), a.outputPath());
    }

    @Test
    void detachedDefaultsToP7s() {
        var a = CliArgs.parse(List.of("sign", "--in", "/tmp/a.xlsx", "--detached"));
        assertEquals(Path.of("/tmp/a.xlsx.p7s"), a.outputPath());
    }

    @Test
    void signRequiresInput() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("sign")));
    }

    @Test
    void pdfDefaultsToPades() {
        var a = CliArgs.parse(List.of("sign", "--in", "/tmp/Sozlesme.PDF"));
        assertEquals(SignFormat.PADES, a.effectiveFormat());
        assertEquals(Path.of("/tmp/Sozlesme_imzali.pdf"), a.outputPath());
    }

    @Test
    void xmlDefaultsToXades() {
        var a = CliArgs.parse(List.of("sign", "--in", "/tmp/yazi.xml"));
        assertEquals(SignFormat.XADES, a.effectiveFormat());
        assertEquals(Path.of("/tmp/yazi_imzali.xml"), a.outputPath());
    }

    @Test
    void pdfCanBeForcedToCades() {
        var a = CliArgs.parse(List.of("sign", "--in", "/tmp/a.pdf", "--format", "cades"));
        assertEquals(Path.of("/tmp/a.pdf.imz"), a.outputPath());
    }

    @Test
    void detachedOnlyForCades() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("sign", "--in", "/tmp/a.pdf", "--detached")));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("sign", "--in", "/tmp/a", "--format", "zip")));
    }
}
