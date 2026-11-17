package io.github.doguomer.esign.pkcs11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /** Windows'ta AKİS sürücüsü DLL'i genellikle System32'ye kurar; Program Files altı yedek adaydır. */
    static final String WINDOWS_DLL = "akisp11.dll";

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
        if (osName.contains("win")) {
            return windowsCandidates();
        }
        return List.of();
    }

    /** Ortam değişkenlerinden (SystemRoot, ProgramFiles) Windows aday yollarını kurar. */
    List<String> windowsCandidates() {
        String sysRoot = envOr("SystemRoot", envOr("windir", "C:\\Windows"));
        List<String> c = new ArrayList<>();
        c.add(sysRoot + "\\System32\\" + WINDOWS_DLL);
        for (String pf : new String[] {envOr("ProgramFiles", "C:\\Program Files"), env.apply("ProgramW6432")}) {
            if (pf == null || pf.isBlank()) {
                continue;
            }
            for (String dir : new String[] {"AKIS", "TUBITAK\\AKIS", "AKIS\\x64"}) {
                String cand = pf + "\\" + dir + "\\" + WINDOWS_DLL;
                if (!c.contains(cand)) {
                    c.add(cand);
                }
            }
        }
        return List.copyOf(c);
    }

    private String envOr(String key, String def) {
        String v = env.apply(key);
        return v == null || v.isBlank() ? def : v.strip();
    }
}
