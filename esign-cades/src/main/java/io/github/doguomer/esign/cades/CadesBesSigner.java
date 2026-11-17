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
import java.time.Clock;
import java.util.Objects;

/** CAdES-BES (ETSI baseline B): Word, Excel, ZIP ve diğer tüm dosyalar için. */
public final class CadesBesSigner {

    private final DssTokenSigner signer;

    public CadesBesSigner(SigningToken token) {
        this(token, Clock.systemUTC());
    }

    CadesBesSigner(SigningToken token, Clock clock) {
        this.signer = new DssTokenSigner(token, clock);
    }

    /** @return DER kodlu CMS SignedData (CAdES-BES) */
    public byte[] sign(byte[] content, String fileName, String alias, CadesPackaging packaging)
            throws TokenException {
        Objects.requireNonNull(content, "content");
        CAdESSignatureParameters params = signer.prepare(new CAdESSignatureParameters(), alias);
        params.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        params.setSignaturePackaging(packaging == CadesPackaging.ATTACHED
                ? SignaturePackaging.ENVELOPING : SignaturePackaging.DETACHED);
        return signer.sign(new CAdESService(new CommonCertificateVerifier()),
                new InMemoryDocument(content, fileName), params, alias);
    }
}
