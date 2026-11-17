package io.github.doguomer.esign.validation;

import java.util.List;

/**
 * Belgedeki tüm imzaların sonucu ve denetim için DSS ayrıntılı XML raporu.
 * All signatures plus the DSS detailed XML report for audit.
 */
public record VerificationReport(List<SignatureResult> signatures, String detailedReportXml, String simpleReportXml) {

    /** Hiç imza yoksa veya en az biri geçersizse INVALID; biri belirsizse INDETERMINATE; aksi halde VALID. */
    public Verdict overall() {
        if (signatures.isEmpty()) {
            return Verdict.INVALID;
        }
        if (signatures.stream().anyMatch(s -> s.verdict() == Verdict.INVALID)) {
            return Verdict.INVALID;
        }
        if (signatures.stream().anyMatch(s -> s.verdict() == Verdict.INDETERMINATE)) {
            return Verdict.INDETERMINATE;
        }
        return Verdict.VALID;
    }
}
