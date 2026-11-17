package io.github.doguomer.esign.pades;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.core.TokenException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.junit.jupiter.api.BeforeEach;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.testing.TestTsa;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.junit.jupiter.api.Test;

class PadesBesSignerTest {

    private InMemorySigningToken token;
    private PadesBesSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        token = new InMemorySigningToken();
        signer = new PadesBesSigner(token, Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ZoneOffset.UTC));
    }

    private static byte[] blankPdf() throws Exception {
        try (PDDocument doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** /Contents sıfırla doldurulur; BC 1.81+ fazla baytı reddeder, yalnızca DER nesnesini alır. */
    private static byte[] cmsBytes(byte[] signedPdf, PDSignature sig) throws Exception {
        try (var in = new org.bouncycastle.asn1.ASN1InputStream(sig.getContents(signedPdf))) {
            return in.readObject().getEncoded();
        }
    }

    private boolean verify(byte[] signedPdf, PDSignature sig) throws Exception {
        byte[] signedContent = sig.getSignedContent(signedPdf);
        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signedContent), cmsBytes(signedPdf, sig));
        SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
        return si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(token.cert));
    }

    @Test
    void signedPdfContainsValidPadesSignature() throws Exception {
        byte[] signed = signer.sign(blankPdf(), "belge.pdf", InMemorySigningToken.ALIAS, "Onay");

        try (PDDocument doc = Loader.loadPDF(signed)) {
            var sigs = doc.getSignatureDictionaries();
            assertEquals(1, sigs.size());
            PDSignature sig = sigs.get(0);
            assertEquals("ETSI.CAdES.detached", sig.getSubFilter(), "PAdES baseline SubFilter");
            assertEquals("Onay", sig.getReason());
            assertTrue(verify(signed, sig));
        }
    }

    @Test
    void secondSignerKeepsFirstSignatureValid() throws Exception {
        byte[] once = signer.sign(blankPdf(), "belge.pdf", InMemorySigningToken.ALIAS, null);
        byte[] twice = signer.sign(once, "belge.pdf", InMemorySigningToken.ALIAS, null);

        try (PDDocument doc = Loader.loadPDF(twice)) {
            var sigs = doc.getSignatureDictionaries();
            assertEquals(2, sigs.size());
            for (PDSignature s : sigs) {
                assertTrue(verify(twice, s));
            }
        }
        assertArrayEquals(once, java.util.Arrays.copyOf(twice, once.length), "Artımlı güncelleme: ilk sürüm aynen korunmalı");
    }

    @Test
    void nonPdfIsRejected() {
        assertThrows(TokenException.class, () -> signer.sign("merhaba".getBytes(StandardCharsets.UTF_8),
                "a.pdf", InMemorySigningToken.ALIAS, null));
    }

    @Test
    void withTsaProducesPadesTWithSignatureTimestamp() throws Exception {
        TestTsa tsa = new TestTsa();
        var tSigner = new PadesBesSigner(token, java.time.Clock.systemUTC(), Timestamping.of(tsa, "test"));
        byte[] signed = tSigner.sign(blankPdf(), "belge.pdf", InMemorySigningToken.ALIAS, null);

        try (PDDocument doc = Loader.loadPDF(signed)) {
            PDSignature sig = doc.getSignatureDictionaries().get(0);
            assertTrue(verify(signed, sig));
            CMSSignedData cms = new CMSSignedData(cmsBytes(signed, sig));
            SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
            assertNotNull(si.getUnsignedAttributes().get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken),
                    "PAdES-T: imza zaman damgası gerekli");
        }
        assertTrue(tsa.calls >= 1);
    }

    private static byte[] pdfWithPages(int n) throws Exception {
        try (PDDocument doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            for (int i = 0; i < n; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void visibleSignatureDrawsBoxOnLastPageAndStaysValid() throws Exception {
        byte[] signed = signer.sign(pdfWithPages(3), "belge.pdf", InMemorySigningToken.ALIAS, "Onaylıyorum",
                java.util.Optional.of(VisibleSignature.defaults()));
        try (PDDocument doc = Loader.loadPDF(signed)) {
            PDSignature sig = doc.getSignatureDictionaries().get(0);
            assertTrue(verify(signed, sig));
            var field = doc.getSignatureFields().get(0);
            var widget = field.getWidgets().get(0);
            assertEquals(doc.getPage(2), widget.getPage(), "son sayfada olmalı");
            var r = widget.getRectangle();
            assertEquals(VisibleSignature.WIDTH, r.getWidth(), 1f);
            assertEquals(VisibleSignature.HEIGHT, r.getHeight(), 1f);
            assertTrue(r.getLowerLeftX() > doc.getPage(2).getMediaBox().getWidth() / 2, "sağ tarafta olmalı");
            assertTrue(r.getLowerLeftY() < 100, "altta olmalı");
            assertNotNull(widget.getNormalAppearanceStream(), "görünüm akışı gerekli");
        }
    }

    @Test
    void visibleSignatureOnChosenPageAndPosition() throws Exception {
        byte[] signed = signer.sign(pdfWithPages(2), "belge.pdf", InMemorySigningToken.ALIAS, null,
                java.util.Optional.of(VisibleSignature.parse("1,50,60")));
        try (PDDocument doc = Loader.loadPDF(signed)) {
            var widget = doc.getSignatureFields().get(0).getWidgets().get(0);
            assertEquals(doc.getPage(0), widget.getPage());
            assertEquals(50f, widget.getRectangle().getLowerLeftX(), 1f);
        }
    }

    @Test
    void visiblePageOutOfRangeIsRejected() {
        var e = assertThrows(TokenException.class, () -> signer.sign(pdfWithPages(1), "belge.pdf",
                InMemorySigningToken.ALIAS, null, java.util.Optional.of(VisibleSignature.parse("5"))));
        assertTrue(e.getMessage().contains("1 sayfa"));
    }
}
