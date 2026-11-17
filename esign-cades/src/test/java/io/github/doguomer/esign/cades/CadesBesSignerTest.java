package io.github.doguomer.esign.cades;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.core.TokenException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CadesBesSignerTest {

    private static final byte[] CONTENT = "Örnek belge içeriği / sample document".getBytes(StandardCharsets.UTF_8);
    private InMemorySigningToken token;
    private CadesBesSigner signer;
    private final Instant signingTime = Instant.parse("2026-11-17T09:00:00Z");

    @BeforeEach
    void setUp() throws Exception {
        token = new InMemorySigningToken();
        signer = new CadesBesSigner(token, Clock.fixed(signingTime, ZoneOffset.UTC));
    }

    @Test
    void attachedSignatureEmbedsContentAndVerifies() throws Exception {
        byte[] imz = signer.sign(CONTENT, "belge.txt", InMemorySigningToken.ALIAS, CadesPackaging.ATTACHED);

        CMSSignedData cms = new CMSSignedData(imz);
        assertArrayEquals(CONTENT, (byte[]) cms.getSignedContent().getContent());
        SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
        assertTrue(si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(token.cert)));
        assertEquals(1, token.signCalls, "Kart tam bir kez imza atmalı");
    }

    @Test
    void besAttributesArePresent() throws Exception {
        byte[] imz = signer.sign(CONTENT, "belge.txt", InMemorySigningToken.ALIAS, CadesPackaging.ATTACHED);
        SignerInformation si = new CMSSignedData(imz).getSignerInfos().getSigners().iterator().next();
        var attrs = si.getSignedAttributes();

        assertNotNull(attrs.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2), "signing-certificate-v2 (ETSI) gerekli");
        assertNotNull(attrs.get(CMSAttributes.signingTime));
        assertNotNull(attrs.get(CMSAttributes.messageDigest));
    }

    @Test
    void detachedSignatureVerifiesAgainstOriginal() throws Exception {
        byte[] p7s = signer.sign(CONTENT, "belge.txt", InMemorySigningToken.ALIAS, CadesPackaging.DETACHED);

        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(CONTENT), p7s);
        SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
        assertTrue(si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(token.cert)));
    }

    @Test
    void detachedSignatureFailsForTamperedDocument() throws Exception {
        byte[] p7s = signer.sign(CONTENT, "belge.txt", InMemorySigningToken.ALIAS, CadesPackaging.DETACHED);
        byte[] tampered = "değiştirilmiş".getBytes(StandardCharsets.UTF_8);

        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(tampered), p7s);
        SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
        assertThrows(Exception.class, () -> {
            if (!si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(token.cert))) {
                throw new IllegalStateException("doğrulama başarısız");
            }
        });
    }

    @Test
    void unknownAliasFails() {
        assertThrows(TokenException.class,
                () -> signer.sign(CONTENT, "b.txt", "yok", CadesPackaging.ATTACHED));
    }
}
