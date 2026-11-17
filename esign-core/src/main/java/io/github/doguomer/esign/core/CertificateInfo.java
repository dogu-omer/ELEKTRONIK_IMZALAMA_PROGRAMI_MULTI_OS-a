package io.github.doguomer.esign.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Bir akıllı kart / token üzerindeki sertifikanın değişmez özeti.
 * Immutable summary of a certificate found on a token.
 *
 * @param alias           token üzerindeki etiket (PKCS#11 CKA_LABEL)
 * @param subject         sertifika sahibi (RFC 2253)
 * @param issuer          yayıncı ESHS (RFC 2253)
 * @param serialHex       seri numarası, onaltılık
 * @param notBefore       geçerlilik başlangıcı
 * @param notAfter        geçerlilik bitişi
 * @param nonRepudiation  keyUsage nonRepudiation biti; nitelikli imza sertifikalarında beklenir
 * @param hasPrivateKey   tokende eşleşen özel anahtar var mı
 * @param sha256Hex       DER kodlu sertifikanın SHA-256 parmak izi
 */
public record CertificateInfo(
        String alias,
        String subject,
        String issuer,
        String serialHex,
        Instant notBefore,
        Instant notAfter,
        boolean nonRepudiation,
        boolean hasPrivateKey,
        String sha256Hex) {

    private static final int KEY_USAGE_NON_REPUDIATION = 1;

    public CertificateInfo {
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(serialHex, "serialHex");
        Objects.requireNonNull(notBefore, "notBefore");
        Objects.requireNonNull(notAfter, "notAfter");
        Objects.requireNonNull(sha256Hex, "sha256Hex");
    }

    /** X.509 sertifikasından özet üretir. */
    public static CertificateInfo of(String alias, X509Certificate cert, boolean hasPrivateKey) {
        Objects.requireNonNull(cert, "cert");
        boolean[] ku = cert.getKeyUsage();
        boolean nonRep = ku != null && ku.length > KEY_USAGE_NON_REPUDIATION && ku[KEY_USAGE_NON_REPUDIATION];
        return new CertificateInfo(
                alias,
                cert.getSubjectX500Principal().getName(),
                cert.getIssuerX500Principal().getName(),
                cert.getSerialNumber().toString(16).toUpperCase(),
                cert.getNotBefore().toInstant(),
                cert.getNotAfter().toInstant(),
                nonRep,
                hasPrivateKey,
                sha256(cert));
    }

    /** Verilen anda sertifika süre olarak geçerli mi (iptal kontrolü değildir). */
    public boolean isTimeValidAt(Instant at) {
        return !at.isBefore(notBefore) && !at.isAfter(notAfter);
    }

    /** Nitelikli imza adayı: özel anahtarı var ve nonRepudiation biti açık. */
    public boolean isSigningCandidate() {
        return hasPrivateKey && nonRepudiation;
    }

    private static String sha256(X509Certificate cert) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IllegalStateException("Sertifika parmak izi hesaplanamadı", e);
        }
    }
}
