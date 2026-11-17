package io.github.doguomer.esign.pkcs11;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Gerçek bir PKCS#11 kütüphanesiyle (SoftHSM2 veya AKİS) entegrasyon testi.
 * Çalıştırmak için: scripts/softhsm-setup.sh, ardından ESIGN_TEST_PKCS11_LIB ve ESIGN_TEST_PIN.
 */
@EnabledIfEnvironmentVariable(named = "ESIGN_TEST_PKCS11_LIB", matches = ".+")
class Pkcs11SigningTokenIntegrationTest {

    @Test
    void listsCertificateWithPrivateKey() throws Exception {
        var cfg = new Pkcs11Config(Path.of(System.getenv("ESIGN_TEST_PKCS11_LIB")), 0);
        char[] pin = System.getenv("ESIGN_TEST_PIN").toCharArray();
        try (var token = Pkcs11SigningToken.open(cfg, pin)) {
            var certs = token.listCertificates();
            assertFalse(certs.isEmpty(), "Token üzerinde sertifika bulunmalı");
            assertTrue(certs.stream().anyMatch(c -> c.isSigningCandidate()),
                    "nonRepudiation + özel anahtarlı sertifika bulunmalı");
        }
        assertTrue(new String(pin).chars().allMatch(ch -> ch == 0), "PIN bellekten silinmeli");
    }

    @Test
    void openingWithoutPinGivesClearHint() {
        var cfg = new Pkcs11Config(Path.of(System.getenv("ESIGN_TEST_PKCS11_LIB")), 0);
        var e = assertThrows(io.github.doguomer.esign.core.TokenException.class,
                () -> Pkcs11SigningToken.open(cfg, null).listCertificates());
        assertTrue(e.getMessage().contains("--login"));
    }
}
