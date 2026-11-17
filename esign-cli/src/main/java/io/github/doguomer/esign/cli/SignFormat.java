package io.github.doguomer.esign.cli;

import java.nio.file.Path;
import java.util.Locale;

/** İmza biçimi. AUTO, dosya uzantısına göre seçer: .pdf → PAdES, .xml → XAdES, diğerleri → CAdES. */
enum SignFormat {
    AUTO("Otomatik"), CADES("CAdES"), PADES("PAdES"), XADES("XAdES");

    private final String label;

    SignFormat(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }

    static SignFormat parse(String s) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("--format auto|cades|pades|xades olmalı: " + s);
        }
    }

    SignFormat resolve(Path file) {
        if (this != AUTO) {
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
