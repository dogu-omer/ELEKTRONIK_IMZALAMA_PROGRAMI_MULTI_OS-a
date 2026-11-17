package io.github.doguomer.esign.core;

import java.util.List;

/**
 * Port: imza aracı (akıllı kart, USB token, HSM).
 * Uygulamalar özel anahtarı asla dışarı vermemelidir.
 * Port for a signing device; implementations must never export private keys.
 */
public interface SigningToken extends AutoCloseable {

    /** Token üzerindeki tüm X.509 sertifikalarını listeler. */
    List<CertificateInfo> listCertificates() throws TokenException;

    /** Kaynakları bırakır ve oturumu kapatır; istisna fırlatmaz. */
    @Override
    void close();
}
