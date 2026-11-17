## esign v0.4.0 — Görünür PDF imzası / Visible PDF signature

**TR**
- `esign sign --in x.pdf --visible`: PDF'in son sayfasının sağ altına görünür imza kutusu çizilir
  (imzacı adı, imza zamanı, varsa `--reason`). Türkçe karakterler gömülü yazı tipiyle (PT Serif) korunur.
- `--visible-at <sayfa>` veya `--visible-at <sayfa>,<x>,<y>` ile yer seçilir (puan; sayfanın sol üstünden).
- Görünür imza, PAdES-BES/T/LT/LTA profillerinin tümüyle birlikte kullanılabilir; imzanın kriptografik
  geçerliliğini değiştirmez.
- v0.3.1: Bouncy Castle 1.86 güvenlik güncellemesi (Dependabot uyarısı giderildi).
- 78 otomatik test.

**EN**
- `--visible` / `--visible-at` draw a visible PAdES signature box (signer, time, reason) with an embedded font
  that keeps Turkish characters. Bouncy Castle updated to 1.86 in v0.3.1. 78 tests pass.

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. Görünür kutuya logo/görsel eklenemez. Kök sertifikalar pakete eklenmez. ESHS kimlik doğrulamalı zaman
damgası desteklenmez. macOS paketi notarize edilmemiştir. Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.

Kurulum: README → "Kurulum / Installation". Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
