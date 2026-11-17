package io.github.doguomer.esign.desktop;

import io.github.doguomer.esign.dss.SignatureProfile;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Masaüstü imza seçenekleri. Desktop signing options.
 *
 * @param tsaUrl  boşsa zaman damgası yok
 * @param trust   LT/LTA için güvenilir kökler (dosya veya klasör)
 */
public record SignOptions(Path input, Format format, SignatureProfile profile, boolean detached, boolean visible,
        String reason, String tsaUrl, Optional<Path> trust) {

    /** İmza biçimi. */
    public enum Format {
        OTOMATIK("Otomatik (uzantıya göre)"), CADES("CAdES (.imz / .p7s)"), PADES("PAdES (PDF içine)"),
        XADES("XAdES (XML içine)");

        private final String label;

        Format(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public Format resolve(Path file) {
            if (this != OTOMATIK) {
                return this;
            }
            String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".pdf")) {
                return PADES;
            }
            if (n.endsWith(".xml")) {
                return XADES;
            }
            return CADES;
        }
    }

    public SignOptions {
        Objects.requireNonNull(input, "input");
        format = format == null ? Format.OTOMATIK : format;
        profile = profile == null ? SignatureProfile.B : profile;
        trust = trust == null ? Optional.empty() : trust;
        Format f = format.resolve(input);
        if (detached && f != Format.CADES) {
            throw new IllegalArgumentException("Ayrık imza yalnızca CAdES biçiminde kullanılabilir.");
        }
        if (visible && f != Format.PADES) {
            throw new IllegalArgumentException("Görünür imza yalnızca PDF (PAdES) için kullanılabilir.");
        }
    }

    public Format effectiveFormat() {
        return format.resolve(input);
    }

    /** Varsayılan çıktı adı; dosya varsa "(2)", "(3)" eklenir, hiçbir dosyanın üzerine yazılmaz. */
    public Path outputPath() {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String base;
        String ext;
        switch (effectiveFormat()) {
            case PADES -> { base = stem + "_imzali"; ext = ".pdf"; }
            case XADES -> { base = stem + "_imzali"; ext = ".xml"; }
            default -> { base = name; ext = detached ? ".p7s" : ".imz"; }
        }
        Path p = input.resolveSibling(base + ext);
        for (int i = 2; java.nio.file.Files.exists(p); i++) {
            p = input.resolveSibling(base + " (" + i + ")" + ext);
        }
        return p;
    }
}
