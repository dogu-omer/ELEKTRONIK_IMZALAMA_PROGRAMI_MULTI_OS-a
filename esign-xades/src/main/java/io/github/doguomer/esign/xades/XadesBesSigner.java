package io.github.doguomer.esign.xades;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.Timestamping;
import java.time.Clock;
import java.util.Objects;

/** XAdES-BES (ETSI baseline B), enveloped: imza XML belgesinin içine eklenir. */
public final class XadesBesSigner {

    private final DssTokenSigner signer;

    public XadesBesSigner(SigningToken token) {
        this(token, Timestamping.none());
    }

    /** @param timestamping {@link Timestamping#none()} → BES, TSA verilirse → T profili */
    public XadesBesSigner(SigningToken token, Timestamping timestamping) {
        this(token, Clock.systemUTC(), timestamping);
    }

    XadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    XadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES" veya "T". */
    public String profile() {
        return signer.timestamping().profile();
    }

    /** @return imzalı XML baytları */
    public byte[] sign(byte[] xml, String fileName, String alias) throws TokenException {
        Objects.requireNonNull(xml, "xml");
        XAdESSignatureParameters params = signer.prepare(new XAdESSignatureParameters(), alias);
        params.setSignatureLevel(signer.timestamping().level(SignatureLevel.XAdES_BASELINE_B, SignatureLevel.XAdES_BASELINE_T));
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        try {
            return signer.sign(signer.timestamping().apply(new XAdESService(new CommonCertificateVerifier())),
                    new InMemoryDocument(xml, fileName), params, alias);
        } catch (TokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new TokenException("XML imzalanamadı (geçerli bir XML belgesi mi?): " + e.getMessage(), e);
        }
    }
}
