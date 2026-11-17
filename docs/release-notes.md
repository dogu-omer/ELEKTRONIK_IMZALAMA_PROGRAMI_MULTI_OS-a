## esign v0.7.0 — Masaüstü arayüzü / Desktop app

**TR**
- Türkçe **masaüstü arayüzü** (Linux ve macOS): dosya seçin, karttaki sertifikayı seçin, PIN girin, imzalayın;
  imzalı dosyaları doğrulayın. Linux'ta uygulama menüsünde **esign-gui**, macOS'ta Finder'dan açılır;
  her yerde `esign gui`.
- PIN yalnızca parola kutusunda girilir ve kaydedilmez; özel anahtar kartı terk etmez; mevcut dosyaların
  üzerine yazılmaz.
- REST API (v0.5.0), Docker (v0.6.0) ve komut satırı aynen geçerlidir.

**EN**
- Turkish desktop GUI for signing with a smart card and validating signatures (Linux menu entry `esign-gui`,
  macOS Finder, or `esign gui`). PIN is never stored; the private key never leaves the card.

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. ESHS kimlik doğrulamalı zaman damgası desteklenmez. macOS paketi notarize edilmemiştir. Yazılım
herhangi bir kurum tarafından sertifikalandırılmamıştır.

Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
