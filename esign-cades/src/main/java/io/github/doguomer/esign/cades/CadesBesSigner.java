package io.github.doguomer.esign.cades;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.Timestamping;
import java.time.Clock;
import java.util.Objects;

/** CAdES-BES (ETSI baseline B): Word, Excel, ZIP ve diğer tüm dosyalar için. */
public final class CadesBesSigner {

    private final DssTokenSigner signer;

    public CadesBesSigner(SigningToken token) {
        this(token, Timestamping.none());
    }

    /** @param timestamping {@link Timestamping#none()} → BES, TSA verilirse → T profili */
    public CadesBesSigner(SigningToken token, Timestamping timestamping) {
        this(token, Clock.systemUTC(), timestamping);
    }

    CadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    CadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES" veya "T". */
    public String profile() {
        return signer.timestamping().profile();
    }

    /** @return DER kodlu CMS SignedData (CAdES-BES) */
    public byte[] sign(byte[] content, String fileName, String alias, CadesPackaging packaging)
            throws TokenException {
        Objects.requireNonNull(content, "content");
        CAdESSignatureParameters params = signer.prepare(new CAdESSignatureParameters(), alias);
        params.setSignatureLevel(signer.timestamping().level(SignatureLevel.CAdES_BASELINE_B, SignatureLevel.CAdES_BASELINE_T));
        params.setSignaturePackaging(packaging == CadesPackaging.ATTACHED
                ? SignaturePackaging.ENVELOPING : SignaturePackaging.DETACHED);
        return signer.sign(signer.timestamping().apply(new CAdESService(new CommonCertificateVerifier())),
                new InMemoryDocument(content, fileName), params, alias);
    }
}
