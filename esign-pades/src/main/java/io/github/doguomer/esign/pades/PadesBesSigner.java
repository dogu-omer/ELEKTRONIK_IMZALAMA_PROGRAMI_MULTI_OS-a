package io.github.doguomer.esign.pades;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import io.github.doguomer.esign.dss.DssTokenSigner;
import io.github.doguomer.esign.dss.SignatureSettings;
import io.github.doguomer.esign.dss.Timestamping;
import eu.europa.esig.dss.enumerations.TextWrapping;
import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import java.awt.Color;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import javax.security.auth.x500.X500Principal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

/**
 * PAdES-BES (ETSI baseline B): imza PDF'in içine artımlı güncelleme (incremental update)
 * olarak eklenir. Önceki imzalar korunur; böylece aynı PDF'i birden fazla kişi imzalayabilir.
 * İsteğe bağlı görünür imza kutusu: imzacı adı ve imza zamanı (Türkçe karakter destekli gömülü yazı tipi).
 */
public final class PadesBesSigner {

    private final DssTokenSigner signer;

    public PadesBesSigner(SigningToken token) {
        this(token, Timestamping.none());
    }

    /** @param timestamping {@link Timestamping#none()} → BES, TSA verilirse → T profili */
    public PadesBesSigner(SigningToken token, Timestamping timestamping) {
        this(token, Clock.systemUTC(), timestamping);
    }

    /** Profil (BES/T/LT/LTA), zaman damgası ve güven/iptal kaynaklarıyla. */
    public PadesBesSigner(SigningToken token, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, Clock.systemUTC(), settings);
    }

    PadesBesSigner(SigningToken token, Clock clock, SignatureSettings settings) {
        this.signer = new DssTokenSigner(token, clock, settings);
    }

    PadesBesSigner(SigningToken token, Clock clock) {
        this(token, clock, Timestamping.none());
    }

    PadesBesSigner(SigningToken token, Clock clock, Timestamping timestamping) {
        this.signer = new DssTokenSigner(token, clock, timestamping);
    }

    /** Üretilecek profil: "BES", "T", "LT" veya "LTA". */
    public String profile() {
        return signer.settings().profile().label();
    }

    /**
     * @param reason  imza nedeni (PDF imza sözlüğündeki /Reason), boş bırakılabilir
     * @return imzalı PDF baytları
     */
    public byte[] sign(byte[] pdf, String fileName, String alias, String reason) throws TokenException {
        return sign(pdf, fileName, alias, reason, Optional.empty());
    }

    /** @param visible verilirse PDF üzerinde görünür imza kutusu çizilir / draws a visible signature box */
    public byte[] sign(byte[] pdf, String fileName, String alias, String reason, Optional<VisibleSignature> visible)
            throws TokenException {
        Objects.requireNonNull(pdf, "pdf");
        if (pdf.length < 5 || !new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new TokenException("Dosya bir PDF değil: " + fileName);
        }
        PAdESSignatureParameters params = signer.prepare(new PAdESSignatureParameters(), alias);
        params.setSignatureLevel(signer.level("PAdES"));
        if (reason != null && !reason.isBlank()) {
            params.setReason(reason);
        }
        if (visible.isPresent()) {
            params.setImageParameters(image(pdf, visible.get(), params, reason));
        }
        return signer.sign(signer.service(PAdESService::new),
                new InMemoryDocument(pdf, fileName), params, alias);
    }

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss xxx");

    private static SignatureImageParameters image(byte[] pdf, VisibleSignature v, PAdESSignatureParameters params,
            String reason) throws TokenException {
        int pages;
        PDRectangle box;
        try (var doc = Loader.loadPDF(pdf)) {
            pages = doc.getNumberOfPages();
            int page = v.page() == 0 ? pages : v.page();
            if (page > pages) {
                throw new TokenException("PDF'de " + pages + " sayfa var; istenen sayfa: " + v.page());
            }
            box = doc.getPage(page - 1).getMediaBox();
        } catch (IOException e) {
            throw new TokenException("PDF okunamadı: " + e.getMessage(), e);
        }
        SignatureFieldParameters field = new SignatureFieldParameters();
        field.setPage(v.page() == 0 ? pages : v.page());
        field.setWidth(v.width());
        field.setHeight(v.height());
        field.setOriginX(v.automaticPosition() ? Math.max(0, box.getWidth() - v.width() - VisibleSignature.MARGIN) : v.x());
        field.setOriginY(v.automaticPosition() ? Math.max(0, box.getHeight() - v.height() - VisibleSignature.MARGIN) : v.y());

        SignatureImageTextParameters text = new SignatureImageTextParameters();
        DSSFileFont font = DSSFileFont.initializeDefault(); // PT Serif: Türkçe karakterler gömülü
        font.setSize(8);
        text.setFont(font);
        text.setTextWrapping(TextWrapping.FILL_BOX_AND_LINEBREAK);
        text.setPadding(4);
        text.setTextColor(new Color(0x1F, 0x2A, 0x44));
        text.setBackgroundColor(new Color(0xF2, 0xF5, 0xFA));
        StringBuilder t = new StringBuilder("Elektronik olarak imzalanmıştır / Digitally signed\n")
                .append(commonName(params.getSigningCertificate().getCertificate().getSubjectX500Principal()))
                .append('\n')
                .append(WHEN.format(params.bLevel().getSigningDate().toInstant().atZone(ZoneId.systemDefault())));
        if (reason != null && !reason.isBlank()) {
            t.append('\n').append(reason.strip());
        }
        text.setText(t.toString());

        SignatureImageParameters image = new SignatureImageParameters();
        image.setFieldParameters(field);
        image.setTextParameters(text);
        return image;
    }

    static String commonName(X500Principal p) {
        X500Name n = X500Name.getInstance(p.getEncoded());
        var rdns = n.getRDNs(BCStyle.CN);
        return rdns.length == 0 ? p.getName() : IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }
}
