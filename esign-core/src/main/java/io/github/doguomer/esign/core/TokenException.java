package io.github.doguomer.esign.core;

/** Token erişim hatası. Mesajlar PIN veya gizli veri içermemelidir. */
public class TokenException extends Exception {
    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
