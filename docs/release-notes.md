## esign v0.3.0 — Faz 5

**TR**
- Yeni `esign verify` komutu: EU DSS ile ETSI EN 319 102-1 doğrulaması (belge özeti, imza, zincir, güvenilir kök,
  OCSP/CRL, zaman damgaları). Sonuç GEÇERLİ / BELİRSİZ / GEÇERSİZ; çıkış kodları 0 / 3 / 4.
  `--original` (ayrık imza), `--trust` veya `ESIGN_TRUST_DIR` (kök sertifikalar), `--offline`, `--report` (XML rapor).
- Uzun dönem imza profilleri: `esign sign --level bes|t|lt|lta`. LT, zincir ve OCSP/CRL yanıtlarını imzaya
  gömer; LTA ayrıca arşiv zaman damgası ekler. LT/LTA için `--tsa` ve `--trust` gerekir.
- Yeni modül `esign-validation`; `esign-dss-support` içinde `SignatureProfile`, `SignatureSettings`, `TrustSettings`.
- 71 otomatik test (test PKI: kök CA, CRL, iptal edilmiş sertifika, LT/LTA, PAdES/XAdES).

**EN**
- New `esign verify` command (ETSI EN 319 102-1 via EU DSS) with exit codes 0 valid / 3 indeterminate / 4 invalid.
- Long-term levels: `esign sign --level lt|lta` embeds chain and OCSP/CRL data (LTA adds an archive timestamp).

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. Kök sertifikalar pakete eklenmez; kullanıcı resmî kaynaktan indirir. ESHS kimlik doğrulamalı zaman
damgası desteklenmez. macOS paketi notarize edilmemiştir. Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.

Kurulum: README → "Kurulum / Installation". Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
