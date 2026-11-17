## esign v0.8.0 — Windows kurulum paketi ve imzalı sürüm dosyaları / Windows installer and signed release files

**TR**
- **Windows 10/11 (x64) kurulum paketi** `esign-0.8.0-windows-x64.msi`: Java gerektirmez (gömülü JRE),
  Başlat menüsünde ve masaüstünde **esign-gui** kısayolu, `esign` komutu sistem PATH'ine eklenir
  (kaldırınca silinir).
- AKİS `akisp11.dll` Windows'ta kendiliğinden aranır (`System32`, `Program Files\AKIS` …); gerekirse
  `--lib`, `setx ESIGN_PKCS11_LIB …` veya arayüzdeki alanla ayarlanır. Arayüz son kullanılan yolu hatırlar.
- **Tüm sürüm dosyaları** (.deb, .pkg, .msi, jar'lar, SHA256SUMS) Sigstore cosign ile anahtarsız imzalı
  (`*.sigstore.json`) ve GitHub artifact attestation (derleme kökeni) ile yayımlanır. Doğrulama: README bölüm 16.
- İş akışı Authenticode imzasına hazır: PFX veya Azure Artifact Signing sırları eklenince MSI imzalanır.

**EN**
- Windows MSI with embedded JRE, Start-menu/desktop shortcut `esign-gui`, `esign` on PATH.
- Automatic AKİS DLL discovery on Windows; override with `--lib` / `ESIGN_PKCS11_LIB`.
- All release files are Sigstore-signed and carry GitHub build-provenance attestations.

**Bilinen sınırlar / Known limits:** MSI şu an **Authenticode ile imzasızdır** (ücretli kod imzalama sertifikası
yok); Windows SmartScreen "bilinmeyen yayıncı" uyarısı verebilir → *Ek bilgi → Yine de çalıştır* (README §15).
Sigstore imzası SmartScreen/Gatekeeper tarafından tanınmaz. İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS
ortamında uçtan uca test yapılmadı; Windows'ta gerçek AKİS kartıyla deneme yapılmadı. macOS paketi notarize
edilmemiştir. Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.

Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing` ve `cosign verify-blob …` (README §16).
