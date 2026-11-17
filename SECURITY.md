# Güvenlik / Security

- PIN hiçbir zaman komut satırı argümanı, dosya, log veya veritabanına yazılmaz; yalnızca terminalden sorulur ve kullanımdan sonra bellekten silinir.
- Özel anahtar kart dışına çıkmaz; imza kart üzerinde üretilir.
- API anahtarları ve zaman damgası hesap bilgileri yalnızca ortam değişkeni veya işletim sistemi anahtarlığında tutulur (`.env` git'e girmez).
- Güvenlik açığı bildirimi: lütfen herkese açık issue yerine GitHub "Private vulnerability reporting" kullanın.
