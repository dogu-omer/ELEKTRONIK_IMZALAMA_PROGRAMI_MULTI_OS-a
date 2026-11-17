package io.github.doguomer.esign.cades;

/** CAdES paketleme biçimi. */
public enum CadesPackaging {
    /** Belge imzanın içinde (İmzager .imz biçimi). */
    ATTACHED,
    /** İmza ayrı dosyada; orijinal belge değişmez (.p7s). */
    DETACHED
}
