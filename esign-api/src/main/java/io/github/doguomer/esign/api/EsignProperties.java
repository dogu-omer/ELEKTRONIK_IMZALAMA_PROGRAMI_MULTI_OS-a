package io.github.doguomer.esign.api;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ortam değişkenleriyle ayarlanır (sırlar depoya yazılmaz):
 * ESIGN_TSA_URL, ESIGN_TRUST_DIR, ESIGN_OFFLINE, ESIGN_API_KEY, ESIGN_SESSION_TTL, ESIGN_MAX_SESSIONS.
 *
 * @param tsaUrl      RFC 3161 zaman damgası sunucusu; boşsa yalnızca BES üretilir
 * @param trustDir    güvenilir kök sertifikalar (PEM/DER dosya veya klasör)
 * @param offline     true ise OCSP/CRL/AIA'ya bağlanılmaz
 * @param apiKey      doluysa /api/** istekleri X-API-Key başlığı ister
 * @param sessionTtl  hazırlanmış imzanın geçerlilik süresi
 * @param maxSessions eşzamanlı bekleyen imza sayısı üst sınırı
 */
@ConfigurationProperties(prefix = "esign")
public record EsignProperties(String tsaUrl, String trustDir, boolean offline, String apiKey,
        Duration sessionTtl, Integer maxSessions) {

    public EsignProperties {
        sessionTtl = sessionTtl == null ? Duration.ofMinutes(10) : sessionTtl;
        maxSessions = maxSessions == null ? 200 : maxSessions;
    }

    boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
