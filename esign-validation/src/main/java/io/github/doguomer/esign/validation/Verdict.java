package io.github.doguomer.esign.validation;

import eu.europa.esig.dss.enumerations.Indication;

/** ETSI EN 319 102-1 sonucu, sade dille. Simplified ETSI EN 319 102-1 outcome. */
public enum Verdict {
    /** TOTAL_PASSED: imza geçerli. */
    VALID("GEÇERLİ", "valid"),
    /** INDETERMINATE: karar verilemedi (ör. güvenilir kök yok, iptal bilgisi alınamadı). */
    INDETERMINATE("BELİRSİZ", "indeterminate"),
    /** TOTAL_FAILED: imza geçersiz (belge değişmiş, sertifika iptal edilmiş vb.). */
    INVALID("GEÇERSİZ", "invalid");

    private final String tr;
    private final String en;

    Verdict(String tr, String en) {
        this.tr = tr;
        this.en = en;
    }

    public String label() {
        return tr + " / " + en;
    }

    static Verdict of(Indication i) {
        if (i == null) {
            return INDETERMINATE;
        }
        return switch (i) {
            case TOTAL_PASSED, PASSED -> VALID;
            case TOTAL_FAILED, FAILED -> INVALID;
            default -> INDETERMINATE;
        };
    }
}
