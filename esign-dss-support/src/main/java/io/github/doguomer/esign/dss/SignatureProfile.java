package io.github.doguomer.esign.dss;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import java.util.Locale;

/**
 * ETSI baseline imza profilleri. ETSI baseline signature levels.
 *
 * <ul>
 *   <li>B (BES): temel imza</li>
 *   <li>T: B + RFC 3161 imza zaman damgası</li>
 *   <li>LT: T + sertifika zinciri ve iptal (OCSP/CRL) verileri imzaya gömülü</li>
 *   <li>LTA: LT + arşiv zaman damgası (uzun dönem doğrulama)</li>
 * </ul>
 */
public enum SignatureProfile {
    B("BES"), T("T"), LT("LT"), LTA("LTA");

    private final String label;

    SignatureProfile(String label) {
        this.label = label;
    }

    /** Kullanıcıya gösterilen ad: BES, T, LT, LTA. */
    public String label() {
        return label;
    }

    public boolean needsTimestamp() {
        return this != B;
    }

    public boolean needsRevocationData() {
        return this == LT || this == LTA;
    }

    /** @param format "CAdES", "PAdES" veya "XAdES" */
    public SignatureLevel level(String format) {
        return SignatureLevel.valueOf(format + "_BASELINE_" + name());
    }

    /** "bes", "b", "t", "lt", "lta" kabul eder. */
    public static SignatureProfile parse(String s) {
        String v = s.strip().toUpperCase(Locale.ROOT);
        if (v.equals("BES")) {
            return B;
        }
        try {
            return valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Profil BES, T, LT veya LTA olmalı: " + s);
        }
    }
}
