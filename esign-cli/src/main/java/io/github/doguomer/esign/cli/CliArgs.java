package io.github.doguomer.esign.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Ayrıştırılmış komut satırı. PIN bilerek argüman olarak kabul edilmez
 * (süreç listesinde görünür); terminalden sorulur.
 */
record CliArgs(
        String command,
        Optional<Path> library,
        int slot,
        boolean login,
        Optional<Path> input,
        Optional<Path> output,
        Optional<String> alias,
        boolean detached) {

    static final String USAGE = """
            Kullanım / Usage:
              esign list-certs [--lib <pkcs11.so>] [--slot <n>] [--login]
              esign sign --in <dosya> [--out <dosya>] [--detached] [--alias <etiket>] [--lib <pkcs11.so>] [--slot <n>]
              esign help

            Seçenekler:
              --lib       PKCS#11 kütüphanesi. Verilmezse ESIGN_PKCS11_LIB veya bilinen AKİS yolları denenir.
              --slot      Slot sırası (varsayılan 0).
              --login     list-certs için PIN sorar (sign her zaman sorar).
              --in        İmzalanacak dosya (Word, Excel, PDF, ZIP, TXT ...).
              --out       Çıktı. Varsayılan: <dosya>.imz (bütünleşik) veya <dosya>.p7s (--detached).
              --detached  Ayrık imza; orijinal dosya ayrıca saklanmalıdır.
              --alias     Kullanılacak sertifika etiketi. Verilmezse imzaya uygun tek sertifika seçilir.
            PIN asla argüman olarak verilmez.
            """;

    static CliArgs parse(List<String> argv) {
        if (argv.isEmpty()) {
            return new CliArgs("help", Optional.empty(), 0, false, Optional.empty(), Optional.empty(),
                    Optional.empty(), false);
        }
        String cmd = argv.get(0);
        Optional<Path> lib = Optional.empty();
        Optional<Path> in = Optional.empty();
        Optional<Path> out = Optional.empty();
        Optional<String> alias = Optional.empty();
        int slot = 0;
        boolean login = false;
        boolean detached = false;
        for (int i = 1; i < argv.size(); i++) {
            String a = argv.get(i);
            switch (a) {
                case "--lib" -> lib = Optional.of(Path.of(value(argv, ++i, a)));
                case "--slot" -> slot = parseSlot(value(argv, ++i, a));
                case "--login" -> login = true;
                case "--in" -> in = Optional.of(Path.of(value(argv, ++i, a)));
                case "--out" -> out = Optional.of(Path.of(value(argv, ++i, a)));
                case "--alias" -> alias = Optional.of(value(argv, ++i, a));
                case "--detached" -> detached = true;
                case "--pin" -> throw new IllegalArgumentException(
                        "--pin desteklenmez: PIN süreç listesinde görünür. Terminalden sorulur.");
                default -> throw new IllegalArgumentException("Bilinmeyen seçenek: " + a);
            }
        }
        if (cmd.equals("sign") && in.isEmpty()) {
            throw new IllegalArgumentException("sign için --in gerekli");
        }
        return new CliArgs(cmd, lib, slot, login, in, out, alias, detached);
    }

    /** Varsayılan çıktı yolu. */
    Path outputPath() {
        Path src = input.orElseThrow();
        return output.orElseGet(() -> src.resolveSibling(src.getFileName() + (detached ? ".p7s" : ".imz")));
    }

    private static String value(List<String> argv, int i, String opt) {
        if (i >= argv.size()) {
            throw new IllegalArgumentException(opt + " bir değer bekler");
        }
        return argv.get(i);
    }

    private static int parseSlot(String s) {
        try {
            int n = Integer.parseInt(s);
            if (n < 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--slot negatif olmayan bir tam sayı olmalı: " + s);
        }
    }
}
