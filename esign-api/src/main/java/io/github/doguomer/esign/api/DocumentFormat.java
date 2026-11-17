package io.github.doguomer.esign.api;

import java.util.Locale;

/** İmza biçimi; AUTO dosya adına göre seçer (.pdf → PAdES, .xml → XAdES, diğerleri → CAdES). */
public enum DocumentFormat {
    AUTO, CADES, PADES, XADES;

    public static DocumentFormat parse(String s) {
        if (s == null || s.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(s.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("format auto|cades|pades|xades olmalı: " + s);
        }
    }

    public DocumentFormat resolve(String fileName) {
        if (this != AUTO) {
            return this;
        }
        String n = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) {
            return PADES;
        }
        if (n.endsWith(".xml")) {
            return XADES;
        }
        return CADES;
    }

    /** DSS adı: "CAdES", "PAdES", "XAdES". */
    public String dssName() {
        return switch (this) {
            case PADES -> "PAdES";
            case XADES -> "XAdES";
            default -> "CAdES";
        };
    }
}
