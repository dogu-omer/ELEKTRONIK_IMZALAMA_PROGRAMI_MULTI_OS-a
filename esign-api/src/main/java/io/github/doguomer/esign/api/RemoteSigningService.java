package io.github.doguomer.esign.api;

import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.signature.AbstractSignatureParameters;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Sunucu kartı göremediği için imza iki adımda üretilir:
 * <ol>
 *   <li>{@link #prepare}: belge + istemcinin sertifika zinciri → imzalanacak veri (DSS ToBeSigned)</li>
 *   <li>istemci bu veriyi kartında imzalar (ör. SHA256withRSA)</li>
 *   <li>{@link #complete}: imza değeri doğrulanır ve imzalı belge üretilir (T/LT/LTA için zaman damgası burada)</li>
 * </ol>
 * Two-step signing: the server never holds a private key.
 */
public final class RemoteSigningService {

    /** Sunucu yapılandırması: profil adından imza ayarlarını üretir (TSA, güven kökleri). */
    @FunctionalInterface
    public interface SettingsFactory {
        SignatureSettings forProfile(SignatureProfile profile);
    }

    public record PrepareRequest(byte[] document, String fileName, List<X509Certificate> chain,
            DocumentFormat format, boolean detached, SignatureProfile profile, String reason) {
        public PrepareRequest {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(chain, "chain");
            fileName = fileName == null || fileName.isBlank() ? "belge" : fileName;
            format = format == null ? DocumentFormat.AUTO : format;
            profile = profile == null ? SignatureProfile.B : profile;
        }
    }

    /**
     * @param dataToSign      istemcinin {@code signatureAlgorithm} ile imzalayacağı baytlar
     * @param digest          dataToSign'ın SHA-256 özeti (yalnızca özet imzalayan istemciler için)
     * @param signatureAlgorithm JCA adı, ör. SHA256withRSA
     */
    public record Prepared(String id, byte[] dataToSign, byte[] digest, String signatureAlgorithm,
            String format, String level, Instant expiresAt) {}

    public record Signed(byte[] content, String fileName, String mediaType, String level) {}

    private record Session(DocumentSignatureService<AbstractSignatureParameters<?>, ?> service,
            DSSDocument document, AbstractSignatureParameters<?> params, ToBeSigned toBeSigned,
            SignatureAlgorithm algorithm, X509Certificate signer, String outputName, String mediaType, String level) {}

    private final SettingsFactory settings;
    private final SessionStore<Session> sessions;
    private final Clock clock;

    public RemoteSigningService(SettingsFactory settings, Clock clock, Duration ttl, int maxSessions) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessions = new SessionStore<>(clock, ttl, maxSessions);
    }

    public Prepared prepare(PrepareRequest r) throws TokenException {
        SignatureSettings s = settings.forProfile(r.profile());
        DocumentFormat f = r.format().resolve(r.fileName());
        var signer = new DssTokenSigner(new ExternalCertificateToken(r.chain()), clock, s);
        DSSDocument doc = new InMemoryDocument(r.document(), r.fileName());
        AbstractSignatureParameters<?> params;
        Function<eu.europa.esig.dss.spi.validation.CertificateVerifier, ?> factory;
        String outName;
        String mediaType;
        switch (f) {
            case PADES -> {
                if (!isPdf(r.document())) {
                    throw new IllegalArgumentException("Dosya bir PDF değil: " + r.fileName());
                }
                PAdESSignatureParameters p = signer.prepare(new PAdESSignatureParameters(), ExternalCertificateToken.ALIAS);
                if (r.reason() != null && !r.reason().isBlank()) {
                    p.setReason(r.reason().strip());
                }
                params = p;
                factory = PAdESService::new;
                outName = stem(r.fileName()) + "_imzali.pdf";
                mediaType = "application/pdf";
            }
            case XADES -> {
                XAdESSignatureParameters p = signer.prepare(new XAdESSignatureParameters(), ExternalCertificateToken.ALIAS);
                p.setSignaturePackaging(SignaturePackaging.ENVELOPED);
                params = p;
                factory = XAdESService::new;
                outName = stem(r.fileName()) + "_imzali.xml";
                mediaType = "application/xml";
            }
            default -> {
                CAdESSignatureParameters p = signer.prepare(new CAdESSignatureParameters(), ExternalCertificateToken.ALIAS);
                p.setSignaturePackaging(r.detached() ? SignaturePackaging.DETACHED : SignaturePackaging.ENVELOPING);
                params = p;
                factory = CAdESService::new;
                outName = r.fileName() + (r.detached() ? ".p7s" : ".imz");
                mediaType = r.detached() ? "application/pkcs7-signature" : "application/pkcs7-mime";
            }
        }
        params.setSignatureLevel(signer.level(f.dssName()));
        @SuppressWarnings({"unchecked", "rawtypes"})
        DocumentSignatureService<AbstractSignatureParameters<?>, ?> service =
                (DocumentSignatureService) signer.service((Function) factory);
        ToBeSigned tbs;
        try {
            tbs = service.getDataToSign(doc, params);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(f.dssName() + " imzası hazırlanamadı (belge biçimi uygun mu?)", e);
        }
        SignatureAlgorithm algo = algorithmFor(r.chain().get(0));
        String level = f.dssName() + "-" + s.profile().label();
        String id = sessions.put(new Session(service, doc, params, tbs, algo, r.chain().get(0), outName, mediaType,
                level));
        return new Prepared(id, tbs.getBytes(), sha256(tbs.getBytes()), algo.getJCEId(), f.dssName(), level,
                sessions.expiresAt(id).orElseThrow());
    }

    /** İmza değeri istemcinin sertifikasıyla doğrulanmadan belge üretilmez. */
    public Signed complete(String id, byte[] signatureValue) throws TokenException {
        Objects.requireNonNull(signatureValue, "signatureValue");
        Session s = sessions.take(id).orElseThrow(() -> new SessionNotFoundException(id));
        if (!verifies(s, signatureValue)) {
            throw new IllegalArgumentException("İmza değeri sertifikanın açık anahtarıyla doğrulanamadı "
                    + "(yanlış veri veya yanlış kart imzalanmış olabilir)");
        }
        DSSDocument signed;
        try {
            signed = s.service().signDocument(s.document(), s.params(), new SignatureValue(s.algorithm(), signatureValue));
        } catch (RuntimeException e) {
            throw new TokenException("İmzalı belge üretilemedi (zaman damgası/OCSP/CRL alınamamış olabilir)", e);
        }
        try (var out = new ByteArrayOutputStream()) {
            signed.writeTo(out);
            return new Signed(out.toByteArray(), s.outputName(), s.mediaType(), s.level());
        } catch (IOException e) {
            throw new TokenException("İmzalı belge yazılamadı", e);
        }
    }

    /** Hazırlanmış oturumu iptal eder. */
    public boolean cancel(String id) {
        return sessions.take(id).isPresent();
    }

    public int activeSessions() {
        return sessions.size();
    }

    private static boolean verifies(Session s, byte[] value) {
        try {
            Signature v = Signature.getInstance(s.algorithm().getJCEId());
            v.initVerify(s.signer().getPublicKey());
            v.update(s.toBeSigned().getBytes());
            return v.verify(value);
        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        }
    }

    static SignatureAlgorithm algorithmFor(X509Certificate cert) {
        String alg = cert.getPublicKey().getAlgorithm();
        EncryptionAlgorithm enc = switch (alg) {
            case "RSA" -> EncryptionAlgorithm.RSA;
            case "EC" -> EncryptionAlgorithm.ECDSA;
            default -> throw new IllegalArgumentException("Desteklenmeyen anahtar türü: " + alg);
        };
        return SignatureAlgorithm.getAlgorithm(enc, DssTokenSigner.DIGEST);
    }

    private static boolean isPdf(byte[] b) {
        return b.length >= 5 && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    private static String stem(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static byte[] sha256(byte[] b) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(b);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Oturum bulunamadı veya süresi doldu. */
    public static final class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String id) {
            super("İmza oturumu bulunamadı veya süresi doldu: " + id);
        }
    }

}
