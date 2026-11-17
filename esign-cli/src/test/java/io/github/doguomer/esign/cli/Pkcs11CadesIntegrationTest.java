package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.cades.CadesBesSigner;
import io.github.doguomer.esign.cades.CadesPackaging;
import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.pkcs11.Pkcs11Config;
import io.github.doguomer.esign.pkcs11.Pkcs11SigningToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Kart (SoftHSM/AKİS) üzerinde gerçek CAdES-BES imzası; bkz. scripts/softhsm-setup.sh. */
@EnabledIfEnvironmentVariable(named = "ESIGN_TEST_PKCS11_LIB", matches = ".+")
class Pkcs11CadesIntegrationTest {

    @Test
    void signsOnTokenAndVerifies() throws Exception {
        var cfg = new Pkcs11Config(Path.of(System.getenv("ESIGN_TEST_PKCS11_LIB")), 0);
        byte[] content = "Faz 2 entegrasyon testi".getBytes(StandardCharsets.UTF_8);
        try (var token = Pkcs11SigningToken.open(cfg, System.getenv("ESIGN_TEST_PIN").toCharArray())) {
            CertificateInfo info = token.listCertificates().stream()
                    .filter(CertificateInfo::isSigningCandidate).findFirst().orElseThrow();
            byte[] imz = new CadesBesSigner(token).sign(content, "t.txt", info.alias(), CadesPackaging.ATTACHED);

            var cms = new CMSSignedData(imz);
            var si = cms.getSignerInfos().getSigners().iterator().next();
            var cert = token.certificateChain(info.alias()).get(0);
            assertTrue(si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(cert)));
            assertArrayEquals(content, (byte[]) cms.getSignedContent().getContent());
        }
    }
}
