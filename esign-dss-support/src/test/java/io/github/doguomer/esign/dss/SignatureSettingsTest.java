package io.github.doguomer.esign.dss;

import static org.junit.jupiter.api.Assertions.*;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import io.github.doguomer.esign.dss.testing.TestPki;
import io.github.doguomer.esign.dss.testing.TestTsa;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignatureSettingsTest {

    @Test
    void profileMapsToEtsiLevels() {
        assertEquals(SignatureLevel.CAdES_BASELINE_B, SignatureProfile.B.level("CAdES"));
        assertEquals(SignatureLevel.PAdES_BASELINE_LTA, SignatureProfile.LTA.level("PAdES"));
        assertEquals(SignatureLevel.XAdES_BASELINE_LT, SignatureProfile.LT.level("XAdES"));
        assertEquals(SignatureProfile.B, SignatureProfile.parse("bes"));
        assertEquals(SignatureProfile.LTA, SignatureProfile.parse(" lta "));
        assertThrows(IllegalArgumentException.class, () -> SignatureProfile.parse("XL"));
    }

    @Test
    void rejectsInconsistentCombinations() throws Exception {
        var tsa = Timestamping.of(new TestTsa(), "t");
        assertThrows(IllegalArgumentException.class,
                () -> new SignatureSettings(SignatureProfile.T, Timestamping.none(), TrustSettings.none()));
        assertThrows(IllegalArgumentException.class,
                () -> new SignatureSettings(SignatureProfile.B, tsa, TrustSettings.none()));
        var noRevocation = TrustSettings.of(List.of(new TestPki().root), false);
        var e = assertThrows(IllegalArgumentException.class,
                () -> new SignatureSettings(SignatureProfile.LT, tsa, noRevocation));
        assertTrue(e.getMessage().contains("OCSP/CRL"));
        assertThrows(IllegalArgumentException.class,
                () -> new SignatureSettings(SignatureProfile.LTA, tsa, TrustSettings.of(List.of(), true)));
        assertEquals(SignatureProfile.T, SignatureSettings.of(tsa).profile());
    }

    @Test
    void loadsPemAndDerFromDirectory() throws Exception {
        TestPki pki = new TestPki();
        var dir = Files.createTempDirectory("kokler");
        Files.write(dir.resolve("kok.der"), pki.root.getEncoded());
        String pem = "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pki.signer.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
        Files.writeString(dir.resolve("ara.pem"), pem);
        Files.writeString(dir.resolve("okubeni.txt"), "yok sayılır");
        var certs = TrustSettings.load(dir);
        assertEquals(2, certs.size());
        assertThrows(java.io.IOException.class, () -> TrustSettings.load(dir.resolve("yok")));
    }
}
