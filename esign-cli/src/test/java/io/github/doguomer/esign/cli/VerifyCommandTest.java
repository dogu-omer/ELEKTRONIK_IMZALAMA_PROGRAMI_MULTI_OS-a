package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.cades.CadesBesSigner;
import io.github.doguomer.esign.cades.CadesPackaging;
import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.dss.testing.TestPki;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** esign verify uçtan uca: test PKI ile imzala, CLI ile doğrula. */
class VerifyCommandTest {

    private static TestPki pki;
    @TempDir Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        pki = new TestPki();
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(List.of(args), new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private Path rootPem() throws Exception {
        Path p = dir.resolve("kok.pem");
        Files.writeString(p, "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder().encodeToString(pki.root.getEncoded())
                + "\n-----END CERTIFICATE-----\n");
        return p;
    }

    private Path signedFile(CadesPackaging pack, byte[] content) throws Exception {
        return signedFile(SignatureProfile.T, pack, content);
    }

    private Path signedFile(SignatureProfile p, CadesPackaging pack, byte[] content) throws Exception {
        TrustSettings trust = p.needsRevocationData()
                ? TrustSettings.of(List.of(pki.root), null, pki.crlSource(), null) : TrustSettings.none();
        var settings = new SignatureSettings(p, Timestamping.of(pki.tsa, "test"), trust);
        byte[] sig = new CadesBesSigner(pki.token(), settings).sign(content, "belge.txt", TestPki.ALIAS, pack);
        Path out = dir.resolve(pack == CadesPackaging.DETACHED ? "belge.txt.p7s" : "belge.txt.imz");
        Files.write(out, sig);
        return out;
    }

    @Test
    void ltSignatureValidatesOfflineExitsZeroAndWritesReport() throws Exception {
        Path signed = signedFile(SignatureProfile.LT, CadesPackaging.ATTACHED, "merhaba".getBytes(StandardCharsets.UTF_8));
        Path report = dir.resolve("rapor.xml");
        int rc = run("verify", "--in", signed.toString(), "--trust", rootPem().toString(), "--offline",
                "--report", report.toString());
        String text = out.toString(StandardCharsets.UTF_8);
        assertEquals(0, rc, text + err.toString(StandardCharsets.UTF_8));
        assertTrue(text.contains("GEÇERLİ / valid"));
        assertTrue(text.contains("CAdES-BASELINE-LT"));
        assertTrue(Files.readString(report).contains("DetailedReport"));
    }

    @Test
    void offlineTSignatureWithoutRevocationIsIndeterminate() throws Exception {
        Path signed = signedFile(CadesPackaging.ATTACHED, "merhaba".getBytes(StandardCharsets.UTF_8));
        assertEquals(3, run("verify", "--in", signed.toString(), "--trust", rootPem().toString(), "--offline"));
    }

    @Test
    void withoutTrustAnchorIsIndeterminate() throws Exception {
        Path signed = signedFile(CadesPackaging.ATTACHED, "merhaba".getBytes(StandardCharsets.UTF_8));
        int rc = run("verify", "--in", signed.toString(), "--offline");
        assertEquals(3, rc);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("güvenilir kök verilmedi"));
    }

    @Test
    void tamperedDetachedOriginalIsInvalid() throws Exception {
        Path signed = signedFile(CadesPackaging.DETACHED, "asıl metin".getBytes(StandardCharsets.UTF_8));
        Path changed = Files.writeString(dir.resolve("belge.txt"), "değişmiş metin");
        int rc = run("verify", "--in", signed.toString(), "--original", changed.toString(),
                "--trust", rootPem().toString(), "--offline");
        assertEquals(4, rc, out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void unsignedFileIsAnError() throws Exception {
        Path plain = Files.writeString(dir.resolve("duz.txt"), "imzasız");
        assertEquals(1, run("verify", "--in", plain.toString(), "--offline"));
    }

    @Test
    void existingReportIsNotOverwritten() throws Exception {
        Path signed = signedFile(CadesPackaging.ATTACHED, "x".getBytes(StandardCharsets.UTF_8));
        Path report = Files.writeString(dir.resolve("var.xml"), "eski");
        assertEquals(1, run("verify", "--in", signed.toString(), "--report", report.toString(), "--offline"));
        assertEquals("eski", Files.readString(report));
    }

    @Test
    void levelDefaultsFollowTsaAndLtNeedsTrust() throws Exception {
        var bes = Main.signatureSettings(CliArgs.parse(List.of("sign", "--in", "a.txt")), Timestamping.none(), null);
        assertEquals(SignatureProfile.B, bes.profile());
        var t = Main.signatureSettings(CliArgs.parse(List.of("sign", "--in", "a.txt")),
                Timestamping.of(pki.tsa, "test"), null);
        assertEquals(SignatureProfile.T, t.profile());
        assertThrows(IllegalArgumentException.class, () -> Main.signatureSettings(
                CliArgs.parse(List.of("sign", "--in", "a.txt", "--level", "lt")), Timestamping.of(pki.tsa, "x"), null));
        assertThrows(IllegalArgumentException.class, () -> Main.signatureSettings(
                CliArgs.parse(List.of("sign", "--in", "a.txt", "--level", "lt", "--offline", "--trust",
                        rootPem().toString())), Timestamping.of(pki.tsa, "x"), null));
        var lt = Main.signatureSettings(CliArgs.parse(List.of("sign", "--in", "a.txt", "--level", "LT", "--trust",
                rootPem().toString())), Timestamping.of(pki.tsa, "x"), null);
        assertEquals(SignatureProfile.LT, lt.profile());
        assertEquals(1, lt.trust().anchors().size());
    }

    @Test
    void trustFallsBackToEnvironment() throws Exception {
        assertTrue(Main.trust(Optional.empty(), null, true).anchors().isEmpty());
        assertEquals(1, Main.trust(Optional.empty(), rootPem().toString(), true).anchors().size());
    }
}
