package io.github.doguomer.esign.dss.testing;

import eu.europa.esig.dss.crl.CRLUtils;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLSource;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLToken;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Testler için küçük bir PKI: kök CA, imzacı, zaman damgası birimi ve istek üzerine üretilen CRL.
 * Tiny test PKI: root CA, signer, TSA and an on-demand CRL source.
 */
public final class TestPki {

    public static final String ALIAS = "imza";
    private static final String CRL_URL = "http://crl.test.invalid/kok.crl";

    public final KeyPair rootKeys;
    public final X509Certificate root;
    public final KeyPair signerKeys;
    public final X509Certificate signer;
    public final TestTsa tsa;
    private final Map<BigInteger, Date> revoked = new ConcurrentHashMap<>();
    private final AtomicInteger serial = new AtomicInteger(100);
    public final AtomicInteger crlRequests = new AtomicInteger();

    public TestPki() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Date from = Date.from(now.minus(10, ChronoUnit.DAYS));
        Date to = Date.from(now.plus(365, ChronoUnit.DAYS));
        var ext = new JcaX509ExtensionUtils();

        rootKeys = rsa();
        X500Name rootDn = new X500Name("C=TR,O=ESIGN TEST,CN=TEST KOK SERTIFIKA (SAHTE)");
        var rb = new JcaX509v3CertificateBuilder(rootDn, BigInteger.ONE, from, to, rootDn, rootKeys.getPublic())
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
                .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
                .addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(rootKeys.getPublic()));
        root = sign(rb, rootKeys);

        signerKeys = rsa();
        signer = issue("C=TR,CN=TEST IMZACI (SAHTE)", signerKeys,
                new KeyUsage(KeyUsage.nonRepudiation | KeyUsage.digitalSignature), null, from, to);

        KeyPair tsaKeys = rsa();
        X509Certificate tsaCert = issue("C=TR,CN=TEST ZAMAN DAMGASI (SAHTE)", tsaKeys,
                new KeyUsage(KeyUsage.digitalSignature), KeyPurposeId.id_kp_timeStamping, from, to);
        tsa = new TestTsa(tsaCert, tsaKeys);
    }

    private X509Certificate issue(String dn, KeyPair keys, KeyUsage ku, KeyPurposeId eku, Date from, Date to)
            throws Exception {
        var ext = new JcaX509ExtensionUtils();
        var b = new JcaX509v3CertificateBuilder(root, BigInteger.valueOf(serial.incrementAndGet()), from, to,
                new X500Name(dn), keys.getPublic())
                .addExtension(Extension.keyUsage, true, ku)
                .addExtension(Extension.authorityKeyIdentifier, false, ext.createAuthorityKeyIdentifier(root))
                .addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[] {
                        new DistributionPoint(new DistributionPointName(new GeneralNames(
                                new GeneralName(GeneralName.uniformResourceIdentifier, CRL_URL))), null, null)}));
        if (eku != null) {
            b.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(eku));
        }
        return sign(b, rootKeys);
    }

    private static X509Certificate sign(org.bouncycastle.cert.X509v3CertificateBuilder b, KeyPair issuer) throws Exception {
        return new JcaX509CertificateConverter().getCertificate(
                b.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuer.getPrivate())));
    }

    private static KeyPair rsa() throws Exception {
        var g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    /** İmzacı sertifikasını verilen tarihte iptal eder. */
    public void revokeSigner(Instant when) {
        revoked.put(signer.getSerialNumber(), Date.from(when));
    }

    /** Kök CA'nın güncel CRL'si (her istekte yeniden, thisUpdate = şimdi). */
    public byte[] currentCrl() {
        try {
            Instant now = Instant.now();
            var b = new X509v2CRLBuilder(X500Name.getInstance(root.getSubjectX500Principal().getEncoded()),
                    Date.from(now.minusSeconds(1)));
            b.setNextUpdate(Date.from(now.plus(1, ChronoUnit.DAYS)));
            b.addExtension(Extension.authorityKeyIdentifier, false,
                    new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(root));
            revoked.forEach((sn, d) -> b.addCRLEntry(sn, d, CRLReason.keyCompromise));
            var holder = b.build(new JcaContentSignerBuilder("SHA256withRSA").build(rootKeys.getPrivate()));
            return new JcaX509CRLConverter().getCRL(holder).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** DSS için çevrimiçi CRL kaynağı yerine geçen bellek içi kaynak. */
    public CRLSource crlSource() {
        return (CRLSource) (cert, issuer) -> {
            crlRequests.incrementAndGet();
            if (issuer == null || !issuer.getCertificate().equals(root)) {
                return null;
            }
            try {
                var token = new CRLToken(cert, CRLUtils.buildCRLValidity(CRLUtils.buildCRLBinary(currentCrl()), issuer));
                token.setSourceURL(CRL_URL);
                token.setExternalOrigin(eu.europa.esig.dss.enumerations.RevocationOrigin.EXTERNAL);
                return token;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    /** İmzacı anahtarıyla bellekte imza atan token; zincir: imzacı → kök. */
    public SigningToken token() {
        return new SigningToken() {
            @Override
            public List<CertificateInfo> listCertificates() {
                return List.of(CertificateInfo.of(ALIAS, signer, true));
            }

            @Override
            public List<X509Certificate> certificateChain(String alias) throws TokenException {
                if (!ALIAS.equals(alias)) {
                    throw new TokenException("Sertifika bulunamadı: " + alias);
                }
                return List.of(signer, root);
            }

            @Override
            public byte[] sign(String alias, byte[] data, String jcaAlgorithm) throws TokenException {
                try {
                    Signature s = Signature.getInstance(jcaAlgorithm);
                    s.initSign(signerKeys.getPrivate());
                    s.update(data);
                    return s.sign();
                } catch (Exception e) {
                    throw new TokenException("imza hatası", e);
                }
            }

            @Override
            public void close() {}
        };
    }

    public static CertificateToken token(X509Certificate c) {
        return new CertificateToken(c);
    }
}
