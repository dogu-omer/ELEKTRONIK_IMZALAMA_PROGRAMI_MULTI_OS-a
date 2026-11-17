## esign v0.6.0 — Docker

**TR**
- REST API için **Docker görüntüsü** ve **docker-compose**: `cp .env.example .env && docker compose up -d`.
  Kök olmayan kullanıcı, salt okunur dosya sistemi, tüm yetkiler düşürülmüş, yalnızca 127.0.0.1'de dinler.
- Hazır görüntü: `docker pull ghcr.io/dogu-omer/esign-api:0.6.0`.
- Veritabanı gerekmez (API durumsuzdur). v0.5.0'daki REST API ve `esign sign --server` aynen geçerlidir.

**EN**
- Container image and docker-compose for the REST API (non-root, read-only FS, no capabilities, localhost
  only). Prebuilt image on GHCR. No database needed.

**Bilinen sınırlar / Known limits:** İmzager karşılıklı testleri ve gerçek Kamu SM/ESHS ortamında uçtan uca test
yapılmadı. İnternete açmadan önce TLS'li ters vekil ve `ESIGN_API_KEY` gerekir. ESHS kimlik doğrulamalı zaman
damgası desteklenmez. macOS paketi notarize edilmemiştir. Yazılım herhangi bir kurum tarafından
sertifikalandırılmamıştır.

Bütünlük: `sha256sum -c SHA256SUMS.txt --ignore-missing`.
