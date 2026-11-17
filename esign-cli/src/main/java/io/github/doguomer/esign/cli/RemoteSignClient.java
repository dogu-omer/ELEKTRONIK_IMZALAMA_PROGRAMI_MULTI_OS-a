package io.github.doguomer.esign.cli;

import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * esign-api istemcisi: hazırla → kartta imzala → birleştir. Sunucuya yalnızca belge ve sertifika zinciri gider;
 * PIN ve özel anahtar bu bilgisayardan çıkmaz. Client for the two-step signing API.
 */
final class RemoteSignClient {

    record Result(byte[] content, String level) {}

    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private final URI server;
    private final String apiKey;
    private final HttpClient http;

    RemoteSignClient(URI server, String apiKey) {
        Objects.requireNonNull(server, "server");
        String scheme = server.getScheme() == null ? "" : server.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Sunucu adresi http(s) olmalı: " + server);
        }
        if (server.getUserInfo() != null) {
            throw new IllegalArgumentException("Sunucu adresine kullanıcı adı/şifre yazmayın; ESIGN_API_KEY kullanın");
        }
        this.server = URI.create(server.toString().replaceAll("/+$", "") + "/");
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.strip();
        if (this.apiKey != null && scheme.equals("http") && !isLoopback(server.getHost())) {
            throw new IllegalArgumentException("API anahtarı şifresiz http ile uzak sunucuya gönderilmez; https kullanın "
                    + "/ refusing to send the API key over plain http to a remote host");
        }
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT).replaceAll("^\\[|\\]$", "");
        return h.equals("localhost") || h.equals("::1") || h.startsWith("127.");
    }

    URI server() {
        return server;
    }

    Result sign(SigningToken token, String alias, byte[] document, String fileName, String format, String level,
            boolean detached, String reason) throws TokenException, IOException {
        String chain = pem(token.certificateChain(alias));
        var form = new Multipart();
        form.file("file", fileName, "application/octet-stream", document);
        form.file("certificate", "chain.pem", "application/x-pem-file", chain.getBytes(StandardCharsets.US_ASCII));
        StringBuilder q = new StringBuilder("api/v1/signatures/prepare?format=").append(enc(format))
                .append("&level=").append(enc(level)).append("&detached=").append(detached);
        if (reason != null && !reason.isBlank()) {
            q.append("&reason=").append(enc(reason));
        }
        HttpResponse<byte[]> prep = send(request(q.toString())
                .header("Content-Type", "multipart/form-data; boundary=" + form.boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(form.finish())).build());
        String json = new String(prep.body(), StandardCharsets.UTF_8);
        String id = field(json, "id");
        byte[] tbs = Base64.getDecoder().decode(field(json, "dataToSign"));
        String algorithm = field(json, "signatureAlgorithm");
        if (!id.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new IOException("Sunucudan geçersiz oturum kimliği");
        }
        byte[] value = token.sign(alias, tbs, algorithm);
        String body = "{\"signatureValue\":\"" + Base64.getEncoder().encodeToString(value) + "\"}";
        HttpResponse<byte[]> done = send(request("api/v1/signatures/" + id + "/complete")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        return new Result(done.body(), done.headers().firstValue("X-Signature-Level").orElse("?"));
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder(server.resolve(path)).timeout(TIMEOUT)
                .header("Accept", "application/json, application/octet-stream, */*");
        if (apiKey != null) {
            b.header("X-API-Key", apiKey);
        }
        return b;
    }

    private HttpResponse<byte[]> send(HttpRequest r) throws IOException {
        try {
            HttpResponse<byte[]> resp = http.send(r, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                String body = new String(resp.body(), StandardCharsets.UTF_8);
                String detail = body.contains("\"detail\"") ? fieldOrNull(body, "detail") : null;
                throw new IOException("Sunucu hatası " + resp.statusCode()
                        + (detail == null ? "" : ": " + detail));
            }
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("İstek kesildi", e);
        } catch (java.net.ConnectException e) {
            throw new IOException("Sunucuya bağlanılamadı: " + server, e);
        }
    }

    static String field(String json, String name) throws IOException {
        String v = fieldOrNull(json, name);
        if (v == null) {
            throw new IOException("Sunucu yanıtında '" + name + "' yok");
        }
        return v;
    }

    private static String fieldOrNull(String json, String name) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String pem(java.util.List<X509Certificate> chain) throws TokenException {
        StringBuilder sb = new StringBuilder();
        try {
            for (X509Certificate c : chain) {
                sb.append("-----BEGIN CERTIFICATE-----\n")
                        .append(Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                                .encodeToString(c.getEncoded()))
                        .append("\n-----END CERTIFICATE-----\n");
            }
        } catch (CertificateEncodingException e) {
            throw new TokenException("Sertifika kodlanamadı", e);
        }
        return sb.toString();
    }

    /** Basit multipart/form-data gövdesi. */
    private static final class Multipart {
        final String boundary;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Multipart() {
            byte[] r = new byte[12];
            new SecureRandom().nextBytes(r);
            boundary = "esign-" + HexFormat.of().formatHex(r);
        }

        void file(String field, String fileName, String type, byte[] data) {
            String safe = fileName.replaceAll("[\\r\\n\"]", "_");
            String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field
                    + "\"; filename=\"" + safe + "\"\r\nContent-Type: " + type + "\r\n\r\n";
            out.writeBytes(head.getBytes(StandardCharsets.UTF_8));
            out.writeBytes(data);
            out.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
        }

        byte[] finish() {
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
            return out.toByteArray();
        }
    }
}
