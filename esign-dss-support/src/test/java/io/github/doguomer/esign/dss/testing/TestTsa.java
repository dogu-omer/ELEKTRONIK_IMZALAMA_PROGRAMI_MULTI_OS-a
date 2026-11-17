package io.github.doguomer.esign.dss.testing;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;

/** Bellek içi RFC 3161 test TSA'sı (gerçek ESHS yerine). In-memory test TSA. */
public final class TestTsa implements TSPSource {

    public final X509Certificate cert;
    private final KeyPair keys;
    public int calls;

    /** TestPki tarafından verilen sertifika ve anahtarla. */
    public TestTsa(X509Certificate cert, KeyPair keys) {
        this.cert = cert;
        this.keys = keys;
    }

    public TestTsa() throws Exception {
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keys = kpg.generateKeyPair();
        Instant now = Instant.now();
        X500Name dn = new X500Name("C=TR,CN=TEST ZAMAN DAMGASI (SAHTE)");
        var builder = new JcaX509v3CertificateBuilder(dn, BigInteger.ONE,
                Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(30, ChronoUnit.DAYS)), dn, keys.getPublic())
                .addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        cert = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
    }

    @Override
    public TimestampBinary getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) {
        try {
            var gen = new TimeStampTokenGenerator(
                    new JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withRSA", keys.getPrivate(), cert),
                    new JcaDigestCalculatorProviderBuilder().build()
                            .get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)),
                    new ASN1ObjectIdentifier("1.3.6.1.4.1.99999.1"));
            gen.addCertificates(new JcaCertStore(List.of(cert)));
            var reqGen = new TimeStampRequestGenerator();
            reqGen.setCertReq(true); // gerçek ESHS gibi TSA sertifikasını yanıta ekle
            var req = reqGen.generate(new ASN1ObjectIdentifier(digestAlgorithm.getOid()), digest);
            TimeStampResponse resp = new TimeStampResponseGenerator(gen, TSPAlgorithms.ALLOWED)
                    .generate(req, BigInteger.valueOf(++calls), new Date());
            return new TimestampBinary(resp.getTimeStampToken().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
