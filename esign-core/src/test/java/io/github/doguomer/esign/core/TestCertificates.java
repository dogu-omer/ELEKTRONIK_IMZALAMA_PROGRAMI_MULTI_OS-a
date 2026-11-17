package io.github.doguomer.esign.core;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Testler için kendinden imzalı sertifika üretir. */
final class TestCertificates {
    private TestCertificates() {}

    static X509Certificate selfSigned(String cn, int keyUsageBits, Instant from, Instant to) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        X500Name name = new X500Name("CN=" + cn + ",C=TR");
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(0xABCDEFL),
                Date.from(from), Date.from(to), name, kp.getPublic());
        if (keyUsageBits != 0) {
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsageBits));
        }
        var signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
