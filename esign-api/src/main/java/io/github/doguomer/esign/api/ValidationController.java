package io.github.doguomer.esign.api;

import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.validation.SignatureResult;
import io.github.doguomer.esign.validation.SignatureVerifier;
import io.github.doguomer.esign.validation.VerificationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/validations")
@Tag(name = "Doğrulama / Validation", description = "ETSI EN 319 102-1 imza doğrulaması (EU DSS)")
class ValidationController {

    private final SignatureVerifier verifier;
    private final TrustSettings trust;

    ValidationController(SignatureVerifier verifier, TrustSettings trust) {
        this.verifier = verifier;
        this.trust = trust;
    }

    record Signature(String id, String verdict, String indication, String subIndication, String signedBy,
            String format, Instant claimedSigningTime, Instant timestampTime, int timestampCount,
            List<String> errors, List<String> warnings) {
        static Signature of(SignatureResult s) {
            return new Signature(s.id(), s.verdict().name(), s.indication(), s.subIndication().orElse(null),
                    s.signedBy(), s.format(), s.claimedSigningTime().orElse(null), s.timestampTime().orElse(null),
                    s.timestampCount(), s.errors(), s.warnings());
        }
    }

    /**
     * @param overall          VALID, INDETERMINATE veya INVALID
     * @param trustAnchorsConfigured false ise zincir güvenilir köke bağlanamaz (sonuç en iyi BELİRSİZ olur)
     */
    record ValidationResponse(String overall, String overallLabel, boolean trustAnchorsConfigured,
            List<Signature> signatures, String detailedReportXml) {}

    @Operation(summary = "İmzalı belgeyi doğrula / Validate",
            description = "CAdES (.imz/.p7s), PAdES (PDF) veya XAdES (XML). Ayrık .p7s için orijinal belgeyi "
                    + "'original' alanında gönderin.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ValidationResponse validate(
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "original", required = false) MultipartFile original,
            @Parameter(description = "true: DSS ayrıntılı XML raporunu ekle")
            @RequestParam(name = "detailed", defaultValue = "false") boolean detailed) throws IOException, TokenException {
        VerificationReport r = verifier.verify(file.getBytes(), SigningController.safeName(file.getOriginalFilename()),
                original == null ? null : original.getBytes(),
                original == null ? null : SigningController.safeName(original.getOriginalFilename()));
        return new ValidationResponse(r.overall().name(), r.overall().label(), !trust.anchors().isEmpty(),
                r.signatures().stream().map(Signature::of).toList(), detailed ? r.detailedReportXml() : null);
    }
}
