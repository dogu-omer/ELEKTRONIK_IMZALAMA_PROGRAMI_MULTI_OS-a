# Güvenlik / Security

## Kart, PIN ve özel anahtar

- PIN hiçbir zaman komut satırı argümanı, dosya, log veya veritabanına yazılmaz; yalnızca terminalden (veya
  masaüstü arayüzünde parola kutusundan) sorulur ve kullanımdan sonra bellekten silinir.
- Özel anahtar kart dışına çıkmaz; imza kart üzerinde üretilir. REST API sunucusu özel anahtar ve PIN görmez
  (iki aşamalı imza: hazırla → istemcide kartta imzala → birleştir).

## Sırlar (API anahtarı, TSA hesabı)

- Sırlar yalnızca ortam değişkeni veya işletim sistemi anahtarlığında tutulur; `.env`, `*.pem`, `*.key`,
  `*.p12`, `*.pfx` git'e girmez (`.gitignore`). Depoda ve git geçmişinde sır bulunmaz (2026-11-17 taraması).
- `ESIGN_API_KEY` **zorunludur**; tanımlı değilse API başlamaz (fail-closed). En az 24 karakter:
  `openssl rand -base64 32`.
- Sunucuya anahtarın kendisi yerine yalnızca SHA-256 özeti verilebilir (`ESIGN_API_KEY_SHA256`); virgülle
  birden çok özet anahtar rotasyonu sağlar: yeni özeti ekleyin → istemcileri güncelleyin → eskiyi silin.
- Karşılaştırma özet üzerinden sabit zamanlıdır; anahtar loglanmaz (`EsignProperties.toString` maskeler).

## Ağ ve TLS

- `docker compose` yalnızca `127.0.0.1`'e bağlanır. Dışa açarken önüne TLS 1.2+ sonlandıran bir ters vekil
  (Caddy, nginx) koyun; HSTS'yi vekilde etkinleştirin. Alternatif: Spring Boot'un yerleşik TLS'i
  (`SERVER_SSL_ENABLED=true`, `SERVER_SSL_BUNDLE`/`SERVER_SSL_KEY_STORE`).
- CLI, API anahtarını şifresiz `http` ile yalnızca `localhost`/`127.0.0.1`/`::1`'e gönderir; uzak sunucuda `https`
  zorunludur. Yönlendirmeler izlenmez.
- IP başına istek sınırı (`ESIGN_RATE_LIMIT`, varsayılan 120/dk) anahtar tahminini ve kaynak tüketimini
  sınırlar. Ters vekil arkasında tüm istekler vekilden geldiği için sınırı vekilde de uygulayın.
- CORS açılmamıştır (tarayıcıdan farklı kökenli çağrı yapılamaz). Yanıtlarda `nosniff`, `X-Frame-Options: DENY`,
  `Referrer-Policy: no-referrer`; `/api/**` için `Cache-Control: no-store`.
- Yükleme sınırı 25 MB (istek 30 MB). Üretimde `ESIGN_API_DOCS=false` ile Swagger UI kapatılabilir.

## Hata ve log (KVKK 6698 / GDPR)

- Hata yanıtları RFC 9457 problem+json'dur; yığın izi, istisna sınıfı veya iç kütüphane mesajı dönmez.
  Beklenmeyen hatalarda istemciye yalnızca bir izleme kimliği (`ref`) verilir.
- Loglara belge içeriği, sertifika sahibi (ad, T.C. kimlik no), PIN, API anahtarı yazılmaz; yalnızca hata sınıfı
  ve izleme kimliği. API veritabanı kullanmaz; belgeler diske yazılmaz, bekleyen imzalar yalnızca bellekte ve
  `ESIGN_SESSION_TTL` (10 dk) kadar tutulur. IP adresi yalnızca istek sınırı için bellekte tutulur.

## Kapsayıcı

- Kök olmayan `esign` kullanıcısı, salt okunur kök dosya sistemi, `cap_drop: ALL`, `no-new-privileges`.

## Bildirim / Reporting

Güvenlik açığı bildirimi için lütfen herkese açık issue yerine GitHub "Private vulnerability reporting"
kullanın. *Please report vulnerabilities privately via GitHub's private vulnerability reporting.*
