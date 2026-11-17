## esign v0.5.0 — REST API

**TR**
- Yeni **esign-api** (Spring Boot): imza doğrulama ve iki aşamalı imza. Sunucu imzalanacak veriyi hazırlar,
  kullanıcı kendi kartıyla imzalar, sunucu imzayı doğrulayıp imzalı belgeyi üretir. **Özel anahtar ve PIN
  sunucuya gitmez.** OpenAPI belgesi `/swagger-ui.html` adresindedir. Veritabanı gerekmez.
- `esign sign --server http://sunucu:8080`: kart bu bilgisayarda, imza sunucu üzerinden.
- `esign-api.jar` bu sürüme eklendi: `java -jar esign-api.jar`. Ayarlar ortam değişkenleriyle
  (`ESIGN_API_KEY`, `ESIGN_TSA_URL`, `ESIGN_TRUST_DIR`).
- 98 otomatik test.

**EN**
- New Spring Boot **REST API**: validation plus two-step signing (prepare → sign on the client's card →
  complete). The private key and PIN never reach the server. OpenAPI at `/swagger-ui.html`. No database.
- `esign sign --server <url>` uses the API with the local card. `esign-api.jar` attached.

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. API'de görünür PDF imza kutusu yok (CLI'da var). Kök sertifikalar pakete eklenmez. ESHS kimlik
doğrulamalı zaman damgası desteklenmez. macOS paketi notarize edilmemiştir. Yazılım herhangi bir kurum
tarafından sertifikalandırılmamıştır.

Kurulum: README → "Kurulum / Installation", "REST API". Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
