package io.github.doguomer.esign.api;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/** PEM veya DER sertifika zinciri okur; ilk sertifika imzacıdır. */
final class Certificates {

    private Certificates() {}

    static List<X509Certificate> parse(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Sertifika zinciri boş");
        }
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<X509Certificate> out = new ArrayList<>();
            for (var c : cf.generateCertificates(new ByteArrayInputStream(data))) {
                out.add((X509Certificate) c);
            }
            if (out.isEmpty()) {
                throw new IllegalArgumentException("Sertifika bulunamadı");
            }
            return List.copyOf(out);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Sertifika okunamadı (PEM veya DER olmalı)", e);
        }
    }
}
