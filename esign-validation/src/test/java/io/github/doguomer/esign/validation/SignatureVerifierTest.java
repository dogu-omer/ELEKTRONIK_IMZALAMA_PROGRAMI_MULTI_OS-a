package io.github.doguomer.esign.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.cades.CadesBesSigner;
import io.github.doguomer.esign.cades.CadesPackaging;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.dss.testing.TestPki;
import io.github.doguomer.esign.pades.PadesBesSigner;
import io.github.doguomer.esign.xades.XadesBesSigner;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SignatureVerifierTest {

    private static final byte[] CONTENT = "Doğrulama testi / validation test".getBytes(StandardCharsets.UTF_8);
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><yazi><metin>Deneme</metin></yazi>";
    private static TestPki pki;

    @BeforeAll
    static void pki() throws Exception {
        pki = new TestPki();
    }

    private static TrustSettings trusted() {
        return TrustSettings.of(List.of(pki.root), null, pki.crlSource(), null);
    }

    private static SignatureSettings settings(SignatureProfile p) {
        return new SignatureSettings(p, p.needsTimestamp() ? Timestamping.of(pki.tsa, "test") : Timestamping.none(),
                trusted());
    }

    private static byte[] cades(SignatureProfile p, CadesPackaging pack) throws TokenException {
        return new CadesBesSigner(pki.token(), settings(p)).sign(CONTENT, "belge.txt", TestPki.ALIAS, pack);
    }

    private static SignatureResult only(VerificationReport r) {
        assertEquals(1, r.signatures().size());
        return r.signatures().get(0);
    }

    @Test
    void trustedBesSignatureIsValid() throws Exception {
        var r = new SignatureVerifier(trusted()).verify(cades(SignatureProfile.B, CadesPackaging.ATTACHED),
                "belge.txt.imz", null, null);
        SignatureResult s = only(r);
        assertEquals(Verdict.VALID, s.verdict(), () -> s.indication() + " " + s.subIndication() + " " + s.errors());
        assertEquals(Verdict.VALID, r.overall());
        assertTrue(s.signedBy().contains("TEST IMZACI"));
        assertEquals("CAdES-BASELINE-B", s.format());
        assertTrue(s.timestampTime().isEmpty());
        assertTrue(r.detailedReportXml().contains("DetailedReport"));
    }

    @Test
    void unknownRootIsIndeterminate() throws Exception {
        var r = new SignatureVerifier(TrustSettings.none()).verify(cades(SignatureProfile.B, CadesPackaging.ATTACHED),
                "belge.txt.imz", null, null);
        SignatureResult s = only(r);
        assertEquals(Verdict.INDETERMINATE, s.verdict());
        assertEquals("NO_CERTIFICATE_CHAIN_FOUND", s.subIndication().orElseThrow());
    }

    @Test
    void tamperedDetachedContentIsInvalid() throws Exception {
        byte[] p7s = cades(SignatureProfile.B, CadesPackaging.DETACHED);
        var ok = new SignatureVerifier(trusted()).verify(p7s, "belge.txt.p7s", CONTENT, "belge.txt");
        assertEquals(Verdict.VALID, only(ok).verdict());

        var bad = new SignatureVerifier(trusted()).verify(p7s, "belge.txt.p7s",
                "değiştirilmiş".getBytes(StandardCharsets.UTF_8), "belge.txt");
        SignatureResult s = only(bad);
        assertEquals(Verdict.INVALID, s.verdict());
        assertEquals("HASH_FAILURE", s.subIndication().orElseThrow());
    }

    @Test
    void timestampedSignatureReportsProofOfExistence() throws Exception {
        var r = new SignatureVerifier(trusted()).verify(cades(SignatureProfile.T, CadesPackaging.ATTACHED),
                "belge.txt.imz", null, null);
        SignatureResult s = only(r);
        assertEquals(Verdict.VALID, s.verdict(), () -> s.subIndication() + " " + s.errors());
        assertEquals("CAdES-BASELINE-T", s.format());
        assertEquals(1, s.timestampCount());
        assertTrue(s.timestampTime().isPresent());
    }

    @Test
    void ltEmbedsRevocationDataAndValidatesOffline() throws Exception {
        byte[] lt = cades(SignatureProfile.LT, CadesPackaging.ATTACHED);
        // Çevrimdışı doğrulama: iptal verisi imzanın içinden okunmalı
        var r = new SignatureVerifier(TrustSettings.of(List.of(pki.root), false)).verify(lt, "b.imz", null, null);
        SignatureResult s = only(r);
        assertEquals("CAdES-BASELINE-LT", s.format());
        assertEquals(Verdict.VALID, s.verdict(), () -> s.subIndication() + " " + s.errors());
    }

    @Test
    void ltaAddsArchiveTimestamp() throws Exception {
        byte[] lta = cades(SignatureProfile.LTA, CadesPackaging.ATTACHED);
        var r = new SignatureVerifier(TrustSettings.of(List.of(pki.root), false)).verify(lta, "b.imz", null, null);
        SignatureResult s = only(r);
        assertEquals("CAdES-BASELINE-LTA", s.format());
        assertEquals(Verdict.VALID, s.verdict(), () -> s.subIndication() + " " + s.errors());
        assertTrue(s.timestampCount() >= 1);
    }

    @Test
    void padesAndXadesLtaAreProducedAndValid() throws Exception {
        byte[] pdf;
        try (PDDocument d = new PDDocument(); var out = new ByteArrayOutputStream()) {
            d.addPage(new PDPage());
            d.save(out);
            pdf = out.toByteArray();
        }
        byte[] signedPdf = new PadesBesSigner(pki.token(), settings(SignatureProfile.LTA))
                .sign(pdf, "b.pdf", TestPki.ALIAS, null);
        SignatureResult p = only(new SignatureVerifier(trusted()).verify(signedPdf, "b.pdf", null, null));
        assertEquals("PAdES-BASELINE-LTA", p.format());
        assertEquals(Verdict.VALID, p.verdict(), () -> p.subIndication() + " " + p.errors());

        byte[] signedXml = new XadesBesSigner(pki.token(), settings(SignatureProfile.LT))
                .sign(XML.getBytes(StandardCharsets.UTF_8), "y.xml", TestPki.ALIAS);
        SignatureResult x = only(new SignatureVerifier(trusted()).verify(signedXml, "y.xml", null, null));
        assertEquals("XAdES-BASELINE-LT", x.format());
        assertEquals(Verdict.VALID, x.verdict(), () -> x.subIndication() + " " + x.errors());
    }

    @Test
    void revokedSignerIsNotValid() throws Exception {
        TestPki local = new TestPki();
        byte[] imz = new CadesBesSigner(local.token(), SignatureSettings.bes())
                .sign(CONTENT, "b.txt", TestPki.ALIAS, CadesPackaging.ATTACHED);
        local.revokeSigner(Instant.now().minus(1, ChronoUnit.HOURS));
        var r = new SignatureVerifier(TrustSettings.of(List.of(local.root), null, local.crlSource(), null))
                .verify(imz, "b.imz", null, null);
        SignatureResult s = only(r);
        assertNotEquals(Verdict.VALID, s.verdict());
        assertTrue(s.subIndication().orElse("").startsWith("REVOKED"), s.subIndication().toString());
        assertTrue(local.crlRequests.get() > 0, "CRL sorgulanmalı");
    }

    @Test
    void unsignedFileIsRejected() {
        assertThrows(TokenException.class, () -> new SignatureVerifier(trusted())
                .verify("imzasız".getBytes(StandardCharsets.UTF_8), "a.txt", null, null));
    }
}
