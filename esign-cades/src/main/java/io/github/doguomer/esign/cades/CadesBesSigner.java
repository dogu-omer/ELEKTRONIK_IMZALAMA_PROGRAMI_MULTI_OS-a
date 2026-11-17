package io.github.doguomer.esign.cades;

import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * CAdES-BES (ETSI baseline B) imza üretir. Özel anahtar işlemi {@link SigningToken}
 * üzerinden kartta yapılır; bu sınıf yalnızca CMS yapısını kurar.
 */
public final class CadesBesSigner {

    private final SigningToken token;
    private final Clock clock;

    public CadesBesSigner(SigningToken token) {
        this(token, Clock.systemUTC());
    }

    CadesBesSigner(SigningToken token, Clock clock) {
        this.token = Objects.requireNonNull(token, "token");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * @param content   imzalanacak dosya içeriği
     * @param fileName  belge adı (imza içinde içerik türü tahmini için)
     * @param alias     tokendeki imza sertifikası
     * @return DER kodlu CMS SignedData (CAdES-BES)
     */
    public byte[] sign(byte[] content, String fileName, String alias, CadesPackaging packaging)
            throws TokenException {
        Objects.requireNonNull(content, "content");
        List<X509Certificate> chain = token.certificateChain(alias);
        CertificateToken signer = new CertificateToken(chain.get(0));

        CAdESSignatureParameters params = new CAdESSignatureParameters();
        params.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        params.setSignaturePackaging(packaging == CadesPackaging.ATTACHED
                ? SignaturePackaging.ENVELOPING : SignaturePackaging.DETACHED);
        params.setDigestAlgorithm(DigestAlgorithm.SHA256);
        params.setSigningCertificate(signer);
        params.setCertificateChain(chain.stream().skip(1).map(CertificateToken::new).toList());
        params.bLevel().setSigningDate(Date.from(clock.instant()));

        CAdESService service = new CAdESService(new CommonCertificateVerifier());
        DSSDocument doc = new InMemoryDocument(content, fileName);
        ToBeSigned tbs = service.getDataToSign(doc, params);

        SignatureAlgorithm algo = SignatureAlgorithm.getAlgorithm(
                signer.getPublicKey().getAlgorithm().equals("EC")
                        ? eu.europa.esig.dss.enumerations.EncryptionAlgorithm.ECDSA
                        : eu.europa.esig.dss.enumerations.EncryptionAlgorithm.RSA,
                DigestAlgorithm.SHA256);
        byte[] raw = token.sign(alias, tbs.getBytes(), algo.getJCEId());

        DSSDocument signed = service.signDocument(doc, params, new SignatureValue(algo, raw));
        try (var out = new ByteArrayOutputStream()) {
            signed.writeTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new TokenException("İmzalı belge yazılamadı", e);
        }
    }
}
