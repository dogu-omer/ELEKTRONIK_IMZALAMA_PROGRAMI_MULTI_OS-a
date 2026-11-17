package io.github.doguomer.esign.xades;

import static org.junit.jupiter.api.Assertions.*;

import io.github.doguomer.esign.core.TokenException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import io.github.doguomer.esign.dss.Timestamping;
import io.github.doguomer.esign.dss.testing.TestTsa;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class XadesBesSignerTest {

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><yazi><konu>Deneme</konu><metin>Türkçe içerik</metin></yazi>";
    private InMemorySigningToken token;
    private XadesBesSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        token = new InMemorySigningToken();
        signer = new XadesBesSigner(token, Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ZoneOffset.UTC));
    }

    private static Document parse(byte[] xml) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private boolean validate(Document doc) throws Exception {
        var props = doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SignedProperties");
        for (int i = 0; i < props.getLength(); i++) {
            ((Element) props.item(i)).setIdAttributeNS(null, "Id", true);
        }
        var sigEl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        var ctx = new DOMValidateContext(token.keyPair.getPublic(), sigEl);
        return XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx).validate(ctx);
    }

    @Test
    void envelopedSignatureValidatesAndHasXadesProperties() throws Exception {
        Document doc = parse(signer.sign(XML.getBytes(StandardCharsets.UTF_8), "yazi.xml", InMemorySigningToken.ALIAS));

        assertEquals("yazi", doc.getDocumentElement().getLocalName(), "Kök eleman korunmalı (enveloped)");
        assertEquals(1, doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SigningCertificateV2").getLength());
        assertEquals(1, doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SigningTime").getLength());
        assertTrue(validate(doc));
    }

    @Test
    void tamperedXmlFailsValidation() throws Exception {
        String signed = new String(signer.sign(XML.getBytes(StandardCharsets.UTF_8), "yazi.xml", InMemorySigningToken.ALIAS),
                StandardCharsets.UTF_8);
        Document doc = parse(signed.replace("Deneme", "Değişti").getBytes(StandardCharsets.UTF_8));
        assertFalse(validate(doc));
    }

    @Test
    void invalidXmlIsRejected() {
        assertThrows(TokenException.class,
                () -> signer.sign("bu xml değil".getBytes(StandardCharsets.UTF_8), "x.xml", InMemorySigningToken.ALIAS));
    }

    @Test
    void withTsaProducesXadesTWithSignatureTimestamp() throws Exception {
        TestTsa tsa = new TestTsa();
        var tSigner = new XadesBesSigner(token, Clock.systemUTC(), Timestamping.of(tsa, "test"));
        Document doc = parse(tSigner.sign(XML.getBytes(StandardCharsets.UTF_8), "yazi.xml", InMemorySigningToken.ALIAS));

        assertEquals(1, doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SignatureTimeStamp").getLength());
        assertTrue(validate(doc));
        assertEquals(1, tsa.calls);
    }
}
