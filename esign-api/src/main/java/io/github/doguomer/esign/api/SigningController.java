package io.github.doguomer.esign.api;

import io.github.doguomer.esign.core.TokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/signatures")
@Tag(name = "İmza / Signing", description = "İki aşamalı imza: hazırla → istemcide kartla imzala → birleştir")
class SigningController {

    private final RemoteSigningService service;

    SigningController(RemoteSigningService service) {
        this.service = service;
    }

    /** Hazırlık yanıtı; baytlar Base64. */
    record PrepareResponse(String id, String dataToSign, String digestSha256, String signatureAlgorithm,
            String format, String level, Instant expiresAt) {}

    /** İstemcinin kartta ürettiği imza değeri (Base64). */
    record CompleteRequest(String signatureValue) {}

    @Operation(summary = "İmzayı hazırla / Prepare",
            description = "Belge ve imzacının sertifika zinciri (PEM/DER, ilk sertifika imzacı) gönderilir. "
                    + "Yanıttaki dataToSign baytları istemcide signatureAlgorithm ile kartta imzalanır.")
    @PostMapping(path = "/prepare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    PrepareResponse prepare(
            @RequestPart("file") MultipartFile file,
            @RequestPart("certificate") MultipartFile certificate,
            @Parameter(description = "auto|cades|pades|xades") @RequestParam(name = "format", defaultValue = "auto") String format,
            @Parameter(description = "bes|t|lt|lta") @RequestParam(name = "level", defaultValue = "bes") String level,
            @Parameter(description = "CAdES ayrık (.p7s)") @RequestParam(name = "detached", defaultValue = "false") boolean detached,
            @Parameter(description = "PAdES imza nedeni") @RequestParam(name = "reason", required = false) String reason)
            throws IOException, TokenException {
        var p = service.prepare(new RemoteSigningService.PrepareRequest(file.getBytes(),
                safeName(file.getOriginalFilename()), Certificates.parse(certificate.getBytes()),
                DocumentFormat.parse(format), detached, ApiConfig.profile(level), reason));
        var b64 = Base64.getEncoder();
        return new PrepareResponse(p.id(), b64.encodeToString(p.dataToSign()), b64.encodeToString(p.digest()),
                p.signatureAlgorithm(), p.format(), p.level(), p.expiresAt());
    }

    @Operation(summary = "İmzayı birleştir / Complete",
            description = "Kartta üretilen imza değeri doğrulanır, imzalı belge döndürülür. Oturum tek kullanımlıktır.")
    @PostMapping(path = "/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<byte[]> complete(@PathVariable("id") String id, @RequestBody CompleteRequest body) throws TokenException {
        if (body == null || body.signatureValue() == null || body.signatureValue().isBlank()) {
            throw new IllegalArgumentException("signatureValue gerekli");
        }
        byte[] value;
        try {
            value = Base64.getDecoder().decode(body.signatureValue().strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("signatureValue Base64 olmalı");
        }
        var s = service.complete(id, value);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(s.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Signature-Level", s.level())
                .body(s.content());
    }

    @Operation(summary = "Hazırlanmış imzayı iptal et / Cancel")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> cancel(@PathVariable("id") String id) {
        return service.cancel(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Yol bileşenlerini atar; yalnızca dosya adı kalır. */
    static String safeName(String original) {
        if (original == null || original.isBlank()) {
            return "belge";
        }
        String n = original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_").strip();
        return n.isEmpty() || n.equals(".") || n.equals("..") ? "belge" : n;
    }
}
