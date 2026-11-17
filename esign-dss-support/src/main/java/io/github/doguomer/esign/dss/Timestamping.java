package io.github.doguomer.esign.dss;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.signature.AbstractSignatureService;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * İsteğe bağlı RFC 3161 zaman damgası. Kapalıysa imza BES (ETSI baseline B), açıksa
 * T (baseline T) profilinde üretilir. Optional RFC 3161 timestamping: off → B level, on → T level.
 */
public final class Timestamping {

    private static final Timestamping NONE = new Timestamping(null, null);

    private final TSPSource source;
    private final String description;

    private Timestamping(TSPSource source, String description) {
        this.source = source;
        this.description = description;
    }

    /** Zaman damgası yok: BES. */
    public static Timestamping none() {
        return NONE;
    }

    /** RFC 3161 HTTP(S) zaman damgası sunucusu (ESHS). Kimlik bilgisi URL'ye yazılmamalıdır. */
    public static Timestamping online(URI url) {
        String scheme = url.getScheme() == null ? "" : url.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Zaman damgası adresi http(s) olmalı: " + url);
        }
        if (url.getUserInfo() != null) {
            throw new IllegalArgumentException("Zaman damgası adresine kullanıcı adı/şifre yazmayın");
        }
        return new Timestamping(new OnlineTSPSource(url.toString(), new TimestampDataLoader()), url.getHost());
    }

    /** Özel bir {@link TSPSource} (testler veya kurum içi sunucu) ile. */
    public static Timestamping of(TSPSource source, String description) {
        return new Timestamping(java.util.Objects.requireNonNull(source, "source"), description);
    }

    public boolean enabled() {
        return source != null;
    }

    /** Kısa profil adı: "BES" veya "T". */
    public String profile() {
        return enabled() ? "T" : "BES";
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** B ve T seviyelerinden uygun olanı döndürür. */
    public SignatureLevel level(SignatureLevel baselineB, SignatureLevel baselineT) {
        return enabled() ? baselineT : baselineB;
    }

    /** Servise zaman damgası kaynağını bağlar. */
    public <S extends AbstractSignatureService<?, ?>> S apply(S service) {
        if (source != null) {
            service.setTspSource(source);
        }
        return service;
    }
}
