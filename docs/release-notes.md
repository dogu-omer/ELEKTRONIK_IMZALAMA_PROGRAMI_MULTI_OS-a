## esign v0.3.1 — Güvenlik güncellemesi / Security update

**TR**
- Bouncy Castle 1.80 → 1.86 (bcprov, bcutil, bcpkix aynı sürüme sabitlendi). GitHub Dependabot uyarısı
  ("Use of a Broken or Risky Cryptographic Algorithm", bcpkix, düzeltildiği sürüm 1.84) giderildi.
- İşlev değişikliği yok; 71 testin tamamı geçti. v0.3.0'daki doğrulama ve LT/LTA özellikleri aynen geçerlidir.

**EN**
- Bouncy Castle 1.80 → 1.86 (all BC artifacts aligned), fixing the Dependabot advisory on bcpkix. No functional changes; 71 tests pass.

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. Kök sertifikalar pakete eklenmez. ESHS kimlik doğrulamalı zaman damgası desteklenmez. macOS paketi
notarize edilmemiştir. Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.

Kurulum: README → "Kurulum / Installation". Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
