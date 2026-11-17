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
