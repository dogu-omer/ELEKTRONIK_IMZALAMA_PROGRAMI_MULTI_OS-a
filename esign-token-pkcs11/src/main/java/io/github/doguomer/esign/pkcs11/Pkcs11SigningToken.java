package io.github.doguomer.esign.pkcs11;

import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import java.security.AuthProvider;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.security.auth.login.LoginException;

/**
 * JDK SunPKCS11 sağlayıcısı üzerinden PKCS#11 token erişimi.
 * Sağlayıcı global Security listesine eklenmez; yalnızca bu nesne kullanır.
 */
public final class Pkcs11SigningToken implements SigningToken {

    private final Provider provider;
    private final KeyStore keyStore;

    private Pkcs11SigningToken(Provider provider, KeyStore keyStore) {
        this.provider = provider;
        this.keyStore = keyStore;
    }

    /**
     * Token'ı açar.
     *
     * @param pin null ise oturum açılmaz (yalnızca açık nesneler okunur).
     *            Verilen dizi çağrıdan sonra sıfırlanır.
     */
    public static Pkcs11SigningToken open(Pkcs11Config config, char[] pin) throws TokenException {
        Objects.requireNonNull(config, "config");
        try {
            Provider base = Security.getProvider("SunPKCS11");
            if (base == null) {
                throw new TokenException("Bu Java çalışma ortamında SunPKCS11 sağlayıcısı yok");
            }
            Provider p = base.configure(config.toSunPkcs11Config());
            KeyStore ks = KeyStore.getInstance("PKCS11", p);
            ks.load(null, pin);
            return new Pkcs11SigningToken(p, ks);
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            if (pin == null && hasCause(e, LoginException.class)) {
                throw new TokenException("Bu token sertifikaları okumak için PIN istiyor; --login ile tekrar deneyin.", e);
            }
            throw new TokenException("Token açılamadı: " + rootMessage(e)
                    + " (kart takılı mı, pcscd çalışıyor mu, kütüphane yolu doğru mu?)", e);
        } finally {
            if (pin != null) {
                Arrays.fill(pin, '\0');
            }
        }
    }

    @Override
    public List<CertificateInfo> listCertificates() throws TokenException {
        try {
            List<CertificateInfo> out = new ArrayList<>();
            for (String alias : Collections.list(keyStore.aliases())) {
                Certificate c = keyStore.getCertificate(alias);
                if (c instanceof X509Certificate x509) {
                    out.add(CertificateInfo.of(alias, x509, keyStore.isKeyEntry(alias)));
                }
            }
            return List.copyOf(out);
        } catch (Exception e) {
            throw new TokenException("Sertifikalar okunamadı: " + rootMessage(e), e);
        }
    }

    @Override
    public void close() {
        if (provider instanceof AuthProvider ap) {
            try {
                ap.logout();
            } catch (LoginException ignored) {
                // Oturum zaten kapalı olabilir.
            }
        }
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        return r.getClass().getSimpleName() + (r.getMessage() == null ? "" : ": " + r.getMessage());
    }
}
