package io.github.doguomer.esign.pades;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
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
        this(token, Timestamping.none());
    }

    /** @param timestamping {@link Timestamping#none()} → BES, TSA verilirse → T profili */
    public PadesBesSigner(SigningToken token, Timestamping timestamping) {
        this(token, Clock.systemUTC(), timestamping);
    }

    /** Profil (BES/T/LT/LTA), zaman damgası ve güven/iptal kaynaklarıyla. */
    public PadesBesSigner(SigningToken token, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, Clock.systemUTC(), settings);
    }

    PadesBesSigner(SigningToken token, Clock clock, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, clock, settings);
    }

    PadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    PadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES", "T", "LT" veya "LTA". */
    public String profile() {
        return signer.settings().profile().label();
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
        params.setSignatureLevel(signer.level("PAdES"));
        if (reason != null && !reason.isBlank()) {
            params.setReason(reason);
        }
        return signer.sign(signer.service(PAdESService::new),
                new InMemoryDocument(pdf, fileName), params, alias);
    }
}
