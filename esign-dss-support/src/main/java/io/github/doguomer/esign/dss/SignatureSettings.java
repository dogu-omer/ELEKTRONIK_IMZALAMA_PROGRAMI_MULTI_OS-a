package io.github.doguomer.esign.dss;

import java.util.Objects;

/**
 * İmza üretim ayarları: profil (BES/T/LT/LTA), zaman damgası ve güven/iptal kaynakları.
 * Geçersiz birleşimler oluşturulurken reddedilir. Invalid combinations are rejected up front.
 */
public record SignatureSettings(SignatureProfile profile, Timestamping timestamping, TrustSettings trust) {

    public SignatureSettings {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(timestamping, "timestamping");
        Objects.requireNonNull(trust, "trust");
        if (profile.needsTimestamp() && !timestamping.enabled()) {
            throw new IllegalArgumentException(profile.label() + " profili zaman damgası sunucusu (--tsa) gerektirir");
        }
        if (!profile.needsTimestamp() && timestamping.enabled()) {
            throw new IllegalArgumentException("BES profilinde zaman damgası kullanılmaz; T, LT veya LTA seçin");
        }
        if (profile.needsRevocationData() && !trust.hasRevocationSource()) {
            throw new IllegalArgumentException(profile.label()
                    + " profili OCSP/CRL erişimi gerektirir (--offline ile kullanılamaz)");
        }
        if (profile.needsRevocationData() && trust.anchors().isEmpty()) {
            throw new IllegalArgumentException(profile.label()
                    + " profili güvenilir kök sertifika gerektirir (--trust <dosya|klasör>)");
        }
    }

    /** Zaman damgası varsa T, yoksa BES; v0.2 davranışı. */
    public static SignatureSettings of(Timestamping timestamping) {
        return new SignatureSettings(timestamping.enabled() ? SignatureProfile.T : SignatureProfile.B,
                timestamping, TrustSettings.none());
    }

    public static SignatureSettings bes() {
        return of(Timestamping.none());
    }
}
