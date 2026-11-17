package io.github.doguomer.esign.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/** ESIGN_API_KEY tanımlıysa X-API-Key başlığını sabit zamanlı karşılaştırır. Constant-time API key check. */
final class ApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";
    private final byte[] expected;

    ApiKeyFilter(String apiKey) {
        this.expected = apiKey == null ? new byte[0] : apiKey.strip().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String given = req.getHeader(HEADER);
        if (expected.length == 0 || (given != null
                && MessageDigest.isEqual(expected, given.strip().getBytes(StandardCharsets.UTF_8)))) {
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"title\":\"Yetkisiz\",\"status\":401,"
                + "\"detail\":\"Geçerli X-API-Key başlığı gerekli / valid X-API-Key header required\"}");
    }
}
