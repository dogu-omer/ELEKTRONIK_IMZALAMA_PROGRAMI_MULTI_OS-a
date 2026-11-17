package io.github.doguomer.esign.cli;

import io.github.doguomer.esign.core.CertificateInfo;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Sertifika listesini okunur metne çevirir. */
final class CertificateTable {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private CertificateTable() {}

    static String render(List<CertificateInfo> certs, Instant now) {
        if (certs.isEmpty()) {
            return "Token üzerinde sertifika bulunamadı. / No certificates found.\n";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (CertificateInfo c : certs) {
            sb.append('[').append(i++).append("] ").append(c.alias()).append('\n')
              .append("    Sahip    : ").append(c.subject()).append('\n')
              .append("    Yayıncı  : ").append(c.issuer()).append('\n')
              .append("    Seri     : ").append(c.serialHex()).append('\n')
              .append("    Geçerlik : ").append(DATE.format(c.notBefore())).append(" → ")
              .append(DATE.format(c.notAfter()))
              .append(c.isTimeValidAt(now) ? " (süre geçerli)" : " (SÜRE DIŞI)").append('\n')
              .append("    SHA-256  : ").append(c.sha256Hex()).append('\n')
              .append("    İmza     : ").append(c.isSigningCandidate()
                      ? "evet (özel anahtar + nonRepudiation)"
                      : "hayır" + reason(c)).append('\n');
        }
        return sb.toString();
    }

    private static String reason(CertificateInfo c) {
        if (!c.hasPrivateKey()) {
            return " (özel anahtar görünmüyor; --login deneyin)";
        }
        return " (nonRepudiation yok; muhtemelen kimlik doğrulama sertifikası)";
    }
}
