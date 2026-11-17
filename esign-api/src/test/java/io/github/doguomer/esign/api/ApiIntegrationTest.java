package io.github.doguomer.esign.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.jayway.jsonpath.JsonPath;
import io.github.doguomer.esign.dss.testing.TestPki;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP uçtan uca: hazırla → (istemci kartı) → birleştir → doğrula; API anahtarı; OpenAPI. */
@SpringBootTest(properties = {"esign.api-key=test-anahtari-en-az-24-karakter", "esign.offline=true"})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static TestPki pki;
    private static final String KEY = "test-anahtari-en-az-24-karakter";

    @Autowired MockMvc mvc;

    @BeforeAll
    static void setUp() throws Exception {
        pki = new TestPki();
    }

    @Test
    void prepareSignCompleteAndValidate() throws Exception {
        var file = new MockMultipartFile("file", "belge.txt", "text/plain",
                "İmzalanacak metin".getBytes(StandardCharsets.UTF_8));
        var cert = new MockMultipartFile("certificate", "zincir.pem", "application/x-pem-file",
                TestData.chainPem(pki).getBytes(StandardCharsets.US_ASCII));
        String json = mvc.perform(multipart("/api/v1/signatures/prepare").file(file).file(cert)
                        .header(ApiKeyFilter.HEADER, KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatureAlgorithm").value("SHA256withRSA"))
                .andExpect(jsonPath("$.level").value("CAdES-BES"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        byte[] tbs = Base64.getDecoder().decode((String) JsonPath.read(json, "$.dataToSign"));
        String value = Base64.getEncoder().encodeToString(TestData.signOnCard(pki, tbs, "SHA256withRSA"));

        byte[] signed = mvc.perform(post("/api/v1/signatures/" + id + "/complete")
                        .header(ApiKeyFilter.HEADER, KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signatureValue\":\"" + value + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("belge.txt.imz")))
                .andExpect(header().string("X-Signature-Level", "CAdES-BES"))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(signed.length > 100);

        mvc.perform(multipart("/api/v1/validations")
                        .file(new MockMultipartFile("file", "belge.txt.imz", "application/pkcs7-mime", signed))
                        .header(ApiKeyFilter.HEADER, KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatures", hasSize(1)))
                .andExpect(jsonPath("$.signatures[0].signedBy", containsString("TEST")))
                .andExpect(jsonPath("$.trustAnchorsConfigured").value(false))
                .andExpect(jsonPath("$.overall").value("INDETERMINATE"));

        mvc.perform(post("/api/v1/signatures/" + id + "/complete")
                        .header(ApiKeyFilter.HEADER, KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signatureValue\":\"" + value + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void timestampProfileWithoutTsaIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/v1/signatures/prepare")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", new byte[] {1}))
                        .file(new MockMultipartFile("certificate", "c.pem", "text/plain",
                                TestData.chainPem(pki).getBytes(StandardCharsets.US_ASCII)))
                        .param("level", "t").header(ApiKeyFilter.HEADER, KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ESIGN_TSA_URL")));
    }

    @Test
    void missingApiKeyIsRejected() throws Exception {
        mvc.perform(multipart("/api/v1/validations")
                        .file(new MockMultipartFile("file", "x", "application/octet-stream", new byte[] {1})))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/validations").header(ApiKeyFilter.HEADER, "yanlis")
                        .file(new MockMultipartFile("file", "x", "application/octet-stream", new byte[] {1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageIsUnprocessable() throws Exception {
        mvc.perform(multipart("/api/v1/validations").header(ApiKeyFilter.HEADER, KEY)
                        .file(new MockMultipartFile("file", "x.bin", "application/octet-stream", new byte[] {1, 2, 3})))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void openApiAndHealthArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/signatures/prepare']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/validations']").exists());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void safeNameStripsPaths() {
        assertEquals("evil.txt", SigningController.safeName("../../etc/evil.txt"));
        assertEquals("a.pdf", SigningController.safeName("C:\\x\\a.pdf"));
        assertEquals("belge", SigningController.safeName(".."));
    }
}
