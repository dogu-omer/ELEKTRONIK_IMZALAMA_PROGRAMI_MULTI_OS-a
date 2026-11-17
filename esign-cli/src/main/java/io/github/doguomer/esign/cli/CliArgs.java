package io.github.doguomer.esign.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Ayrıştırılmış komut satırı. PIN bilerek argüman olarak kabul edilmez
 * (süreç listesinde görünür); --login ile terminalden sorulur.
 */
record CliArgs(String command, Optional<Path> library, int slot, boolean login) {

    static final String USAGE = """
            Kullanım / Usage:
              esign list-certs [--lib <pkcs11.so>] [--slot <n>] [--login]
              esign help

            Seçenekler:
              --lib    PKCS#11 kütüphanesi. Verilmezse ESIGN_PKCS11_LIB veya bilinen AKİS yolları denenir.
              --slot   Slot sırası (varsayılan 0).
              --login  PIN sorar; özel anahtar eşleşmesi için gerekebilir. PIN asla argüman olarak verilmez.
            """;

    static CliArgs parse(List<String> argv) {
        if (argv.isEmpty()) {
            return new CliArgs("help", Optional.empty(), 0, false);
        }
        String cmd = argv.get(0);
        Optional<Path> lib = Optional.empty();
        int slot = 0;
        boolean login = false;
        for (int i = 1; i < argv.size(); i++) {
            String a = argv.get(i);
            switch (a) {
                case "--lib" -> lib = Optional.of(Path.of(value(argv, ++i, a)));
                case "--slot" -> slot = parseSlot(value(argv, ++i, a));
                case "--login" -> login = true;
                case "--pin" -> throw new IllegalArgumentException(
                        "--pin desteklenmez: PIN süreç listesinde görünür. --login kullanın.");
                default -> throw new IllegalArgumentException("Bilinmeyen seçenek: " + a);
            }
        }
        return new CliArgs(cmd, lib, slot, login);
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
