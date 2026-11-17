package io.github.doguomer.esign.desktop;

import io.github.doguomer.esign.cades.CadesBesSigner;
import io.github.doguomer.esign.cades.CadesPackaging;
import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.TrustSettings;
import io.github.doguomer.esign.pades.PadesBesSigner;
import io.github.doguomer.esign.pades.VisibleSignature;
import io.github.doguomer.esign.validation.SignatureVerifier;
import io.github.doguomer.esign.validation.VerificationReport;
import io.github.doguomer.esign.validation.VerificationText;
import io.github.doguomer.esign.xades.XadesBesSigner;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Arayüzden bağımsız iş mantığı (test edilebilir). UI-independent logic. */
public final class DesktopService {

    /** Doğrulama sonucu: rapor ve okunur metin. */
    public record VerifyOutcome(VerificationReport report, String text) {}

    /** İmzaya uygun (nonRepudiation, süresi geçerli) sertifikalar. */
    public List<CertificateInfo> signingCandidates(SigningToken token) throws TokenException {
        return token.listCertificates().stream().filter(CertificateInfo::isSigningCandidate).toList();
    }

    /** Ayarlar kart açılmadan önce denetlenir; hatalı ayarda PIN boşuna sorulmaz. */
    public SignatureSettings settings(SignOptions o) throws IOException {
        Timestamping ts = o.tsaUrl() == null || o.tsaUrl().isBlank()
                ? Timestamping.none() : Timestamping.online(URI.create(o.tsaUrl().strip()));
        TrustSettings trust = TrustSettings.none();
        if (o.profile().needsRevocationData()) {
            if (o.trust().isEmpty()) {
                throw new IllegalArgumentException(o.profile().label()
                        + " profili için güvenilir kök sertifika klasörü seçin.");
            }
            trust = TrustSettings.of(TrustSettings.load(o.trust().get()), true);
        }
        try {
            return new SignatureSettings(o.profile(), ts, trust);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage().replace("(--tsa)", "(zaman damgası adresi)")
                    .replace("(--trust <dosya|klasör>)", "(güvenilir kökler)"), e);
        }
    }

    /** İmzalar ve yeni dosyaya yazar; mevcut dosyaların üzerine yazmaz. */
    public Path sign(SigningToken token, String alias, SignOptions o) throws TokenException, IOException {
        return sign(token, alias, o, settings(o));
    }

    Path sign(SigningToken token, String alias, SignOptions o, SignatureSettings s) throws TokenException, IOException {
        byte[] content = Files.readAllBytes(o.input());
        String name = o.input().getFileName().toString();
        String reason = o.reason() == null || o.reason().isBlank() ? null : o.reason().strip();
        byte[] signed = switch (o.effectiveFormat()) {
            case PADES -> new PadesBesSigner(token, s).sign(content, name, alias, reason,
                    o.visible() ? Optional.of(VisibleSignature.defaults()) : Optional.empty());
            case XADES -> new XadesBesSigner(token, s).sign(content, name, alias);
            default -> new CadesBesSigner(token, s).sign(content, name, alias,
                    o.detached() ? CadesPackaging.DETACHED : CadesPackaging.ATTACHED);
        };
        Path out = o.outputPath();
        Files.write(out, signed, StandardOpenOption.CREATE_NEW);
        return out;
    }

    public VerifyOutcome verify(Path signed, Optional<Path> original, Optional<Path> trustPath, boolean offline)
            throws TokenException, IOException {
        List<X509Certificate> anchors = trustPath.isPresent() ? TrustSettings.load(trustPath.get()) : List.of();
        return verify(signed, original, TrustSettings.of(anchors, !offline));
    }

    VerifyOutcome verify(Path signed, Optional<Path> original, TrustSettings trust) throws TokenException, IOException {
        byte[] orig = original.isPresent() ? Files.readAllBytes(original.get()) : null;
        VerificationReport r = new SignatureVerifier(trust).verify(Files.readAllBytes(signed),
                signed.getFileName().toString(), orig, original.map(p -> p.getFileName().toString()).orElse(null));
        return new VerifyOutcome(r, VerificationText.render(r, trust.anchors().isEmpty(), ZoneId.systemDefault())
                .replace("(--trust / ESIGN_TRUST_DIR)", "(Güvenilir kökler alanı)"));
    }
}
