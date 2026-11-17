package io.github.doguomer.esign.cli;

import io.github.doguomer.esign.cades.CadesBesSigner;
import io.github.doguomer.esign.cades.CadesPackaging;
import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.pkcs11.Pkcs11Config;
import io.github.doguomer.esign.pkcs11.Pkcs11LibraryLocator;
import io.github.doguomer.esign.pkcs11.Pkcs11SigningToken;
import io.github.doguomer.esign.pades.PadesBesSigner;
import io.github.doguomer.esign.xades.XadesBesSigner;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/** esign komut satırı giriş noktası. Çıkış kodları: 0 başarı, 1 token/G-Ç hatası, 2 kullanım hatası. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(List.of(args), System.out, System.err));
    }

    static int run(List<String> argv, PrintStream out, PrintStream err) {
        CliArgs a;
        try {
            a = CliArgs.parse(argv);
        } catch (IllegalArgumentException e) {
            err.println("Hata: " + e.getMessage());
            err.print(CliArgs.USAGE);
            return 2;
        }
        return switch (a.command()) {
            case "help", "--help", "-h" -> {
                out.print(CliArgs.USAGE);
                yield 0;
            }
            case "version", "--version" -> {
                out.println("esign " + version());
                yield 0;
            }
            case "list-certs" -> listCerts(a, out, err);
            case "sign" -> sign(a, out, err);
            default -> {
                err.println("Bilinmeyen komut: " + a.command());
                err.print(CliArgs.USAGE);
                yield 2;
            }
        };
    }

    private static int listCerts(CliArgs a, PrintStream out, PrintStream err) {
        Path lib = resolveLibrary(a, err);
        if (lib == null) {
            return 1;
        }
        char[] pin = null;
        if (a.login()) {
            pin = readPin(err);
            if (pin == null) {
                return 2;
            }
        }
        try (var token = Pkcs11SigningToken.open(new Pkcs11Config(lib, a.slot()), pin)) {
            out.print(CertificateTable.render(token.listCertificates(), Instant.now()));
            return 0;
        } catch (TokenException e) {
            err.println("Hata: " + e.getMessage());
            return 1;
        }
    }

    private static int sign(CliArgs a, PrintStream out, PrintStream err) {
        Path in = a.input().orElseThrow();
        Path target = a.outputPath();
        if (!Files.isRegularFile(in)) {
            err.println("Hata: dosya bulunamadı: " + in);
            return 1;
        }
        if (Files.exists(target)) {
            err.println("Hata: çıktı zaten var, üzerine yazılmaz: " + target);
            return 1;
        }
        Timestamping timestamping;
        try {
            timestamping = timestamping(a.tsa(), System.getenv(TSA_ENV));
        } catch (IllegalArgumentException e) {
            err.println("Hata: " + e.getMessage());
            return 2;
        }
        Path lib = resolveLibrary(a, err);
        if (lib == null) {
            return 1;
        }
        char[] pin = readPin(err);
        if (pin == null) {
            return 2;
        }
        try (var token = Pkcs11SigningToken.open(new Pkcs11Config(lib, a.slot()), pin)) {
            String alias = chooseAlias(token, a, err);
            if (alias == null) {
                return 1;
            }
            byte[] content = Files.readAllBytes(in);
            String name = in.getFileName().toString();
            SignFormat format = a.effectiveFormat();
            byte[] signed = switch (format) {
                case PADES -> new PadesBesSigner(token, timestamping).sign(content, name, alias, a.reason().orElse(null));
                case XADES -> new XadesBesSigner(token, timestamping).sign(content, name, alias);
                default -> new CadesBesSigner(token, timestamping).sign(content, name, alias,
                        a.detached() ? CadesPackaging.DETACHED : CadesPackaging.ATTACHED);
            };
            Files.write(target, signed, StandardOpenOption.CREATE_NEW);
            out.println("İmzalandı (" + format.label() + "-" + timestamping.profile()
                    + (a.detached() ? ", ayrık" : "") + "): " + target);
            if (!timestamping.enabled()) {
                out.println("Not: zaman damgası yok (BES). T profili için --tsa <url> verin.");
            }
            return 0;
        } catch (TokenException | IOException e) {
            err.println("Hata: " + e.getMessage());
            return 1;
        }
    }

    static final String TSA_ENV = "ESIGN_TSA_URL";

    /** --tsa önceliklidir; yoksa ortam değişkeni; ikisi de yoksa zaman damgası kullanılmaz (BES). */
    static Timestamping timestamping(java.util.Optional<String> cli, String env) {
        return cli.or(() -> java.util.Optional.ofNullable(env).filter(v -> !v.isBlank()))
                .map(String::strip)
                .map(u -> {
                    try {
                        return Timestamping.online(java.net.URI.create(u));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(e.getMessage().startsWith("Zaman")
                                ? e.getMessage() : "Geçersiz zaman damgası adresi: " + u);
                    }
                })
                .orElse(Timestamping.none());
    }

    static String version() {
        String v = Main.class.getPackage().getImplementationVersion();
        return v == null ? "dev" : v;
    }

    /** --alias verilmemişse imzaya uygun tek sertifikayı seçer; birden fazlaysa kullanıcıdan seçim ister. */
    static String chooseAlias(SigningToken token, CliArgs a, PrintStream err) throws TokenException {
        List<CertificateInfo> candidates = token.listCertificates().stream()
                .filter(CertificateInfo::isSigningCandidate)
                .filter(c -> a.alias().map(c.alias()::equals).orElse(true))
                .toList();
        if (candidates.size() == 1) {
            return candidates.get(0).alias();
        }
        if (candidates.isEmpty()) {
            err.println("Hata: imzaya uygun sertifika bulunamadı"
                    + a.alias().map(x -> " (alias: " + x + ")").orElse("") + ". esign list-certs --login ile kontrol edin.");
        } else {
            err.println("Hata: birden fazla imza sertifikası var; --alias ile seçin: "
                    + candidates.stream().map(CertificateInfo::alias).toList());
        }
        return null;
    }

    private static Path resolveLibrary(CliArgs a, PrintStream err) {
        var locator = new Pkcs11LibraryLocator();
        Path lib = a.library().or(locator::locate).orElse(null);
        if (lib == null) {
            err.println("Hata: PKCS#11 kütüphanesi bulunamadı. --lib verin veya "
                    + Pkcs11LibraryLocator.ENV_VAR + " ayarlayın.");
            err.println("Denenen yollar: " + locator.candidates());
        }
        return lib;
    }

    private static char[] readPin(PrintStream err) {
        Console console = System.console();
        if (console == null) {
            err.println("Hata: PIN için etkileşimli terminal gerekir.");
            return null;
        }
        return console.readPassword("PIN: ");
    }
}
