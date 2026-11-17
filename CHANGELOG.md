# Değişiklik günlüğü / Changelog

## esign v0.5.0 — REST API

- Yeni `esign-api` modülü (Spring Boot 4.1, OpenAPI 3.1 / Swagger UI): `POST /api/v1/validations` doğrulama,
  `POST /api/v1/signatures/prepare` + `/{id}/complete` iki aşamalı imza (özel anahtar ve PIN sunucuya gitmez).
  Sunucu kart imzasını açık anahtarla doğrulamadan belge üretmez; oturumlar tek kullanımlık, süreli ve sınırlı.
  İsteğe bağlı `ESIGN_API_KEY` (X-API-Key). Veritabanı yok.
- `esign sign --server <url>`: kart bu bilgisayarda, imza sunucu üzerinden.
- 98 test (API: 15, istemci: 5). SoftHSM kartı → CLI → API kapsayıcısı uçtan uca denendi.

## esign v0.4.0 — Görünür PDF imzası

- `--visible`, `--visible-at`: PAdES görünür imza kutusu (ad, zaman, neden; Türkçe karakter destekli). 78 test.

## esign v0.3.1 — Güvenlik güncellemesi

- Bouncy Castle 1.80 → 1.86 (Dependabot: bcpkix, düzeltme 1.84). İşlev değişikliği yok.

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

## esign v0.2.0 — Faz 4

**TR**
- CAdES, PAdES ve XAdES imzalarında isteğe bağlı RFC 3161 zaman damgası: `--tsa <url>` veya `ESIGN_TSA_URL` verilirse **T**, verilmezse **BES** profili.
- Gömülü Java çalışma ortamlı paketler: Linux `.deb` (amd64) ve macOS `.pkg` (Apple Silicon). Java kurmanız gerekmez.
- `esign version` komutu; Türkçe/İngilizce kapsamlı README, akademik makale taslağı (`docs/`).
- Lisans GPL-3.0 olarak güncellendi.
- 51 otomatik test (SoftHSM2 ile gerçek PKCS#11 testleri dahil).

**EN**
- Optional RFC 3161 timestamps for CAdES/PAdES/XAdES (`--tsa` → T profile, otherwise BES).
- Self-contained `.deb` and macOS `.pkg` with an embedded runtime; `esign.jar` for any OS with Java 21+.

**Bilinen sınırlar / Known limits:** imza doğrulama, OCSP/CRL, LT/LTA ve İmzager karşılıklı testleri sonraki fazlardadır.
ESHS kimlik doğrulamalı zaman damgası desteklenmez. macOS paketi notarize edilmemiştir.
Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.

Kurulum: README → "Kurulum / Installation". Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
