package io.github.doguomer.esign.xades;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.SignatureSettings;
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

    /** Profil (BES/T/LT/LTA), zaman damgası ve güven/iptal kaynaklarıyla. */
    public XadesBesSigner(SigningToken token, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, Clock.systemUTC(), settings);
    }

    XadesBesSigner(SigningToken token, Clock clock, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, clock, settings);
    }

    XadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    XadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES", "T", "LT" veya "LTA". */
    public String profile() {
        return signer.settings().profile().label();
    }

    /** @return imzalı XML baytları */
    public byte[] sign(byte[] xml, String fileName, String alias) throws TokenException {
        Objects.requireNonNull(xml, "xml");
        XAdESSignatureParameters params = signer.prepare(new XAdESSignatureParameters(), alias);
        params.setSignatureLevel(signer.level("XAdES"));
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        try {
            return signer.sign(signer.service(XAdESService::new),
                    new InMemoryDocument(xml, fileName), params, alias);
        } catch (TokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new TokenException("XML imzalanamadı (geçerli bir XML belgesi mi?): " + e.getMessage(), e);
        }
    }
}
