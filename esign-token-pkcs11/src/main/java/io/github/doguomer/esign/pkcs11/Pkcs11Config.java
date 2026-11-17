package io.github.doguomer.esign.pkcs11;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SunPKCS11 yapılandırması.
 *
 * @param library       PKCS#11 kütüphanesi (ör. AKİS için libakisp11.so)
 * @param slotListIndex okuyucu/slot sırası (0 = ilk kartlı slot)
 */
public record Pkcs11Config(Path library, int slotListIndex) {

    private static final Pattern SAFE_PATH = Pattern.compile("[^\\r\\n\"]+");

    public Pkcs11Config {
        Objects.requireNonNull(library, "library");
        if (slotListIndex < 0) {
            throw new IllegalArgumentException("slotListIndex negatif olamaz");
        }
        String p = library.toAbsolutePath().toString();
        if (!SAFE_PATH.matcher(p).matches()) {
            // Yapılandırma enjeksiyonunu önler (satır sonu / tırnak).
            throw new IllegalArgumentException("Geçersiz kütüphane yolu");
        }
    }

    /**
     * SunPKCS11 yapılandırma ayrıştırıcısı tırnak içindeki ters bölüleri kaçış karakteri sayar
     * ("C:\\Windows\\System32" bozulur). Windows yükleyicisi düz bölüyü de kabul ettiği için çevrilir.
     * SunPKCS11's parser treats backslashes as escapes; Windows accepts forward slashes.
     */
    String configPath() {
        return library.toAbsolutePath().toString().replace('\\', '/');
    }

    /** SunPKCS11 satır içi yapılandırma metni ("--" önekli). */
    String toSunPkcs11Config() {
        return "--name=esign-slot" + slotListIndex + "\n"
                + "library=\"" + configPath() + "\"\n"
                + "slotListIndex=" + slotListIndex + "\n";
    }
}
