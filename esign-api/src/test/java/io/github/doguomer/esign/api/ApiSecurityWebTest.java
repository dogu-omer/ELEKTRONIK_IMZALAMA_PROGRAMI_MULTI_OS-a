package io.github.doguomer.esign.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Yalnızca SHA-256 özeti yapılandırılmış sunucu; istek sınırı 3/dk; güvenlik başlıkları; hata sızıntısı. */
@SpringBootTest(properties = {"esign.offline=true", "esign.rate-limit=3",
        "esign.api-key-sha256=050269143710ae3af0a908cf14129649b5ab505393292c08f0982d4ac5c287a6"})
@AutoConfigureMockMvc
class ApiSecurityWebTest {

    @Autowired MockMvc mvc;

    @Test
    void hashedKeyRateLimitHeadersAndNoLeak() throws Exception {
        // 1) doğru anahtar (sunucu yalnızca özetini biliyor); bozuk JSON 400 olmalı, 500 değil
        mvc.perform(post("/api/v1/signatures/yok/complete").header(ApiKeyFilter.HEADER, ApiSecurityTest.KEY)
                        .remoteAddress("10.0.0.9").contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(not(containsString("Exception"))));
        // 2) yanlış anahtar
        mvc.perform(delete("/api/v1/signatures/yok").header(ApiKeyFilter.HEADER, "yanlis-anahtar-yanlis-anahtar")
                        .remoteAddress("10.0.0.9"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
        // 3) doğru anahtar, oturum yok
        mvc.perform(delete("/api/v1/signatures/yok").header(ApiKeyFilter.HEADER, ApiSecurityTest.KEY)
                        .remoteAddress("10.0.0.9"))
                .andExpect(status().isNotFound());
        // 4) sınır aşıldı (anahtar tahmini de sınırlanır)
        mvc.perform(delete("/api/v1/signatures/yok").header(ApiKeyFilter.HEADER, ApiSecurityTest.KEY)
                        .remoteAddress("10.0.0.9"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
        // başka istemci etkilenmez; sağlık uç noktası sınırsız
        mvc.perform(delete("/api/v1/signatures/yok").header(ApiKeyFilter.HEADER, ApiSecurityTest.KEY)
                        .remoteAddress("10.0.0.10"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health").remoteAddress("10.0.0.9")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
