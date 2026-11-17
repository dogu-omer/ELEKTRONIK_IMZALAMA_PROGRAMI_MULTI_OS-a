package io.github.doguomer.esign.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.dss.testing.TestPki;
import io.github.doguomer.esign.validation.SignatureVerifier;
import io.github.doguomer.esign.validation.VerificationReport;
import io.github.doguomer.esign.validation.Verdict;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hazırla → kartta imzala → birleştir akışı ve doğrulama. */
class RemoteSigningServiceTest {

    private static TestPki pki;
    private static final byte[] TEXT = "Merhaba dünya".getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void setUp() throws Exception {
        pki = new TestPki();
    }

    private static RemoteSigningService service(Clock clock) {
        TrustSettings trust = TrustSettings.of(List.of(pki.root), null, pki.crlSource(), null);
        return new RemoteSigningService(p -> new SignatureSettings(p,
                p.needsTimestamp() ? Timestamping.of(pki.tsa, "test") : Timestamping.none(),
                p.needsRevocationData() ? trust : TrustSettings.none()), clock, Duration.ofMinutes(5), 3);
    }

    private static RemoteSigningService service() {
        return service(Clock.systemUTC());
    }

    private static RemoteSigningService.PrepareRequest req(byte[] doc, String name, DocumentFormat f,
            boolean detached, SignatureProfile p) {
        return new RemoteSigningService.PrepareRequest(doc, name, List.of(pki.signer, pki.root), f, detached, p,
                "Onay");
    }

    private static RemoteSigningService.Signed roundTrip(RemoteSigningService s,
            RemoteSigningService.PrepareRequest r) throws Exception {
        var p = s.prepare(r);
        assertEquals("SHA256withRSA", p.signatureAlgorithm());
        assertEquals(32, p.digest().length);
        return s.complete(p.id(), TestData.signOnCard(pki, p.dataToSign(), p.signatureAlgorithm()));
    }

    private static VerificationReport verify(byte[] signed, String name, byte[] original) throws Exception {
        TrustSettings trust = TrustSettings.of(List.of(pki.root), null, pki.crlSource(), null);
        return new SignatureVerifier(trust).verify(signed, name, original, original == null ? null : "belge.txt");
    }

    @Test
    void cadesAttachedBesIsValid() throws Exception {
        var signed = roundTrip(service(), req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B));
        assertEquals("belge.txt.imz", signed.fileName());
        assertEquals("CAdES-BES", signed.level());
        assertEquals(Verdict.VALID, verify(signed.content(), signed.fileName(), null).overall());
    }

    @Test
    void cadesDetachedNeedsOriginal() throws Exception {
        var signed = roundTrip(service(), req(TEXT, "belge.txt", DocumentFormat.CADES, true, SignatureProfile.B));
        assertEquals("belge.txt.p7s", signed.fileName());
        assertEquals(Verdict.VALID, verify(signed.content(), signed.fileName(), TEXT).overall());
    }

    @Test
    void padesTimestampedIsValid() throws Exception {
        var signed = roundTrip(service(), req(TestData.pdf(), "rapor.pdf", DocumentFormat.AUTO, false,
                SignatureProfile.T));
        assertEquals("rapor_imzali.pdf", signed.fileName());
        assertEquals("PAdES-T", signed.level());
        var r = verify(signed.content(), signed.fileName(), null);
        assertEquals(Verdict.VALID, r.overall());
        assertTrue(r.signatures().get(0).timestampTime().isPresent());
    }

    @Test
    void xadesLtIsValid() throws Exception {
        var signed = roundTrip(service(), req(TestData.xml(), "yazi.xml", DocumentFormat.AUTO, false,
                SignatureProfile.LT));
        assertEquals("yazi_imzali.xml", signed.fileName());
        var r = verify(signed.content(), signed.fileName(), null);
        assertEquals(Verdict.VALID, r.overall());
        assertTrue(r.signatures().get(0).format().contains("XAdES"));
    }

    @Test
    void wrongSignatureValueIsRejectedAndSessionConsumed() throws Exception {
        var s = service();
        var p = s.prepare(req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B));
        byte[] bogus = TestData.signOnCard(pki, "başka veri".getBytes(StandardCharsets.UTF_8), p.signatureAlgorithm());
        assertThrows(IllegalArgumentException.class, () -> s.complete(p.id(), bogus));
        byte[] good = TestData.signOnCard(pki, p.dataToSign(), p.signatureAlgorithm());
        assertThrows(RemoteSigningService.SessionNotFoundException.class, () -> s.complete(p.id(), good));
    }

    @Test
    void sessionIsSingleUse() throws Exception {
        var s = service();
        var p = s.prepare(req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B));
        byte[] value = TestData.signOnCard(pki, p.dataToSign(), p.signatureAlgorithm());
        s.complete(p.id(), value);
        assertThrows(RemoteSigningService.SessionNotFoundException.class, () -> s.complete(p.id(), value));
    }

    @Test
    void sessionExpires() throws Exception {
        var now = new AtomicReference<>(Instant.parse("2026-11-17T10:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId z) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        var s = service(clock);
        var p = s.prepare(req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B));
        now.set(now.get().plus(Duration.ofMinutes(6)));
        byte[] value = TestData.signOnCard(pki, p.dataToSign(), p.signatureAlgorithm());
        assertThrows(RemoteSigningService.SessionNotFoundException.class, () -> s.complete(p.id(), value));
        assertEquals(0, s.activeSessions());
    }

    @Test
    void sessionLimitIsEnforced() throws Exception {
        var s = service();
        for (int i = 0; i < 3; i++) {
            s.prepare(req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B));
        }
        assertThrows(SessionStore.TooManySessionsException.class,
                () -> s.prepare(req(TEXT, "belge.txt", DocumentFormat.AUTO, false, SignatureProfile.B)));
    }

    @Test
    void padesRejectsNonPdf() {
        assertThrows(IllegalArgumentException.class,
                () -> service().prepare(req(TEXT, "x.pdf", DocumentFormat.AUTO, false, SignatureProfile.B)));
    }
}
