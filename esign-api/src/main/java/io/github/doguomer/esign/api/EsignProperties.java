package io.github.doguomer.esign.api;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ortam değişkenleriyle ayarlanır (sırlar depoya yazılmaz):
 * ESIGN_TSA_URL, ESIGN_TRUST_DIR, ESIGN_OFFLINE, ESIGN_API_KEY, ESIGN_API_KEY_SHA256, ESIGN_ALLOW_ANONYMOUS,
 * ESIGN_RATE_LIMIT, ESIGN_SESSION_TTL, ESIGN_MAX_SESSIONS.
 *
 * @param tsaUrl      RFC 3161 zaman damgası sunucusu; boşsa yalnızca BES üretilir
 * @param trustDir    güvenilir kök sertifikalar (PEM/DER dosya veya klasör)
 * @param offline     true ise OCSP/CRL/AIA'ya bağlanılmaz
 * @param apiKey      /api/** için X-API-Key (düz metin; sunucuda yalnızca özeti tutulur)
 * @param apiKeySha256 virgülle ayrılmış SHA-256 hex özetleri (anahtarın kendisi sunucuya hiç yazılmaz)
 * @param allowAnonymous true ise anahtarsız çalışmaya izin verilir (yalnızca yerel geliştirme)
 * @param rateLimit   IP başına dakikalık /api/** istek sınırı
 * @param sessionTtl  hazırlanmış imzanın geçerlilik süresi
 * @param maxSessions eşzamanlı bekleyen imza sayısı üst sınırı
 */
@ConfigurationProperties(prefix = "esign")
public record EsignProperties(String tsaUrl, String trustDir, boolean offline, String apiKey,
        String apiKeySha256, boolean allowAnonymous, Integer rateLimit, Duration sessionTtl, Integer maxSessions) {

    public EsignProperties {
        sessionTtl = sessionTtl == null ? Duration.ofMinutes(10) : sessionTtl;
        maxSessions = maxSessions == null ? 200 : maxSessions;
        rateLimit = rateLimit == null ? 120 : rateLimit;
    }

    boolean hasApiKey() {
        return ApiKeyFilter.configured(apiKey, apiKeySha256);
    }

    @Override
    public String toString() { // anahtar asla loglanmaz / never log secrets
        return "EsignProperties[tsaUrl=" + tsaUrl + ", trustDir=" + trustDir + ", offline=" + offline
                + ", apiKey=" + (hasApiKey() ? "***" : "<yok>") + ", allowAnonymous=" + allowAnonymous
                + ", rateLimit=" + rateLimit + ", sessionTtl=" + sessionTtl + ", maxSessions=" + maxSessions + "]";
    }
}
