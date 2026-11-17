package io.github.doguomer.esign.dss;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLSource;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Güvenilir kök sertifikalar ve iptal (OCSP/CRL) kaynakları. Doğrulamada ve LT/LTA imzada kullanılır.
 * Trust anchors and revocation sources used for validation and LT/LTA signing.
 */
public final class TrustSettings {

    private final List<X509Certificate> anchors;
    private final OCSPSource ocsp;
    private final CRLSource crl;
    private final AIASource aia;

    private TrustSettings(List<X509Certificate> anchors, OCSPSource ocsp, CRLSource crl, AIASource aia) {
        this.anchors = List.copyOf(anchors);
        this.ocsp = ocsp;
        this.crl = crl;
        this.aia = aia;
    }

    /** Güvenilir kök yok, ağ erişimi yok. */
    public static TrustSettings none() {
        return new TrustSettings(List.of(), null, null, null);
    }

    /**
     * @param online true ise sertifikalardaki OCSP/CRL/AIA adreslerine bağlanılır
     */
    public static TrustSettings of(List<X509Certificate> anchors, boolean online) {
        if (!online) {
            return new TrustSettings(anchors, null, null, null);
        }
        return new TrustSettings(anchors, new OnlineOCSPSource(new OCSPDataLoader()),
                new OnlineCRLSource(new CommonsDataLoader()), new DefaultAIASource(new CommonsDataLoader()));
    }

    /** Özel kaynaklarla (kurum içi OCSP, testler). Null verilen kaynak kullanılmaz. */
    public static TrustSettings of(List<X509Certificate> anchors, OCSPSource ocsp, CRLSource crl, AIASource aia) {
        return new TrustSettings(anchors, ocsp, crl, aia);
    }

    public List<X509Certificate> anchors() {
        return anchors;
    }

    public boolean hasRevocationSource() {
        return ocsp != null || crl != null;
    }

    /** DSS için yeni bir doğrulayıcı yapılandırması üretir. */
    public CertificateVerifier certificateVerifier() {
        CommonCertificateVerifier v = new CommonCertificateVerifier();
        if (!anchors.isEmpty()) {
            CommonTrustedCertificateSource trusted = new CommonTrustedCertificateSource();
            anchors.forEach(c -> trusted.addCertificate(new CertificateToken(c)));
            v.setTrustedCertSources(trusted);
        }
        v.setOcspSource(ocsp);
        v.setCrlSource(crl);
        v.setAIASource(aia);
        return v;
    }

    /**
     * Dosya veya klasördeki sertifikaları (PEM veya DER; .crt .cer .pem .der) okur.
     * Loads PEM/DER certificates from a file or a directory.
     */
    public static List<X509Certificate> load(Path path) throws IOException {
        List<Path> files;
        if (Files.isDirectory(path)) {
            try (Stream<Path> s = Files.list(path)) {
                files = s.filter(Files::isRegularFile).filter(TrustSettings::isCertFile).sorted().toList();
            }
        } else if (Files.isRegularFile(path)) {
            files = List.of(path);
        } else {
            throw new IOException("Güvenilir sertifika yolu bulunamadı: " + path);
        }
        List<X509Certificate> out = new ArrayList<>();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (Path f : files) {
                try (InputStream in = Files.newInputStream(f)) {
                    for (var c : cf.generateCertificates(in)) {
                        out.add((X509Certificate) c);
                    }
                } catch (CertificateException e) {
                    throw new IOException("Sertifika okunamadı: " + f + " (" + e.getMessage() + ")", e);
                }
            }
        } catch (CertificateException e) {
            throw new IOException(e);
        }
        if (out.isEmpty()) {
            throw new IOException("Güvenilir sertifika bulunamadı: " + path);
        }
        return out;
    }

    private static boolean isCertFile(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".crt") || n.endsWith(".cer") || n.endsWith(".pem") || n.endsWith(".der");
    }
}
