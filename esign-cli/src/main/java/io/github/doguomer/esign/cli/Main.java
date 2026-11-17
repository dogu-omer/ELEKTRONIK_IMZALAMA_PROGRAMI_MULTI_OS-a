package io.github.doguomer.esign.cli;

import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.pkcs11.Pkcs11Config;
import io.github.doguomer.esign.pkcs11.Pkcs11LibraryLocator;
import io.github.doguomer.esign.pkcs11.Pkcs11SigningToken;
import java.io.Console;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** esign komut satırı giriş noktası. Çıkış kodları: 0 başarı, 1 token hatası, 2 kullanım hatası. */
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
            case "list-certs" -> listCerts(a, out, err);
            default -> {
                err.println("Bilinmeyen komut: " + a.command());
                err.print(CliArgs.USAGE);
                yield 2;
            }
        };
    }

    private static int listCerts(CliArgs a, PrintStream out, PrintStream err) {
        var locator = new Pkcs11LibraryLocator();
        Path lib = a.library().or(locator::locate).orElse(null);
        if (lib == null) {
            err.println("PKCS#11 kütüphanesi bulunamadı. --lib verin veya "
                    + Pkcs11LibraryLocator.ENV_VAR + " ayarlayın.");
            err.println("Denenen yollar: " + locator.candidates());
            return 1;
        }
        char[] pin = null;
        if (a.login()) {
            Console console = System.console();
            if (console == null) {
                err.println("--login için etkileşimli terminal gerekir.");
                return 2;
            }
            pin = console.readPassword("PIN: ");
        }
        try (var token = Pkcs11SigningToken.open(new Pkcs11Config(lib, a.slot()), pin)) {
            out.print(CertificateTable.render(token.listCertificates(), Instant.now()));
            return 0;
        } catch (TokenException e) {
            err.println("Hata: " + e.getMessage());
            return 1;
        }
    }
}
