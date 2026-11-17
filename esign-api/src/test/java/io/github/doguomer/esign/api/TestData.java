package io.github.doguomer.esign.api;

import io.github.doguomer.esign.dss.testing.TestPki;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/** Testler için belgeler ve "kartta imza" benzetimi. */
final class TestData {

    private TestData() {}

    static byte[] pdf() throws Exception {
        try (PDDocument doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    static byte[] xml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><belge><konu>Deneme ğüşıöç</konu></belge>"
                .getBytes(StandardCharsets.UTF_8);
    }

    static String chainPem(TestPki pki) {
        return pem(pki.signer) + pem(pki.root);
    }

    static String pem(java.security.cert.X509Certificate c) {
        try {
            return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(c.getEncoded())
                    + "\n-----END CERTIFICATE-----\n";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** İstemcideki kartın yaptığı iş: dataToSign'ı özel anahtarla imzala. */
    static byte[] signOnCard(TestPki pki, byte[] dataToSign, String algorithm) throws Exception {
        Signature s = Signature.getInstance(algorithm);
        s.initSign(pki.signerKeys.getPrivate());
        s.update(dataToSign);
        return s.sign();
    }
}
