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
    private final SignatureSettings settings;

    public DssTokenSigner(SigningToken token, Clock clock) {
        this(token, clock, SignatureSettings.bes());
    }

    public DssTokenSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this(token, clock, SignatureSettings.of(timestamping));
    }

    public DssTokenSigner(SigningToken token, Clock clock, SignatureSettings settings) {
        this.token = Objects.requireNonNull(token, "token");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public SignatureSettings settings() {
        return settings;
    }

    public Timestamping timestamping() {
        return settings.timestamping();
    }

    /** @param format "CAdES", "PAdES" veya "XAdES" */
    public eu.europa.esig.dss.enumerations.SignatureLevel level(String format) {
        return settings.profile().level(format);
    }

    /** Doğrulayıcı (güven/iptal kaynakları) ve zaman damgası bağlanmış imza servisi üretir. */
    public <S extends eu.europa.esig.dss.signature.AbstractSignatureService<?, ?>> S service(
            java.util.function.Function<eu.europa.esig.dss.spi.validation.CertificateVerifier, S> factory) {
        return settings.timestamping().apply(factory.apply(settings.trust().certificateVerifier()));
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
        } catch (RuntimeException e) {
            SignatureProfile p = settings.profile();
            if (p.needsRevocationData()) {
                throw new TokenException(p.label() + " imza üretilemedi (zaman damgası, sertifika zinciri veya "
                        + "OCSP/CRL verisi alınamadı): " + e.getMessage(), e);
            }
            if (p.needsTimestamp()) {
                throw new TokenException("Zaman damgası alınamadı ("
                        + timestamping().description().orElse("TSA") + "): " + e.getMessage(), e);
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
