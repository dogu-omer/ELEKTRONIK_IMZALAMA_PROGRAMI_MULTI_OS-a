package io.github.doguomer.esign.pkcs11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * PKCS#11 kütüphanesini bulur. Önce ESIGN_PKCS11_LIB ortam değişkeni,
 * sonra işletim sistemine göre bilinen AKİS kurulum yolları denenir.
 *
 * Not: Aday yollar AKİS sürücü paketinin olağan kurulum yerleridir;
 * sürüme göre değişebilir, bu yüzden ortam değişkeni her zaman önceliklidir.
 */
public final class Pkcs11LibraryLocator {

    public static final String ENV_VAR = "ESIGN_PKCS11_LIB";

    static final List<String> LINUX_CANDIDATES = List.of(
            "/usr/lib/libakisp11.so",
            "/usr/local/lib/libakisp11.so",
            "/usr/lib/x86_64-linux-gnu/libakisp11.so",
            "/usr/lib64/libakisp11.so");

    static final List<String> MAC_CANDIDATES = List.of(
            "/usr/local/lib/libakisp11.dylib",
            "/Library/AKIS/libakisp11.dylib");

    private final Function<String, String> env;
    private final Predicate<Path> exists;
    private final String osName;

    public Pkcs11LibraryLocator() {
        this(System::getenv, Files::isRegularFile, System.getProperty("os.name", ""));
    }

    Pkcs11LibraryLocator(Function<String, String> env, Predicate<Path> exists, String osName) {
        this.env = env;
        this.exists = exists;
        this.osName = osName.toLowerCase(Locale.ROOT);
    }

    public Optional<Path> locate() {
        String fromEnv = env.apply(ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path p = Path.of(fromEnv.trim());
            return exists.test(p) ? Optional.of(p) : Optional.empty();
        }
        return candidates().stream().map(Path::of).filter(exists).findFirst();
    }

    public List<String> candidates() {
        if (osName.contains("mac")) {
            return MAC_CANDIDATES;
        }
        if (osName.contains("linux")) {
            return LINUX_CANDIDATES;
        }
        return List.of();
    }
}
