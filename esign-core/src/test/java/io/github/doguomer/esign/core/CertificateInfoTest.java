package io.github.doguomer.esign.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.junit.jupiter.api.Test;

class CertificateInfoTest {

    private final Instant now = Instant.parse("2026-11-17T10:00:00Z");

    @Test
    void qualifiedSigningCertificateIsCandidate() throws Exception {
        var cert = TestCertificates.selfSigned("Test Imzaci", KeyUsage.nonRepudiation,
                now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(365)));
        var info = CertificateInfo.of("imza", cert, true);

        assertTrue(info.nonRepudiation());
        assertTrue(info.isSigningCandidate());
        assertEquals("ABCDEF", info.serialHex());
        assertTrue(info.subject().contains("CN=Test Imzaci"));
        assertEquals(64, info.sha256Hex().length());
    }

    @Test
    void authenticationCertificateIsNotCandidate() throws Exception {
        var cert = TestCertificates.selfSigned("Kimlik Dogrulama", KeyUsage.digitalSignature,
                now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        var info = CertificateInfo.of("auth", cert, true);

        assertFalse(info.nonRepudiation());
        assertFalse(info.isSigningCandidate());
    }

    @Test
    void certificateWithoutPrivateKeyIsNotCandidate() throws Exception {
        var cert = TestCertificates.selfSigned("CA", KeyUsage.nonRepudiation,
                now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        assertFalse(CertificateInfo.of("ca", cert, false).isSigningCandidate());
    }

    @Test
    void timeValidity() throws Exception {
        var from = now.minus(Duration.ofDays(1));
        var to = now.plus(Duration.ofDays(1));
        var info = CertificateInfo.of("x", TestCertificates.selfSigned("X", 0, from, to), false);

        assertTrue(info.isTimeValidAt(now));
        assertFalse(info.isTimeValidAt(to.plusSeconds(1)));
        assertFalse(info.isTimeValidAt(from.minusSeconds(1)));
    }
}
