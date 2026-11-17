package io.github.doguomer.esign.pades;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import java.time.Clock;
import java.util.Objects;

/**
 * PAdES-BES (ETSI baseline B): imza PDF'in içine artımlı güncelleme (incremental update)
 * olarak eklenir. Önceki imzalar korunur; böylece aynı PDF'i birden fazla kişi imzalayabilir.
 * Görünür imza alanı sonraki bir sürümde eklenecek.
 */
public final class PadesBesSigner {

    private final DssTokenSigner signer;

    public PadesBesSigner(SigningToken token) {
        this(token, Clock.systemUTC());
    }

    PadesBesSigner(SigningToken token, Clock clock) {
        this.signer = new DssTokenSigner(token, clock);
    }

    /**
     * @param reason  imza nedeni (PDF imza sözlüğündeki /Reason), boş bırakılabilir
     * @return imzalı PDF baytları
     */
    public byte[] sign(byte[] pdf, String fileName, String alias, String reason) throws TokenException {
        Objects.requireNonNull(pdf, "pdf");
        if (pdf.length < 5 || !new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new TokenException("Dosya bir PDF değil: " + fileName);
        }
        PAdESSignatureParameters params = signer.prepare(new PAdESSignatureParameters(), alias);
        params.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        if (reason != null && !reason.isBlank()) {
            params.setReason(reason);
        }
        return signer.sign(new PAdESService(new CommonCertificateVerifier()),
                new InMemoryDocument(pdf, fileName), params, alias);
    }
}
