package io.github.doguomer.esign.cades;

import io.github.doguomer.esign.core.CertificateInfo;
import io.github.doguomer.esign.core.SigningToken;
import io.github.doguomer.esign.core.TokenException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Testler için bellekte çalışan sahte token. */
final class InMemorySigningToken implements SigningToken {
    static final String ALIAS = "imza";
    final KeyPair keyPair;
    final X509Certificate cert;
    int signCalls;

    InMemorySigningToken() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        keyPair = g.generateKeyPair();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        X500Name name = new X500Name("CN=TEST IMZACI,C=TR");
        var b = new JcaX509v3CertificateBuilder(name, BigInteger.TEN, Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(30, ChronoUnit.DAYS)), name, keyPair.getPublic());
        b.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.nonRepudiation));
        cert = new JcaX509CertificateConverter().getCertificate(
                b.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
    }

    @Override
    public List<CertificateInfo> listCertificates() {
        return List.of(CertificateInfo.of(ALIAS, cert, true));
    }

    @Override
    public List<X509Certificate> certificateChain(String alias) throws TokenException {
        if (!ALIAS.equals(alias)) {
            throw new TokenException("Sertifika bulunamadı: " + alias);
        }
        return List.of(cert);
    }

    @Override
    public byte[] sign(String alias, byte[] data, String jcaAlgorithm) throws TokenException {
        try {
            signCalls++;
            Signature s = Signature.getInstance(jcaAlgorithm);
            s.initSign(keyPair.getPrivate());
            s.update(data);
            return s.sign();
        } catch (Exception e) {
            throw new TokenException("imza hatası", e);
        }
    }

    @Override
    public void close() {}
}
