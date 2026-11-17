package io.github.doguomer.esign.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Tüm yanıtlara güvenlik başlıkları; /api/** yanıtları (imzalı belgeler) önbelleğe alınmaz. */
final class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (req.getRequestURI().startsWith("/api/")) {
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }
        chain.doFilter(req, res);
    }
}
