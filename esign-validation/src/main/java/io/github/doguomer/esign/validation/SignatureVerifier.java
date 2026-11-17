package io.github.doguomer.esign.validation;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.jaxb.object.Message;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.TrustSettings;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CAdES (.imz/.p7s), PAdES (PDF) ve XAdES (XML) imzalarını ETSI EN 319 102-1'e göre doğrular:
 * belge bütünlüğü, imza değeri, sertifika zinciri, güvenilir kök, iptal durumu (OCSP/CRL) ve
 * zaman damgaları. Varsayılan DSS doğrulama politikası kullanılır.
 * Validates signatures per ETSI EN 319 102-1 with the default DSS validation policy.
 */
public final class SignatureVerifier {

    private final TrustSettings trust;

    public SignatureVerifier(TrustSettings trust) {
        this.trust = Objects.requireNonNull(trust, "trust");
    }

    /**
     * @param signed          imzalı dosya (.imz, .p7s, PDF, XML)
     * @param detachedContent ayrık CAdES (.p7s) için orijinal belge; diğerlerinde null
     */
    public VerificationReport verify(byte[] signed, String name, byte[] detachedContent, String contentName)
            throws TokenException {
        Objects.requireNonNull(signed, "signed");
        SignedDocumentValidator validator;
        try {
            validator = SignedDocumentValidator.fromDocument(new InMemoryDocument(signed, name));
        } catch (RuntimeException e) {
            throw new TokenException("İmzalı belge tanınmadı (CAdES/PAdES/XAdES değil): " + e.getMessage(), e);
        }
        validator.setCertificateVerifier(trust.certificateVerifier());
        if (detachedContent != null) {
            DSSDocument original = new InMemoryDocument(detachedContent, contentName);
            validator.setDetachedContents(List.of(original));
        }
        Reports reports;
        try {
            reports = validator.validateDocument();
        } catch (RuntimeException e) {
            throw new TokenException("Doğrulama yapılamadı: " + e.getMessage(), e);
        }
        SimpleReport simple = reports.getSimpleReport();
        List<SignatureResult> results = simple.getSignatureIdList().stream().map(id -> toResult(simple, id)).toList();
        return new VerificationReport(results, reports.getXmlDetailedReport(), reports.getXmlSimpleReport());
    }

    private static SignatureResult toResult(SimpleReport r, String id) {
        var indication = r.getIndication(id);
        var sub = Optional.ofNullable(r.getSubIndication(id)).map(Enum::name);
        SignatureLevel level = r.getSignatureFormat(id);
        var timestamps = r.getSignatureTimestamps(id);
        Optional<Instant> claimed = Optional.ofNullable(r.getSigningTime(id)).map(Date::toInstant);
        Optional<Instant> poe = timestamps == null || timestamps.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(r.getBestSignatureTime(id)).map(Date::toInstant);
        return new SignatureResult(
                id,
                Verdict.of(indication),
                indication == null ? "?" : indication.name(),
                sub,
                Optional.ofNullable(r.getSignedBy(id)).orElse("?"),
                level == null ? "?" : level.toString(),
                claimed,
                poe,
                timestamps == null ? 0 : timestamps.size(),
                messages(r.getAdESValidationErrors(id)),
                messages(r.getAdESValidationWarnings(id)));
    }

    private static List<String> messages(List<Message> m) {
        return m == null ? List.of() : m.stream().map(Message::getValue).distinct().toList();
    }
}
