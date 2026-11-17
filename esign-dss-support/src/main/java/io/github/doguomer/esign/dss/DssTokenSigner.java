package io.github.doguomer.esign.dss;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.signature.AbstractSignatureParameters;
import eu.europa.esig.dss.signature.DocumentSignatureService;
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
 * EU DSS'in iki aşamalı imza akışını (getDataToSign → kartta imza → signDocument)
 * {@link SigningToken} ile birleştirir. CAdES, PAdES ve XAdES modülleri bunu ortak kullanır.
 */
public final class DssTokenSigner {

    public static final DigestAlgorithm DIGEST = DigestAlgorithm.SHA256;

    private final SigningToken token;
    private final Clock clock;
    private final Timestamping timestamping;

    public DssTokenSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    public DssTokenSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.token = Objects.requireNonNull(token, "token");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timestamping = Objects.requireNonNull(timestamping, "timestamping");
    }

    public Timestamping timestamping() {
        return timestamping;
    }

    /** İmzacı sertifikası, zincir, özet algoritması ve imza zamanını parametrelere yazar. */
    public <P extends AbstractSignatureParameters<?>> P prepare(P params, String alias) throws TokenException {
        List<X509Certificate> chain = token.certificateChain(alias);
        params.setSigningCertificate(new CertificateToken(chain.get(0)));
        params.setCertificateChain(chain.stream().skip(1).map(CertificateToken::new).toList());
        params.setDigestAlgorithm(DIGEST);
        params.bLevel().setSigningDate(Date.from(clock.instant()));
        return params;
    }

    /** Hazırlanmış parametrelerle belgeyi imzalar; özel anahtar işlemi kartta yapılır. */
    public <P extends AbstractSignatureParameters<?>> byte[] sign(
            DocumentSignatureService<P, ?> service, DSSDocument doc, P params, String alias) throws TokenException {
        ToBeSigned tbs = service.getDataToSign(doc, params);
        SignatureAlgorithm algo = algorithmFor(params.getSigningCertificate());
        byte[] raw = token.sign(alias, tbs.getBytes(), algo.getJCEId());
        DSSDocument signed;
        try {
            signed = service.signDocument(doc, params, new SignatureValue(algo, raw));
        } catch (eu.europa.esig.dss.model.DSSException e) {
            if (timestamping.enabled()) {
                throw new TokenException("Zaman damgası alınamadı ("
                        + timestamping.description().orElse("TSA") + "): " + e.getMessage(), e);
            }
            throw e;
        }
        try (var out = new ByteArrayOutputStream()) {
            signed.writeTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new TokenException("İmzalı belge yazılamadı", e);
        }
    }

    static SignatureAlgorithm algorithmFor(CertificateToken cert) {
        EncryptionAlgorithm enc = "EC".equals(cert.getPublicKey().getAlgorithm())
                ? EncryptionAlgorithm.ECDSA : EncryptionAlgorithm.RSA;
        return SignatureAlgorithm.getAlgorithm(enc, DIGEST);
    }
}
