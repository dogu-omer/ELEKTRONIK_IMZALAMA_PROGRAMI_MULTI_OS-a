package io.github.doguomer.esign.cli;

import io.github.doguomer.esign.validation.SignatureResult;
import io.github.doguomer.esign.validation.VerificationReport;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Doğrulama raporunun insan okunur (TR/EN) metni. Human-readable verification summary. */
final class VerificationText {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx");

    private VerificationText() {}

    static String render(VerificationReport r, boolean noTrustAnchors, ZoneId zone) {
        StringBuilder sb = new StringBuilder();
        int n = r.signatures().size();
        if (n == 0) {
            sb.append("İmza bulunamadı / No signature found\n");
        }
        for (int i = 0; i < n; i++) {
            SignatureResult s = r.signatures().get(i);
            sb.append("İmza / Signature ").append(i + 1).append('/').append(n).append(": ")
                    .append(s.verdict().label()).append('\n');
            line(sb, "İmzalayan / Signed by", s.signedBy());
            line(sb, "Profil / Level", s.format());
            line(sb, "ETSI sonucu / Indication",
                    s.indication() + s.subIndication().map(x -> " (" + x + ")").orElse(""));
            s.claimedSigningTime().ifPresent(t ->
                    line(sb, "Beyan edilen zaman / Claimed time", fmt(t, zone) + " (kanıt değil / not proof)"));
            line(sb, "Zaman damgası / Timestamp", s.timestampTime()
                    .map(t -> fmt(t, zone) + " (" + s.timestampCount() + " adet / total)")
                    .orElse("yok / none"));
            s.errors().forEach(e -> sb.append("  Hata / Error: ").append(e).append('\n'));
            s.warnings().forEach(w -> sb.append("  Uyarı / Warning: ").append(w).append('\n'));
        }
        sb.append("Sonuç / Overall: ").append(r.overall().label()).append('\n');
        if (noTrustAnchors) {
            sb.append("Not: güvenilir kök verilmedi (--trust / ESIGN_TRUST_DIR); sertifika zinciri doğrulanamaz.\n")
                    .append("Note: no trust anchors given; the certificate chain cannot be validated.\n");
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append("  ").append(key).append(": ").append(value).append('\n');
    }

    private static String fmt(Instant t, ZoneId zone) {
        return TIME.format(t.atZone(zone));
    }
}
