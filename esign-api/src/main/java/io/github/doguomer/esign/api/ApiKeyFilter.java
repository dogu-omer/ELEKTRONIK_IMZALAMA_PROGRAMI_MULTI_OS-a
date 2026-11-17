package io.github.doguomer.esign.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * X-API-Key doğrulaması. Sunucu anahtarın kendisini değil SHA-256 özetini tutar; gelen anahtarın özeti
 * kayıtlı özetlerle sabit zamanlı karşılaştırılır (uzunluk/zamanlama sızıntısı yok). Birden çok özet
 * anahtar döndürmeye (rotation) izin verir.
 * <p>API key check: the server keeps only SHA-256 digests and compares them in constant time.
 */
final class ApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";
    /** Kaba kuvvet denemesini anlamsız kılmak için alt sınır (≈128 bit Base64). */
    static final int MIN_KEY_LENGTH = 24;

    private final List<byte[]> digests;

    private ApiKeyFilter(List<byte[]> digests) {
        if (digests.isEmpty()) {
            throw new IllegalArgumentException("En az bir API anahtarı gerekli");
        }
        this.digests = List.copyOf(digests);
    }

    /**
     * @param plainKey     ESIGN_API_KEY (düz metin, isteğe bağlı)
     * @param sha256Hashes ESIGN_API_KEY_SHA256: virgülle ayrılmış hex SHA-256 özetleri (isteğe bağlı)
     */
    static ApiKeyFilter of(String plainKey, String sha256Hashes) {
        List<byte[]> out = new ArrayList<>();
        if (plainKey != null && !plainKey.isBlank()) {
            String k = plainKey.strip();
            if (k.length() < MIN_KEY_LENGTH) {
                throw new IllegalArgumentException("ESIGN_API_KEY en az " + MIN_KEY_LENGTH
                        + " karakter olmalı (öneri: openssl rand -base64 32)");
            }
            out.add(sha256(k));
        }
        if (sha256Hashes != null) {
            for (String h : sha256Hashes.split(",")) {
                if (h.isBlank()) {
                    continue;
                }
                byte[] d;
                try {
                    d = HexFormat.of().parseHex(h.strip().toLowerCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("ESIGN_API_KEY_SHA256 geçersiz hex değer içeriyor");
                }
                if (d.length != 32) {
                    throw new IllegalArgumentException("ESIGN_API_KEY_SHA256 değerleri 64 hex karakter olmalı");
                }
                out.add(d);
            }
        }
        return new ApiKeyFilter(out);
    }

    static boolean configured(String plainKey, String sha256Hashes) {
        return (plainKey != null && !plainKey.isBlank()) || (sha256Hashes != null && !sha256Hashes.isBlank());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (matches(req.getHeader(HEADER))) {
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setHeader("WWW-Authenticate", "ApiKey header=\"" + HEADER + "\"");
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"title\":\"Yetkisiz\",\"status\":401,"
                + "\"detail\":\"Geçerli X-API-Key başlığı gerekli / valid X-API-Key header required\"}");
    }

    boolean matches(String given) {
        if (given == null || given.isBlank()) {
            return false;
        }
        byte[] d = sha256(given.strip());
        boolean ok = false;
        for (byte[] expected : digests) {
            ok |= MessageDigest.isEqual(expected, d); // tüm özetler denenir; erken çıkış yok
        }
        return ok;
    }

    static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
