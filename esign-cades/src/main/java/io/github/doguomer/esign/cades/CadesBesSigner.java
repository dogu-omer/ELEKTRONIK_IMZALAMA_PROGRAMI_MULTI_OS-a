package io.github.doguomer.esign.cades;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.InMemoryDocument;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.SignatureSettings;
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

    /** Profil (BES/T/LT/LTA), zaman damgası ve güven/iptal kaynaklarıyla. */
    public CadesBesSigner(SigningToken token, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, Clock.systemUTC(), settings);
    }

    CadesBesSigner(SigningToken token, Clock clock, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, clock, settings);
    }

    CadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    CadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES", "T", "LT" veya "LTA". */
    public String profile() {
        return signer.settings().profile().label();
    }

    /** @return DER kodlu CMS SignedData (CAdES-BES) */
    public byte[] sign(byte[] content, String fileName, String alias, CadesPackaging packaging)
            throws TokenException {
        Objects.requireNonNull(content, "content");
        CAdESSignatureParameters params = signer.prepare(new CAdESSignatureParameters(), alias);
        params.setSignatureLevel(signer.level("CAdES"));
        params.setSignaturePackaging(packaging == CadesPackaging.ATTACHED
                ? SignaturePackaging.ENVELOPING : SignaturePackaging.DETACHED);
        return signer.sign(signer.service(CAdESService::new),
                new InMemoryDocument(content, fileName), params, alias);
    }
}
