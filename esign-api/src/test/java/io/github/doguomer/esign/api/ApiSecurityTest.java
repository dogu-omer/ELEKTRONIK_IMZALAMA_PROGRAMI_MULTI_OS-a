package io.github.doguomer.esign.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** API anahtarı (özetli), kapalı-güvenli başlatma, istek sınırı ve güvenlik başlıkları. */
class ApiSecurityTest {

    static final String KEY = "Zx9-rotasyon-anahtari-0123456789";
    static final String KEY_HASH = HexFormat.of().formatHex(ApiKeyFilter.sha256(KEY));

    @Test
    void hashedKeysMatchInConstantTimeAndSupportRotation() {
        var f = ApiKeyFilter.of(null, "00".repeat(32) + " , " + KEY_HASH.toUpperCase());
        assertTrue(f.matches(KEY));
        assertTrue(f.matches("  " + KEY + " "));
        assertFalse(f.matches(KEY + "x"));
        assertFalse(f.matches(""));
        assertFalse(f.matches(null));
    }

    @Test
    void weakOrMalformedKeysAreRejectedAtStartup() {
        assertThrows(IllegalArgumentException.class, () -> ApiKeyFilter.of("kisa", null));
        assertThrows(IllegalArgumentException.class, () -> ApiKeyFilter.of(null, "abc"));
        assertThrows(IllegalArgumentException.class, () -> ApiKeyFilter.of(null, "zz".repeat(32)));
        assertThrows(IllegalArgumentException.class, () -> ApiKeyFilter.of(null, " , "));
    }

    @Test
    void appRefusesToStartWithoutKey() {
        var app = new SpringApplicationBuilder(EsignApiApplication.class)
                .properties("server.port=0", "esign.offline=true");
        var e = assertThrows(Exception.class, () -> app.run().close());
        Throwable t = e;
        while (t.getCause() != null && !(t instanceof IllegalStateException)) {
            t = t.getCause();
        }
        assertTrue(t.getMessage().contains("ESIGN_API_KEY"), t.getMessage());
    }

    @Test
    void propertiesNeverPrintTheKey() {
        var p = new EsignProperties(null, null, true, KEY, null, false, null, null, null);
        assertFalse(p.toString().contains(KEY));
    }

    @Test
    void rateLimitRefillsOverTime() {
        var now = new long[] {Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()};
        var clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId z) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(now[0]); }
        };
        var f = new RateLimitFilter(3, clock);
        assertTrue(f.tryAcquire("1.2.3.4"));
        assertTrue(f.tryAcquire("1.2.3.4"));
        assertTrue(f.tryAcquire("1.2.3.4"));
        assertFalse(f.tryAcquire("1.2.3.4"));
        assertTrue(f.tryAcquire("5.6.7.8"), "başka istemci etkilenmez");
        now[0] += Duration.ofSeconds(20).toMillis();
        assertTrue(f.tryAcquire("1.2.3.4"));
        assertFalse(f.tryAcquire("1.2.3.4"));
    }
}
