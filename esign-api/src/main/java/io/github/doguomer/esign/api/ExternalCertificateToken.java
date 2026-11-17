package io.github.doguomer.esign.api;

import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * İstemcinin gönderdiği sertifika zinciri; özel anahtar sunucuda yoktur, imza istemcide (kartta) üretilir.
 * Certificate chain supplied by the client. The private key never reaches the server.
 */
final class ExternalCertificateToken implements SigningToken {

    static final String ALIAS = "external";
    private final List<X509Certificate> chain;

    ExternalCertificateToken(List<X509Certificate> chain) {
        if (chain == null || chain.isEmpty()) {
            throw new IllegalArgumentException("İmzacı sertifikası gerekli");
        }
        this.chain = List.copyOf(chain);
    }

    @Override
    public List<CertificateInfo> listCertificates() {
        return List.of(CertificateInfo.of(ALIAS, chain.get(0), false));
    }

    @Override
    public List<X509Certificate> certificateChain(String alias) {
        return chain;
    }

    @Override
    public byte[] sign(String alias, byte[] data, String jcaAlgorithm) throws TokenException {
        throw new TokenException("Sunucu imza üretmez; imza istemcideki kartta oluşturulmalıdır");
    }

    @Override
    public void close() {}
}
