package io.github.doguomer.esign.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * İstemci IP'si başına dakikalık istek sınırı (token bucket, bellek içi). Kimlik doğrulamadan ÖNCE çalışır;
 * böylece API anahtarı tahmini ve kaynak tüketimi (büyük PDF/DSS doğrulaması) sınırlanır. IP adresi yalnızca
 * bellekte, kişisel veri olarak saklanmadan ve loglanmadan tutulur (KVKK/GDPR veri en aza indirme).
 * <p>Ters vekil arkasında tüm istekler vekilin IP'sinden gelir; o durumda sınır vekilde de uygulanmalıdır.
 */
final class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_CLIENTS = 10_000;

    private static final class Bucket {
        double tokens;
        long lastNanos;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastNanos = now;
        }
    }

    private final int perMinute;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    RateLimitFilter(int perMinute, Clock clock) {
        if (perMinute < 1) {
            throw new IllegalArgumentException("ESIGN_RATE_LIMIT en az 1 olmalı");
        }
        this.perMinute = perMinute;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (tryAcquire(String.valueOf(req.getRemoteAddr()))) {
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"title\":\"Çok fazla istek\",\"status\":429,"
                + "\"detail\":\"İstek sınırı aşıldı; daha sonra tekrar deneyin / rate limit exceeded\"}");
    }

    boolean tryAcquire(String client) {
        long now = clock.millis() * 1_000_000L;
        if (buckets.size() >= MAX_CLIENTS) {
            buckets.clear(); // sınırsız bellek büyümesini önle
        }
        Bucket b = buckets.computeIfAbsent(client, k -> new Bucket(perMinute, now));
        synchronized (b) {
            double refill = (now - b.lastNanos) / 60_000_000_000d * perMinute;
            b.tokens = Math.min(perMinute, b.tokens + Math.max(0, refill));
            b.lastNanos = now;
            if (b.tokens >= 1) {
                b.tokens -= 1;
                return true;
            }
            return false;
        }
    }
}
