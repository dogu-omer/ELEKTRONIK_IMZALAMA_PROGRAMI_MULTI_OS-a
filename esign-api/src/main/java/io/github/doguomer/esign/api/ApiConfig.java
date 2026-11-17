package io.github.doguomer.esign.api;

import io.github.doguomer.esign.dss.SignatureProfile;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.validation.SignatureVerifier;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApiConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TrustSettings trustSettings(EsignProperties p) {
        try {
            var anchors = p.trustDir() == null || p.trustDir().isBlank()
                    ? List.<java.security.cert.X509Certificate>of() : TrustSettings.load(Path.of(p.trustDir().strip()));
            return TrustSettings.of(anchors, !p.offline());
        } catch (IOException e) {
            throw new UncheckedIOException("ESIGN_TRUST_DIR okunamadı: " + e.getMessage(), e);
        }
    }

    @Bean
    Timestamping timestamping(EsignProperties p) {
        return p.tsaUrl() == null || p.tsaUrl().isBlank()
                ? Timestamping.none() : Timestamping.online(URI.create(p.tsaUrl().strip()));
    }

    @Bean
    RemoteSigningService.SettingsFactory settingsFactory(Timestamping ts, TrustSettings trust) {
        return profile -> {
            if (profile.needsTimestamp() && !ts.enabled()) {
                throw new IllegalArgumentException(profile.label()
                        + " profili için sunucuda ESIGN_TSA_URL tanımlı değil; BES kullanın");
            }
            if (profile.needsRevocationData() && trust.anchors().isEmpty()) {
                throw new IllegalArgumentException(profile.label()
                        + " profili için sunucuda ESIGN_TRUST_DIR (güvenilir kökler) tanımlı değil");
            }
            if (profile.needsRevocationData() && !trust.hasRevocationSource()) {
                throw new IllegalArgumentException(profile.label() + " profili ESIGN_OFFLINE=true ile kullanılamaz");
            }
            return new SignatureSettings(profile, profile.needsTimestamp() ? ts : Timestamping.none(),
                    profile.needsRevocationData() ? trust : TrustSettings.none());
        };
    }

    @Bean
    RemoteSigningService remoteSigningService(RemoteSigningService.SettingsFactory f, Clock clock, EsignProperties p) {
        return new RemoteSigningService(f, clock, p.sessionTtl(), p.maxSessions());
    }

    @Bean
    SignatureVerifier signatureVerifier(TrustSettings trust) {
        return new SignatureVerifier(trust);
    }

    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(EsignProperties p) {
        var reg = new FilterRegistrationBean<>(new ApiKeyFilter(p.apiKey()));
        reg.addUrlPatterns("/api/*");
        reg.setEnabled(p.hasApiKey());
        return reg;
    }

    @Bean
    OpenAPI openApi() {
        String v = EsignApiApplication.class.getPackage().getImplementationVersion();
        return new OpenAPI().info(new Info()
                .title("esign REST API")
                .version(v == null ? "dev" : v)
                .description("CAdES/PAdES/XAdES imza doğrulama ve iki aşamalı imza (hazırla → kartta imzala → "
                        + "birleştir). Özel anahtar sunucuya gelmez. / Validation and two-step signing; the private "
                        + "key never leaves the client's smart card.")
                .license(new License().name("GPL-3.0-or-later").url("https://www.gnu.org/licenses/gpl-3.0.html")));
    }

    static SignatureProfile profile(String s) {
        return s == null || s.isBlank() ? SignatureProfile.B : SignatureProfile.parse(s);
    }
}
