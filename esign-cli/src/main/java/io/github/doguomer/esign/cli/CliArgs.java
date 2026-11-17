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
        boolean detached,
        SignFormat format,
        Optional<String> reason,
        Optional<String> tsa,
        Optional<String> level,
        Optional<Path> trust,
        boolean offline,
        Optional<Path> original,
        Optional<Path> report,
        Optional<String> visible,
        Optional<String> server) {

    static final String USAGE = """
            Kullanım / Usage:
              esign list-certs [--lib <pkcs11.so>] [--slot <n>] [--login]
              esign sign --in <dosya> [--out <dosya>] [--format auto|cades|pades|xades] [--detached]
                         [--reason <metin>] [--alias <etiket>] [--tsa <url>] [--lib <pkcs11.so>] [--slot <n>]
                         [--level bes|t|lt|lta] [--trust <dosya|dizin>] [--offline]
                         [--visible] [--visible-at son|<sayfa>|<sayfa>,<x>,<y>] [--server <url>]
              esign verify --in <imzalı dosya> [--original <dosya>] [--trust <dosya|dizin>] [--offline]
                         [--report <rapor.xml>]
              esign gui          (Türkçe masaüstü arayüzü / desktop UI)
              esign version
              esign help

            Seçenekler:
              --lib       PKCS#11 kütüphanesi. Verilmezse ESIGN_PKCS11_LIB veya bilinen AKİS yolları denenir.
              --slot      Slot sırası (varsayılan 0).
              --login     list-certs için PIN sorar (sign her zaman sorar).
              --in        İmzalanacak dosya (Word, Excel, PDF, ZIP, TXT ...).
              --format    auto (varsayılan): .pdf → PAdES, .xml → XAdES, diğer dosyalar → CAdES.
              --out       Çıktı. Varsayılan: CAdES <dosya>.imz / <dosya>.p7s, PAdES <ad>_imzali.pdf,
                          XAdES <ad>_imzali.xml.
              --detached  Ayrık CAdES imza; orijinal dosya ayrıca saklanmalıdır.
              --reason    PDF imza nedeni (yalnızca PAdES).
              --alias     Kullanılacak sertifika etiketi. Verilmezse imzaya uygun tek sertifika seçilir.
              --tsa       RFC 3161 zaman damgası sunucusu (ESHS). Verilirse imza T profilinde (BES + zaman
                          damgası) üretilir; verilmezse ESIGN_TSA_URL denenir, o da yoksa BES üretilir.
              --level     İmza profili: bes, t (zaman damgalı), lt (+ OCSP/CRL), lta (+ arşiv zaman damgası).
                          Verilmezse --tsa varsa t, yoksa bes. lt/lta için --tsa ve --trust gerekir.
              --trust     Güvenilir kök sertifika(lar): PEM/DER dosyası veya dizin (.crt .cer .pem .der).
                          Verilmezse ESIGN_TRUST_DIR denenir. Ör. Kamu SM kök sertifikaları.
              --offline   OCSP/CRL/AIA ağ sorgusu yapma (yalnızca imzanın içindeki veriler kullanılır).
              --visible   PAdES: son sayfanın sağ altına görünür imza kutusu (ad, zaman, neden).
              --visible-at Görünür imzanın yeri; x/y puan cinsinden, sayfanın sol üstünden.
              --server    esign-api sunucusu (ör. http://localhost:8080). İmza iki adımda üretilir: sunucu
                          hazırlar, kart bu bilgisayarda imzalar, sunucu birleştirir. Belge sunucuya gönderilir;
                          PIN ve özel anahtar gönderilmez. ESIGN_API_KEY tanımlıysa X-API-Key olarak eklenir.
              --original  verify: ayrık (.p7s) imzanın orijinal dosyası.
              --report    verify: ETSI EN 319 102-1 ayrıntılı XML raporunu bu dosyaya yaz.
            verify çıkış kodları: 0 geçerli, 3 belirsiz, 4 geçersiz, 1 hata, 2 kullanım hatası.
            PIN asla argüman olarak verilmez.
            """;

    static CliArgs parse(List<String> argv) {
        if (argv.isEmpty()) {
            return new CliArgs("help", Optional.empty(), 0, false, Optional.empty(), Optional.empty(),
                    Optional.empty(), false, SignFormat.AUTO, Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
        String cmd = argv.get(0);
        Optional<Path> lib = Optional.empty();
        Optional<Path> in = Optional.empty();
        Optional<Path> out = Optional.empty();
        Optional<String> alias = Optional.empty();
        int slot = 0;
        boolean login = false;
        boolean detached = false;
        SignFormat format = SignFormat.AUTO;
        Optional<String> reason = Optional.empty();
        Optional<String> tsa = Optional.empty();
        Optional<String> level = Optional.empty();
        Optional<Path> trust = Optional.empty();
        boolean offline = false;
        Optional<Path> original = Optional.empty();
        Optional<Path> report = Optional.empty();
        Optional<String> visible = Optional.empty();
        Optional<String> server = Optional.empty();
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
                case "--format" -> format = SignFormat.parse(value(argv, ++i, a));
                case "--reason" -> reason = Optional.of(value(argv, ++i, a));
                case "--tsa" -> tsa = Optional.of(value(argv, ++i, a));
                case "--level" -> level = Optional.of(value(argv, ++i, a));
                case "--trust" -> trust = Optional.of(Path.of(value(argv, ++i, a)));
                case "--offline" -> offline = true;
                case "--original" -> original = Optional.of(Path.of(value(argv, ++i, a)));
                case "--report" -> report = Optional.of(Path.of(value(argv, ++i, a)));
                case "--visible" -> visible = visible.or(() -> Optional.of(""));
                case "--visible-at" -> visible = Optional.of(value(argv, ++i, a));
                case "--server" -> server = Optional.of(value(argv, ++i, a));
                case "--pin" -> throw new IllegalArgumentException(
                        "--pin desteklenmez: PIN süreç listesinde görünür. Terminalden sorulur.");
                default -> throw new IllegalArgumentException("Bilinmeyen seçenek: " + a);
            }
        }
        if ((cmd.equals("sign") || cmd.equals("verify")) && in.isEmpty()) {
            throw new IllegalArgumentException(cmd + " için --in gerekli");
        }
        if (cmd.equals("verify")) {
            return new CliArgs(cmd, lib, slot, login, in, out, alias, detached, format, reason, tsa,
                    level, trust, offline, original, report, visible, server);
        }
        if (original.isPresent() || report.isPresent()) {
            throw new IllegalArgumentException("--original ve --report yalnızca verify için geçerlidir");
        }
        if (server.isPresent() && (tsa.isPresent() || trust.isPresent() || offline || visible.isPresent())) {
            throw new IllegalArgumentException("--server ile --tsa, --trust, --offline ve --visible kullanılmaz; "
                    + "zaman damgası ve güven ayarları sunucudadır");
        }
        if (detached && in.isPresent() && format.resolve(in.get()) != SignFormat.CADES) {
            throw new IllegalArgumentException("--detached yalnızca CAdES için geçerlidir (--format cades)");
        }
        if (visible.isPresent() && in.isPresent() && format.resolve(in.get()) != SignFormat.PADES) {
            throw new IllegalArgumentException("--visible yalnızca PDF (PAdES) imzası için geçerlidir");
        }
        return new CliArgs(cmd, lib, slot, login, in, out, alias, detached, format, reason, tsa,
                level, trust, offline, original, report, visible, server);
    }

    /** Dosyaya uygulanacak gerçek biçim. */
    SignFormat effectiveFormat() {
        return format.resolve(input.orElseThrow());
    }

    /** Varsayılan çıktı yolu. */
    Path outputPath() {
        Path src = input.orElseThrow();
        String name = src.getFileName().toString();
        return output.orElseGet(() -> switch (effectiveFormat()) {
            case PADES -> src.resolveSibling(stem(name) + "_imzali.pdf");
            case XADES -> src.resolveSibling(stem(name) + "_imzali.xml");
            default -> src.resolveSibling(name + (detached ? ".p7s" : ".imz"));
        });
    }

    private static String stem(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
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
