package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import io.github.doguomer.esign.dss.testing.TestPki;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** İstemci protokolü: sahte sunucu hazırlar, test kartı imzalar, sunucu imzayı açık anahtarla doğrular. */
class RemoteSignClientTest {

    private static TestPki pki;
    private HttpServer server;
    private final AtomicReference<String> seenKey = new AtomicReference<>();
    private final AtomicReference<String> seenQuery = new AtomicReference<>();
    private final byte[] tbs = "imzalanacak-veri".getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void pki() throws Exception {
        pki = new TestPki();
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/signatures/prepare", ex -> {
            seenKey.set(ex.getRequestHeaders().getFirst("X-API-Key"));
            seenQuery.set(ex.getRequestURI().getRawQuery());
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] resp;
            int code;
            if (body.contains("BEGIN CERTIFICATE") && body.contains("belge içeriği")) {
                code = 200;
                resp = ("{\"id\":\"abcDEF123_-x\",\"dataToSign\":\"" + Base64.getEncoder().encodeToString(tbs)
                        + "\",\"signatureAlgorithm\":\"SHA256withRSA\",\"level\":\"CAdES-BES\"}")
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                code = 400;
                resp = "{\"detail\":\"eksik alan\"}".getBytes(StandardCharsets.UTF_8);
            }
            ex.sendResponseHeaders(code, resp.length);
            ex.getResponseBody().write(resp);
            ex.close();
        });
        server.createContext("/api/v1/signatures/abcDEF123_-x/complete", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String b64 = body.replaceAll(".*\"signatureValue\":\"([^\"]+)\".*", "$1");
            boolean ok;
            try {
                Signature v = Signature.getInstance("SHA256withRSA");
                v.initVerify(pki.signer.getPublicKey());
                v.update(tbs);
                ok = v.verify(Base64.getDecoder().decode(b64));
            } catch (Exception e) {
                ok = false;
            }
            byte[] resp = ok ? "IMZALI".getBytes(StandardCharsets.UTF_8) : "{\"detail\":\"kötü imza\"}"
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("X-Signature-Level", "CAdES-BES");
            ex.sendResponseHeaders(ok ? 200 : 400, resp.length);
            ex.getResponseBody().write(resp);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private URI url() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void signsThroughServer() throws Exception {
        var client = new RemoteSignClient(url(), "gizli");
        var r = client.sign(pki.token(), TestPki.ALIAS, "belge içeriği".getBytes(StandardCharsets.UTF_8),
                "belge.txt", "cades", "bes", false, "Onay nedeni");
        assertArrayEquals("IMZALI".getBytes(StandardCharsets.UTF_8), r.content());
        assertEquals("CAdES-BES", r.level());
        assertEquals("gizli", seenKey.get());
        assertTrue(seenQuery.get().contains("reason=Onay+nedeni"));
    }

    @Test
    void serverErrorDetailIsShown() {
        var client = new RemoteSignClient(url(), null);
        var e = assertThrows(IOException.class, () -> client.sign(pki.token(), TestPki.ALIAS,
                "başka".getBytes(StandardCharsets.UTF_8), "a.txt", "cades", "bes", false, null));
        assertTrue(e.getMessage().contains("400") && e.getMessage().contains("eksik alan"), e.getMessage());
    }

    @Test
    void rejectsBadUrls() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteSignClient(URI.create("ftp://x"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new RemoteSignClient(URI.create("http://u:p@host"), null));
    }

    @Test
    void pemChain() throws Exception {
        String pem = RemoteSignClient.pem(List.of(pki.signer, pki.root));
        assertEquals(2, pem.split("BEGIN CERTIFICATE").length - 1);
    }

    @Test
    void serverOptionRejectsLocalTimestampSettings() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(
                List.of("sign", "--in", "a.txt", "--server", "http://x", "--tsa", "http://t")));
        assertTrue(CliArgs.parse(List.of("sign", "--in", "a.txt", "--server", "http://x")).server().isPresent());
    }
}
