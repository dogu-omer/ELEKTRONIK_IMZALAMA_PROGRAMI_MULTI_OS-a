package io.github.doguomer.esign.core;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Port: imza aracı (akıllı kart, USB token, HSM).
 * Uygulamalar özel anahtarı asla dışarı vermemelidir.
 * Port for a signing device; implementations must never export private keys.
 */
public interface SigningToken extends AutoCloseable {

    /** Token üzerindeki tüm X.509 sertifikalarını listeler. */
    List<CertificateInfo> listCertificates() throws TokenException;

    /** Alias'a ait sertifika zinciri (ilk eleman imzacı sertifikası). */
    List<X509Certificate> certificateChain(String alias) throws TokenException;

    /**
     * Veriyi token üzerindeki özel anahtarla imzalar; anahtar token dışına çıkmaz.
     *
     * @param jcaAlgorithm ör. "SHA256withRSA", "SHA256withECDSA"
     * @return ham imza değeri
     */
    byte[] sign(String alias, byte[] data, String jcaAlgorithm) throws TokenException;

    /** Kaynakları bırakır ve oturumu kapatır; istisna fırlatmaz. */
    @Override
    void close();
}
