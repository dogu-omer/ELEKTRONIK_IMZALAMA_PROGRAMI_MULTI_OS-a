# esign — Linux, macOS ve Windows için Elektronik İmza / E-Signature for Linux, macOS and Windows

[![ci](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/actions/workflows/ci.yml/badge.svg)](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/actions/workflows/ci.yml)
[![Lisans: GPL-3.0](https://img.shields.io/badge/lisans-GPL--3.0-blue.svg)](LICENSE)

**TR —** AKİS akıllı kart veya USB token (PKCS#11) ile **CAdES**, **PAdES** ve **XAdES** formatında
elektronik imza üreten, açık kaynak, Java tabanlı komut satırı aracı. İsteğe bağlı **RFC 3161 zaman
damgası** ile BES veya T profilinde imza atar. İmza motoru olarak Avrupa Komisyonu'nun
[EU DSS](https://github.com/esig/dss) kütüphanesini kullanır.

**EN —** Open-source Java command-line tool that creates **CAdES**, **PAdES** and **XAdES** signatures
with a PKCS#11 smart card or USB token (e.g. Turkish AKİS cards), with optional **RFC 3161
timestamping** (BES, T, LT, LTA profiles) and **validates** signatures (ETSI EN 319 102-1, OCSP/CRL). Built on the European Commission's [EU DSS](https://github.com/esig/dss).

> ⚠️ **Durum / Status: v0.8.0 — Faz 6 / 7 sürüyor.** İmza üretimi, doğrulama, OCSP/CRL, LT/LTA, görünür PDF imzası, REST API, Docker, masaüstü arayüzü ve Windows kurulum paketi hazırdır;
> **İmzager karşılıklı testleri ve gerçek ESHS/Kamu SM ortamında uçtan uca test henüz yapılmadı.** Bu yazılım
> BTK veya TÜBİTAK tarafından onaylanmış değildir. / Signing, validation, revocation checks and LT/LTA
> work; İmzager interoperability and end-to-end tests against real Turkish CAs/TSAs are not done yet.
> Not certified by any authority.

---

## İçindekiler / Contents

1. [Özellikler / Features](#1-özellikler--features)
2. [Kurulum / Installation](#2-kurulum--installation)
3. [Kart okuyucu ve AKİS / Card reader and AKİS](#3-kart-okuyucu-ve-akis--card-reader-and-akis)
4. [Kullanım / Usage](#4-kullanım--usage)
5. [Zaman damgası / Timestamping](#5-zaman-damgası--timestamping)
6. [Hukuki çerçeve ve sınırlar / Legal framework and limits](#6-hukuki-çerçeve-ve-sınırlar--legal-framework-and-limits)
7. [Güvenlik / Security](#7-güvenlik--security)
8. [Kaynaktan derleme ve test / Build and test](#8-kaynaktan-derleme-ve-test--build-and-test)
9. [Mimari / Architecture](#9-mimari--architecture)
10. [Yol haritası / Roadmap](#10-yol-haritası--roadmap)
11. [Lisans / License](#11-lisans--license)
12. [REST API](#12-rest-api)
13. [Docker](#13-docker)
14. [Masaüstü arayüzü / Desktop app](#14-masaüstü-arayüzü--desktop-app)
15. [Windows](#15-windows)
16. [Sürüm imzaları ve doğrulama / Release signatures](#16-sürüm-imzaları-ve-doğrulama--release-signatures)

---

## 1. Özellikler / Features

| Özellik / Feature | Durum / Status | Standart / Standard |
| --- | --- | --- |
| Karttaki sertifikaları listeleme / List card certificates | ✅ | PKCS#11 v2.x |
| CAdES-BES bütünleşik `.imz` (İmzager biçimi) ve ayrık `.p7s` | ✅ | ETSI EN 319 122, RFC 5652 |
| PAdES-BES (PDF içine, çoklu imza) / multiple signers | ✅ | ETSI EN 319 142 |
| Görünür PDF imzası (`--visible`) / visible PDF signature | ✅ | ISO 32000 |
| XAdES-BES enveloped (XML içine) | ✅ | ETSI EN 319 132 |
| Zaman damgalı T profili / T profile with timestamp | ✅ isteğe bağlı / optional | RFC 3161 |
| Gömülü JRE ile `.deb` ve macOS `.pkg` | ✅ | — |
| İmza doğrulama (`esign verify`), zincir, OCSP/CRL / Validation, revocation | ✅ | ETSI EN 319 102-1, RFC 6960, RFC 5280 |
| LT / LTA (uzun dönem arşiv) / long-term archival | ✅ `--level lt\|lta` | ETSI EN 319 122/132/142 |
| İmzager karşılıklı uyumluluk / interoperability | ⏳ Faz 6 | — |
| REST API: doğrulama + iki aşamalı imza (hazırla → kartta imzala → birleştir), OpenAPI | ✅ v0.5.0 | OpenAPI 3.1 |
| Sunucu üzerinden imza (`esign sign --server`) / sign via API | ✅ v0.5.0 | — |
| Docker görüntüsü ve docker-compose / container image | ✅ v0.6.0 | OCI |
| Türkçe masaüstü arayüzü (Linux, macOS) / desktop app | ✅ v0.7.0 | — |

## 2. Kurulum / Installation

Hazır paketler [Releases](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/releases)
sayfasındadır. Paketler kendi Java çalışma ortamını içerir; ayrıca Java kurmanız gerekmez.
*Packages ship their own Java runtime; no separate Java install is needed.*

| Sistem / OS | Dosya / File | Kurulum / Install |
| --- | --- | --- |
| Debian, Ubuntu, Kali, Pardus (amd64) | `esign_<sürüm>_amd64.deb` | `sudo apt install ./esign_*_amd64.deb` |
| macOS (Apple Silicon) | `esign-<sürüm>-macos-arm64.pkg` | Çift tıklayın / double-click |
| Windows 10/11 (x64) | `esign-<sürüm>-windows-x64.msi` | Çift tıklayın; ayrıntı bölüm 15 / double-click, see §15 |
| Diğer / Other (Java 21+) | `esign.jar` | `java -jar esign.jar help` |

Kurulumdan sonra / After installing:

```bash
# Linux: program /opt/esign altına kurulur. PATH'e ekleyin:
sudo ln -sf /opt/esign/bin/esign /usr/local/bin/esign
# macOS: /Applications/esign.app
sudo ln -sf /Applications/esign.app/Contents/MacOS/esign /usr/local/bin/esign

esign version
```

İndirilen dosyanın bütünlüğünü kontrol edin / verify the download:

```bash
sha256sum -c SHA256SUMS.txt --ignore-missing      # macOS: shasum -a 256 -c SHA256SUMS.txt
```

Tüm sürüm dosyaları ayrıca Sigstore ile imzalıdır; doğrulama için bölüm 16.
*All release files are also Sigstore-signed; see §16.*

> macOS paketi henüz Apple tarafından imzalı/notarize değildir. Gatekeeper uyarısında
> *Sistem Ayarları → Gizlilik ve Güvenlik → Yine de Aç* seçin.
> *The macOS package is not notarized yet; use System Settings → Privacy & Security → Open Anyway.*

## 3. Kart okuyucu ve AKİS / Card reader and AKİS

### Linux

```bash
sudo apt install pcscd libccid pcsc-tools
sudo systemctl enable --now pcscd     # Kali'de varsayılan olarak kapalı olabilir / may be off on Kali
pcsc_scan                             # Kartı takın; okuyucu ve ATR görünmeli / insert card, reader + ATR must appear
```

AKİS PKCS#11 sürücüsünü (`libakisp11.so`) TÜBİTAK BİLGEM'den veya kartınızı aldığınız ESHS'den
(ör. Kamu SM, E-Güven, E-Tuğra, TürkTrust) indirip kurun. Program şu yolları kendiliğinden dener:
*Install the AKİS PKCS#11 driver from TÜBİTAK BİLGEM or your certificate provider. These paths are tried automatically:*

| Sistem | Yol / Path |
| --- | --- |
| Linux | `/usr/lib/libakisp11.so`, `/usr/local/lib/libakisp11.so`, `/usr/lib/x86_64-linux-gnu/libakisp11.so`, `/usr/lib64/libakisp11.so` |
| macOS | `/usr/local/lib/libakisp11.dylib`, `/Library/AKIS/libakisp11.dylib` |
| Windows | `%SystemRoot%\System32\akisp11.dll`, `%ProgramFiles%\AKIS\akisp11.dll`, `%ProgramFiles%\TUBITAK\AKIS\akisp11.dll`, `%ProgramFiles%\AKIS\x64\akisp11.dll` |

Farklı bir yerdeyse `--lib /yol/libakisp11.so` verin veya `ESIGN_PKCS11_LIB` ortam değişkenini
ayarlayın. Başka bir PKCS#11 sürücüsü (ör. OpenSC `opensc-pkcs11.so`) de aynı şekilde kullanılabilir.
*Elsewhere: pass `--lib` or set `ESIGN_PKCS11_LIB`. Any PKCS#11 module (e.g. OpenSC) works the same way.*

### macOS

macOS kart okuyucuyu kendi PC/SC katmanıyla tanır; ek servis gerekmez. Yalnızca AKİS sürücüsünü kurun.
*macOS has built-in PC/SC; just install the AKİS driver.*

### Sorun giderme / Troubleshooting

| Belirti / Symptom | Neden ve çözüm / Cause and fix |
| --- | --- |
| `PKCS#11 kütüphanesi bulunamadı` | Sürücü yolu yanlış → `--lib` verin / wrong driver path |
| Okuyucu görünmüyor / reader not found | `pcscd` kapalı → `sudo systemctl start pcscd` |
| `PIN istiyor; --login ile tekrar deneyin` | Kart sertifikaları PIN olmadan göstermiyor → `list-certs --login` |
| PIN kilitlendi / PIN locked | Art arda yanlış PIN. PUK ile ESHS üzerinden açın; program PIN'i tekrar denemez. |
| `Zaman damgası alınamadı` | TSA adresi, ağ veya hesap sorunu; bkz. bölüm 5 |

## 4. Kullanım / Usage

```text
esign list-certs [--lib <pkcs11.so>] [--slot <n>] [--login]
esign sign --in <dosya> [--out <dosya>] [--format auto|cades|pades|xades] [--detached]
           [--reason <metin>] [--alias <etiket>] [--tsa <url>] [--lib <pkcs11.so>] [--slot <n>]
esign version
esign help
```

```bash
# Karttaki sertifikalar / certificates on the card
esign list-certs --login

# Her tür dosya → CAdES bütünleşik .imz (İmzager gibi) / any file → attached CAdES
esign sign --in rapor.docx                       # → rapor.docx.imz

# Ayrık imza: orijinal dosya değişmez / detached: original untouched
esign sign --in rapor.xlsx --detached            # → rapor.xlsx.p7s

# PDF → PAdES, XML → XAdES (uzantıdan seçilir / chosen by extension)
esign sign --in sozlesme.pdf --reason "Onaylıyorum"   # → sozlesme_imzali.pdf
esign sign --in yazi.xml                               # → yazi_imzali.xml

# PDF'i İmzager gibi .imz olarak / PDF as CAdES .imz
esign sign --in sozlesme.pdf --format cades

# Zaman damgalı (T profili) / with timestamp (T profile)
esign sign --in sozlesme.pdf --tsa http://zd.ornek-eshs.com.tr

# Görünür imza kutusu (son sayfa sağ alt, ya da sayfa/konum seçilir) / visible signature box
esign sign --in sozlesme.pdf --visible --reason "Onaylıyorum"
esign sign --in sozlesme.pdf --visible-at 2,50,700

# Uzun dönem (LT: OCSP/CRL gömülü, LTA: + arşiv zaman damgası) / long-term profiles
esign sign --in sozlesme.pdf --tsa http://zd.ornek-eshs.com.tr --level lta --trust ~/kamusm-kokler/

# Doğrulama / validation
esign verify --in rapor.docx.imz --trust ~/kamusm-kokler/
esign verify --in rapor.docx.p7s --original rapor.docx --trust ~/kamusm-kokler/ --report rapor.xml
```

| Biçim / Format | Varsayılan çıktı / Default output | Ne zaman / When |
| --- | --- | --- |
| CAdES bütünleşik | `<dosya>.imz` | Word, Excel, ZIP, TXT; İmzager ile paylaşım |
| CAdES ayrık | `<dosya>.p7s` | Orijinal dosya aynen kalmalıysa |
| PAdES | `<ad>_imzali.pdf` | PDF; birden çok kişi sırayla imzalayabilir |
| XAdES | `<ad>_imzali.xml` | XML resmi yazışma, e-belge |

Kurallar / Rules:

- PIN yalnızca terminalden sorulur; `--pin` argümanı bilerek yoktur. / PIN is only read from the terminal.
- Var olan çıktı dosyasının üzerine yazılmaz. / Existing output files are never overwritten.
- Kartta birden çok imza sertifikası varsa `--alias` ile seçin. / Use `--alias` if several signing certs exist.
- Çıkış kodları / exit codes: `0` başarı veya geçerli/success or valid, `1` kart veya dosya hatası/token or I/O error,
  `2` kullanım hatası/usage error, `3` doğrulama belirsiz/indeterminate, `4` imza geçersiz/invalid.

### İmza profilleri / Signature levels

| `--level` | İçerik / Content | Gerekenler / Needs |
| --- | --- | --- |
| `bes` (varsayılan, TSA yoksa) | İmza + sertifika / signature + certificate | Kart |
| `t` (varsayılan, TSA varsa) | + RFC 3161 imza zaman damgası | `--tsa` |
| `lt` | + zincir ve OCSP/CRL yanıtları imzaya gömülür / chain and revocation data embedded | `--tsa`, `--trust`, ağ (OCSP/CRL) |
| `lta` | + arşiv zaman damgası; süre dolmadan yeniden damgalanabilir / archive timestamp | `--tsa`, `--trust`, ağ |

### Doğrulama / Validation (`esign verify`)

EU DSS ile ETSI EN 319 102-1 doğrulaması yapılır: belge özeti, imza değeri, sertifika zinciri, güvenilir kök,
OCSP/CRL iptal durumu ve zaman damgaları. *Full ETSI EN 319 102-1 validation via EU DSS.*

| Sonuç / Verdict | Anlamı / Meaning | Çıkış kodu |
| --- | --- | --- |
| GEÇERLİ / valid | `TOTAL_PASSED` | 0 |
| BELİRSİZ / indeterminate | Güvenilir kök yok, iptal bilgisi alınamadı vb. / no trust anchor, no revocation data | 3 |
| GEÇERSİZ / invalid | Belge değişmiş, sertifika iptal edilmiş vb. / document changed, certificate revoked | 4 |

- **Güvenilir kökler / trust anchors:** `--trust <dosya|dizin>` veya `ESIGN_TRUST_DIR`. Kamu SM (ve diğer ESHS)
  kök sertifikalarını sağlayıcının resmî sitesinden indirip parmak izini doğrulayarak bu dizine koyun; program
  kök sertifika **dağıtmaz**. *Download root certificates from the provider and verify fingerprints; none are bundled.*
- `--offline`: ağa çıkılmaz; yalnızca imzanın içindeki veriler (LT/LTA) kullanılır. *No network access.*
- `--report rapor.xml`: denetim için DSS ayrıntılı XML raporu. *Detailed XML report for audits.*
- Güvenilir kök verilmezse sonuç en iyi ihtimalle **BELİRSİZ** olur. *Without anchors the best result is indeterminate.*

## 5. Zaman damgası / Timestamping

| Durum / Case | Sonuç / Result |
| --- | --- |
| `--tsa` yok ve `ESIGN_TSA_URL` boş | **BES** (zaman damgasız) / no timestamp |
| `--tsa <url>` veya `ESIGN_TSA_URL=<url>` | **T** = BES + RFC 3161 imza zaman damgası |
| TSA'ya ulaşılamazsa / TSA unreachable | Hata verilir, **imzasız/eksik dosya yazılmaz** / fails, nothing written |

- Zaman damgası, imza değerinin özetine (SHA-512) alınır ve imzanın *imzasız öznitelikleri*ne eklenir.
  *The token covers the signature value and is stored as an unsigned attribute.*
- Zaman damgası **belgenin o anda var olduğunu** kanıtlar; **kimin imzaladığını** kanıtlamaz. İmza sertifikası
  ve zaman damgası sertifikası ayrı ayrı doğrulanmalıdır. *A timestamp proves existence at a time, not identity.*
- Yasal zaman damgası için BTK'dan yetkili bir ESHS'nin hizmetini kullanın. Bazı ESHS'ler isteğe
  müşteri numarası/şifre ile kimlik doğrulaması ekler; bu sürüm **yalnızca kimlik doğrulamasız
  standart RFC 3161 HTTP(S)** sunucularını destekler. ESHS'nizin yöntemini sağlayıcıdan teyit edin.
  *Some Turkish providers require account authentication inside the request; this version supports plain RFC 3161 over HTTP(S) only.*
- Kullanıcı adı/şifre **URL'ye yazılmaz**; program böyle bir URL'yi reddeder.

## 6. Hukuki çerçeve ve sınırlar / Legal framework and limits

| Dayanak / Source | Konu / Subject |
| --- | --- |
| 5070 sayılı Elektronik İmza Kanunu, md. 4 | Güvenli elektronik imzanın tanımı: yalnızca imza sahibine bağlı, onun tasarrufundaki güvenli imza oluşturma aracıyla, nitelikli sertifikaya dayanarak oluşturulur ve sonradan yapılan değişikliği belli eder |
| 5070 s. Kanun, md. 5 | Güvenli elektronik imza elle atılan imza ile aynı hukuki sonucu doğurur; **resmî şekle veya özel merasime tabi işlemler ile teminat sözleşmeleri** güvenli e-imza ile yapılamaz |
| 5070 s. Kanun, md. 6 | Güvenli elektronik imza oluşturma araçlarının nitelikleri |
| 6100 sayılı HMK, md. 205 | Usulüne göre güvenli e-imza ile oluşturulan elektronik veriler senet hükmündedir |
| BTK ikincil mevzuatı (Yönetmelik ve Tebliğ) | ESHS'ler, nitelikli sertifika, teknik kriterler ve algoritmalar |
| 6698 sayılı KVKK / GDPR | Sertifikadaki kişisel veriler (ad, T.C. kimlik no) — program bunları saklamaz/göndermez |
| ETSI EN 319 122 / 142 / 132, RFC 3161, RFC 5652 | CAdES / PAdES / XAdES, zaman damgası, CMS |

**Sınırlar / Limits — lütfen okuyun / please read:**

1. Hukuki geçerlilik **nitelikli elektronik sertifikadan** ve **güvenli imza oluşturma aracından** (AKİS kart)
   gelir; bu yazılım yalnızca standart biçimde imza paketi üretir. Yazılımın teknik olarak imza üretebilmesi
   tek başına hukuki uygunluk anlamına gelmez.
2. Bu yazılım BTK, TÜBİTAK veya başka bir kurum tarafından **test edilmemiş, onaylanmamış ve sertifikalandırılmamıştır**.
3. `esign verify` teknik bir doğrulama raporu üretir; İmzager ile karşılıklı testler ve gerçek Kamu SM/ESHS
   ortamında uçtan uca testler henüz yapılmamıştır. Hukuki önemi olan kararlarda sonucu resmî bir
   doğrulayıcıyla (ör. İmzager) teyit edin.
4. Kurumsal veya resmî kullanım öncesi kurumunuzun bilgi güvenliği ve hukuk birimine danışın.

*EN: Legal effect under Turkish Law No. 5070 comes from the qualified certificate and the secure
signature-creation device, not from this software. The software is not certified, does not yet
validate signatures or check revocation, and must not be the sole basis for a legal decision.*

## 7. Güvenlik / Security

- Özel anahtar **kartın dışına çıkmaz**; imza kartın içinde hesaplanır.
- PIN argüman, dosya, log veya ortam değişkeni olarak alınmaz; kullanımdan sonra bellekten silinir.
- Gizli bilgiler (`.env`, `*.pem`, `*.p12`, `softhsm2.conf`) `.gitignore` ile depodan dışlanır.
- Bağımlılıklar: EU DSS 6.5.1, Bouncy Castle 1.80, Apache PDFBox (DSS üzerinden).
- Açık bildirimi için bkz. [SECURITY.md](SECURITY.md).

## 8. Kaynaktan derleme ve test / Build and test

```bash
sudo apt install openjdk-21-jdk maven            # macOS: brew install openjdk@21 maven
mvn -B verify                                     # 108 test
java -jar esign-cli/target/esign.jar help

# Kartsız gerçek PKCS#11 testi (SoftHSM2) / real PKCS#11 tests without a card
sudo apt install softhsm2 opensc
eval "$(scripts/softhsm-setup.sh)"
mvn -B verify

# Gömülü JRE'li paket / native package with embedded JRE
packaging/jpackage.sh deb       # Linux (fakeroot gerekir)  → dist/esign_<sürüm>_amd64.deb
packaging/jpackage.sh pkg       # macOS                     → dist/esign-<sürüm>.pkg
# Windows (PowerShell, WiX 3 + Git Bash): MSI üretir, kurar/kaldırır ve dener
pwsh packaging/windows/build-msi.ps1   # → dist/esign-<sürüm>-windows-x64.msi
```

Sürüm yayını / release: `pom.xml` dosyalarındaki sürümü artırıp `main`'e gönderin (veya `v*` etiketi
gönderin). GitHub Actions `release` iş akışı testleri çalıştırır; o sürüm henüz yayımlanmadıysa `v<sürüm>`
etiketini oluşturup `.deb`, `.pkg`, `.msi`, jar'lar, `SHA256SUMS.txt` ve her dosyanın Sigstore imzasını
(`*.sigstore.json`) Releases'e yükler.
*Bump the version and push to `main`; the workflow tags and publishes it once.*

## 9. Mimari / Architecture

Her modül tek bir işten sorumludur ve yalnızca alttaki katmana bağımlıdır (hexagonal / ports & adapters).
*Each module has one job and depends only on the layer below.*

```text
esign-cli ──► esign-desktop (Swing) ──► cades/pades/xades + validation + token-pkcs11
          ├─► esign-cades ─┐
          ├─► esign-pades ─┼─► esign-dss-support ──► esign-core (SigningToken portu, bağımlılıksız)
          ├─► esign-xades ─┤           │                    ▲
          ├─► esign-validation ┘       │                    │
          └─► esign-token-pkcs11 ──────┼────────────────────┘ (SunPKCS11 adaptörü)
esign-api (Spring Boot) ──► cades/pades/xades + validation (kart yok; imza istemcide)
                                       └─► EU DSS (LGPL-2.1): RFC 3161, OCSP/CRL, ETSI doğrulama
```

| Modül | Görev / Responsibility |
| --- | --- |
| `esign-core` | `SigningToken` portu, `CertificateInfo`; dış bağımlılık yok |
| `esign-token-pkcs11` | JDK SunPKCS11 ile kart erişimi (AKİS, SoftHSM, OpenSC) |
| `esign-dss-support` | Kart ↔ EU DSS köprüsü; `SignatureProfile` (B/T/LT/LTA), `Timestamping`, `TrustSettings` (kökler, OCSP/CRL) |
| `esign-validation` | `SignatureVerifier`: ETSI EN 319 102-1 doğrulama, sade sonuç + XML rapor |
| `esign-cades` / `-pades` / `-xades` | Biçime özgü imza üreticiler |
| `esign-desktop` | Türkçe Swing masaüstü arayüzü: dosya, sertifika, PIN, imzala, doğrula |
| `esign-cli` | `esign` komut satırı; `--server` ile API istemcisi; `esign gui` |
| `esign-api` | Spring Boot REST API: doğrulama, iki aşamalı imza, OpenAPI; veritabanı gerektirmez |

Yeni bir kart türü için yalnızca `SigningToken` uygulanır; yeni bir mevzuat profili için yalnızca ilgili
imzalayıcı değişir. Akademik açıklama: [docs/makale-taslagi.md](docs/makale-taslagi.md).

## 10. Yol haritası / Roadmap

1. ✅ Linux'ta kart erişimi, sertifika listeleme
2. ✅ CAdES-BES (`.imz`, `.p7s`)
3. ✅ PAdES-BES ve XAdES-BES
4. ✅ RFC 3161 zaman damgası (isteğe bağlı, T profili), `.deb` / macOS paketleri
5. ✅ İmza doğrulama (`esign verify`), OCSP/CRL, zincir, LT/LTA
6. ⏳ İmzager karşılıklı uyumluluk testleri, ✅ görünür PDF imzası (v0.4.0), ESHS kimlik doğrulamalı zaman damgası, ✅ masaüstü arayüz (v0.7.0)
7. ✅ REST API (Spring Boot, v0.5.0), ✅ Docker (v0.6.0)

## 11. Lisans / License

[GNU GPL v3.0](LICENSE). EU DSS LGPL-2.1, Bouncy Castle MIT benzeri, Apache PDFBox Apache-2.0
lisanslıdır; tümü GPL-3.0 ile uyumludur. / All dependencies are GPL-3.0 compatible.

## 12. REST API

`esign-api`, imza **doğrulama** ve **iki aşamalı imza** sunan bir Spring Boot servisidir. Sunucu akıllı kartı
göremez; bu yüzden özel anahtar ve PIN sunucuya **hiç gitmez**:

1. **Hazırla / prepare** — istemci belgeyi ve imzacının sertifika zincirini gönderir; sunucu imzalanacak
   veriyi (`dataToSign`, ETSI imzalı öznitelikler) döndürür.
2. **Kartta imzala / sign on card** — istemci bu baytları kendi kartında `signatureAlgorithm`
   (ör. `SHA256withRSA`) ile imzalar.
3. **Birleştir / complete** — sunucu imza değerini sertifikanın açık anahtarıyla doğrular, gerekirse zaman
   damgası/OCSP/CRL ekler ve imzalı belgeyi döndürür. Oturum tek kullanımlıktır, süresi sınırlıdır.

*The server never sees the private key or PIN: it prepares the data to be signed, the client signs it on the
card, the server verifies the value and assembles the signed document.*

```bash
mvn -B -pl esign-api -am package -DskipTests
java -jar esign-api/target/esign-api.jar                 # http://localhost:8080/swagger-ui.html

# Kartlı bilgisayardan sunucu üzerinden imza / sign through the API with the local card
ESIGN_API_KEY=... esign sign --in sozlesme.pdf --server http://localhost:8080 [--level t]
```

| Uç nokta / Endpoint | Açıklama |
| --- | --- |
| `POST /api/v1/signatures/prepare` | multipart: `file`, `certificate` (PEM/DER zincir); parametre: `format` auto/cades/pades/xades, `level` bes/t/lt/lta, `detached`, `reason` |
| `POST /api/v1/signatures/{id}/complete` | JSON `{"signatureValue":"<Base64>"}` → imzalı dosya (`X-Signature-Level` başlığı) |
| `DELETE /api/v1/signatures/{id}` | Hazırlanmış imzayı iptal et |
| `POST /api/v1/validations` | multipart: `file`, isteğe bağlı `original` (ayrık .p7s); `detailed=true` XML rapor ekler |
| `GET /v3/api-docs`, `/swagger-ui.html` | OpenAPI 3.1 belgesi ve deneme arayüzü |
| `GET /actuator/health` | Sağlık denetimi (liveness/readiness) |

Ayarlar yalnızca ortam değişkenleriyle verilir (sırlar depoya girmez) / configuration via environment only:

| Değişken | Varsayılan | Açıklama |
| --- | --- | --- |
| `ESIGN_API_KEY` | — (zorunlu) | `/api/**` için `X-API-Key`; en az 24 karakter. Sunucu yalnızca SHA-256 özetini tutar, sabit zamanlı karşılaştırır. Anahtar yoksa API **başlamaz** |
| `ESIGN_API_KEY_SHA256` | boş | Anahtar yerine yalnızca hex SHA-256 özet(ler)i; virgülle birden çok = anahtar rotasyonu |
| `ESIGN_RATE_LIMIT` | `120` | IP başına dakikalık `/api/**` istek sınırı (aşılınca `429` + `Retry-After`) |
| `ESIGN_API_DOCS` | `true` | `false`: Swagger UI ve OpenAPI belgesi yayınlanmaz |
| `ESIGN_ALLOW_ANONYMOUS` | `false` | Yalnızca yerel geliştirme: anahtarsız çalıştırma |
| `ESIGN_TSA_URL` | boş | RFC 3161 TSA; boşsa yalnızca BES |
| `ESIGN_TRUST_DIR` | boş | Güvenilir kökler (doğrulama ve LT/LTA için) |
| `ESIGN_OFFLINE` | `false` | `true`: OCSP/CRL/AIA sorgusu yok |
| `ESIGN_SESSION_TTL` / `ESIGN_MAX_SESSIONS` | `10m` / `200` | Bekleyen imza süresi ve üst sınırı |

Veritabanı gerekmez: API durumsuzdur; bekleyen imzalar yalnızca bellekte ve kısa süre tutulur, belgeler diske
yazılmaz. Dosya sınırı 25 MB. İnternete açarken önüne TLS sonlandıran bir ters vekil (nginx, Caddy) koyun ve
`ESIGN_API_KEY` tanımlayın. CLI, API anahtarını şifresiz `http` ile yalnızca `localhost`'a gönderir; uzak sunucu
için `https` zorunludur. Ayrıntılar: [SECURITY.md](SECURITY.md). *No database needed. Put a TLS reverse proxy in front and set an API key before
exposing it.* PAdES görünür imza kutusu şimdilik yalnızca CLI'dadır.

## 13. Docker

API, kök olmayan kullanıcıyla, salt okunur dosya sisteminde ve tüm Linux yetkileri düşürülmüş olarak çalışır.
PostgreSQL gerekmez (API durumsuzdur). *Runs as non-root, read-only root FS, all capabilities dropped; no
database required.*

```bash
cp .env.example .env            # ESIGN_API_KEY=$(openssl rand -base64 32) gibi değerleri girin; .env git'e girmez
docker compose up -d            # yalnızca 127.0.0.1:8080'de dinler
curl http://localhost:8080/actuator/health

# Hazır görüntü (her sürümde) / prebuilt image on each release
docker run -d -p 127.0.0.1:8080:8080 -e ESIGN_API_KEY=... ghcr.io/dogu-omer/esign-api:latest
```

Güvenilir kök sertifikaları (Kamu SM vb.) `./trust` klasörüne koyup `.env` içinde
`ESIGN_TRUST_DIR_IN_CONTAINER=/trust` yazın. Zaman damgası için `ESIGN_TSA_URL`. CI her gönderimde görüntüyü
derleyip sağlık ve OpenAPI denetimi yapar; sürümlerde `ghcr.io/dogu-omer/esign-api:<sürüm>` yayımlanır.

## 14. Masaüstü arayüzü / Desktop app

Komut satırı bilmeyen kullanıcılar için Türkçe pencere: **İmzala**, **Doğrula** ve **Hakkında** sekmeleri.
*A Turkish GUI for users who do not use the terminal.*

| Nasıl açılır / How to open | |
| --- | --- |
| Linux (`.deb`) | Uygulama menüsü → Ofis → **esign-gui**; veya terminalde `esign gui` |
| macOS (`.pkg`) | Finder'dan `esign` uygulamasını açın (konsolsuz başlatma arayüzü açar) |
| JAR | `java -jar esign.jar gui` |

İmzala: dosya seçin → PKCS#11 kütüphanesi otomatik bulunur (AKİS) → **Karttaki sertifikalar** → biçim
(otomatik/CAdES/PAdES/XAdES), profil (BES/T/LT/LTA), ayrık imza, görünür PDF imzası, neden → **İmzala** → PIN.
PIN yalnızca parola kutusunda girilir, hiçbir yere yazılmaz ve iş bitince bellekten silinir; özel anahtar kartı
terk etmez. Mevcut dosyanın üzerine yazılmaz (`belge (2).pdf` gibi yeni ad verilir). Zaman damgası adresi
`ESIGN_TSA_URL` ortam değişkeninden doldurulur.

Doğrula: imzalı dosya (ayrık imzada asıl belge), güvenilir kök klasörü ve çevrimdışı seçeneği; sonuç
GEÇERLİ / BELİRSİZ / GEÇERSİZ olarak renkli gösterilir.

## 15. Windows

### Kurulum / Installation

1. [Releases](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/releases) sayfasından `esign-<sürüm>-windows-x64.msi` dosyasını indirin.
   Java kurmanıza gerek yoktur; paket kendi Java çalışma ortamını (JRE) içerir.
2. Çift tıklayıp kurun (yönetici izni ister). Varsayılan klasör `C:\Program Files\esign`.
3. Kurulum şunları yapar / the installer:
   - **Başlat menüsü → esign → esign-gui** ve masaüstü kısayolu (masaüstü arayüzü),
   - kurulum klasörünü **sistem PATH**'ine ekler; yeni bir komut isteminde `esign` çalışır,
   - kaldırıldığında (Ayarlar → Uygulamalar) PATH girdisini de siler.

```bat
:: Yeni bir cmd / PowerShell penceresi açın / open a NEW terminal
esign version
esign list-certs
esign sign belge.pdf
esign gui
```

### AKİS kart sürücüsü / Smart card driver

AKİS Windows sürücüsünü TÜBİTAK BİLGEM'den veya kartınızı aldığınız ESHS'den kurun. Sürücü
`akisp11.dll` dosyasını genellikle `C:\Windows\System32` klasörüne koyar ve program onu **kendiliğinden bulur**
(bölüm 3'teki tabloya bakın). Windows'un kendi akıllı kart hizmeti (`SCardSvr`) kullanılır; ek servis gerekmez.
*The AKİS driver usually installs `akisp11.dll` into System32, where it is found automatically.*

DLL başka yerdeyse / if the DLL is elsewhere:

| Yöntem / Method | Örnek / Example |
| --- | --- |
| Komut başına / per command | `esign sign belge.pdf --lib "C:\Program Files\AKIS\akisp11.dll"` |
| Kalıcı (kullanıcı) / persistent | `setx ESIGN_PKCS11_LIB "C:\Program Files\AKIS\akisp11.dll"` (yeni pencere açın) |
| Masaüstü arayüzü / GUI | *Kart kütüphanesi* alanından seçin; son kullanılan yol hatırlanır |

> Uygulama 64 bittir; **64 bit** `akisp11.dll` gerekir. `SysWOW64` altındaki 32 bit DLL çalışmaz.
> *The app is 64-bit and needs the 64-bit DLL.*

### SmartScreen uyarısı / SmartScreen warning

Sürüm paketleri şu an bir **Authenticode kod imzalama sertifikasıyla imzalı değildir** (proje ücretli sertifika
almadıkça). Bu yüzden Windows *"Windows kişisel bilgisayarınızı korudu"* / *"Bilinmeyen yayıncı"* uyarısı
gösterebilir. Dosyanın bu depodan geldiğini **SHA256SUMS ve Sigstore imzasıyla** (bölüm 16) doğruladıktan sonra
**Ek bilgi → Yine de çalıştır** seçin. Tarayıcı indirmeyi engellerse *Tut / Keep* deyin.

*The MSI is not Authenticode-signed (no paid code-signing certificate yet), so SmartScreen may warn about an
unknown publisher. Verify the download (§16), then choose **More info → Run anyway**.*

### Authenticode imzası (bakımcılar için) / Code signing for maintainers

İş akışı imzalamaya hazırdır; sertifika eklenince bir sonraki sürüm kendiliğinden imzalanır. Sırlar **yalnızca**
*GitHub → Settings → Secrets and variables → Actions → New repository secret* altına girilir; depoya asla
dosya olarak eklenmez (`.gitignore` `*.pfx`, `*.p12` dosyalarını dışlar).

**A) PFX sertifikası (OV/EV kod imzalama)**

1. Bir sertifika otoritesinden (ör. DigiCert, Sectigo, GlobalSign, Certum; Türkiye'de kod imzalama satan ESHS'ler)
   **kod imzalama sertifikası** alın. 2023'ten beri özel anahtar donanımda (token/HSM) tutulmak zorunda
   olduğundan çoğu sağlayıcı PFX yerine bulut imzalama sunar; PFX alabiliyorsanız bu yolu kullanın,
   alamıyorsanız B'yi seçin.
2. PFX'i base64'e çevirin (PowerShell):
   `[Convert]::ToBase64String([IO.File]::ReadAllBytes("cert.pfx")) | Set-Clipboard`
3. Secrets: `WINDOWS_PFX_BASE64` (panodaki metin), `WINDOWS_PFX_PASSWORD` (PFX parolası).
   İsteğe bağlı `WINDOWS_TIMESTAMP_URL` (varsayılan `http://timestamp.digicert.com`).

**B) Azure Artifact Signing (eski adıyla Trusted Signing)**

1. Azure'da *Artifact Signing* hesabı ve *Public Trust* sertifika profili oluşturun, kimlik doğrulamasını tamamlayın.
2. Bir uygulama kaydı (service principal) açıp ona *Artifact Signing Certificate Profile Signer* (eski adı *Trusted Signing Certificate Profile Signer*) rolünü verin.
3. Secrets: `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_SIGNING_ENDPOINT`
   (ör. `https://weu.codesigning.azure.net/`), `AZURE_SIGNING_ACCOUNT`, `AZURE_CERT_PROFILE`.

İkisi de tanımlı değilse imza adımı atlanır ve MSI imzasız yayımlanır. İkisi birden tanımlıysa PFX kullanılır.
Not: Yalnızca `.msi` imzalanır; içindeki `esign.exe` başlatıcıları imzasızdır (SmartScreen indirilen MSI'ya bakar).
*If no secret is set the signing step is skipped. Only the MSI is signed.*

## 16. Sürüm imzaları ve doğrulama / Release signatures

Her sürümdeki **tüm dosyalar** (`.deb`, `.pkg`, `.msi`, jar'lar ve `SHA256SUMS.txt`) GitHub Actions içinde
[Sigstore](https://www.sigstore.dev/) **cosign** ile anahtarsız (keyless) imzalanır: imza, bu deponun `release.yml`
iş akışının kimliğine bağlıdır ve herkese açık Rekor şeffaflık kaydına yazılır. Her dosyanın yanında
`<dosya>.sigstore.json` paketi bulunur. Ayrıca GitHub **artifact attestation** (SLSA derleme kökeni) üretilir.
Bu imzalar ücretsizdir ama Windows SmartScreen veya macOS Gatekeeper tarafından tanınmaz; onlar için
Authenticode / Apple notarization gerekir (bölüm 15).

*Every release file is signed keyless with Sigstore cosign (bound to this repo's release workflow, logged in
Rekor) and has a GitHub build-provenance attestation. These do not replace Authenticode/notarization.*

```bash
# 1) cosign ile / with cosign (https://docs.sigstore.dev/cosign/system_config/installation/)
F=esign-0.8.0-windows-x64.msi
cosign verify-blob --bundle "$F.sigstore.json" \
  --certificate-identity-regexp '^https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/\.github/workflows/release\.yml@' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com "$F"

# SHA256SUMS'ı imzayla doğrulayıp sonra özetleri kontrol etmek tüm dosyaları kapsar
# verifying the signed SHA256SUMS then the checksums covers every file
cosign verify-blob --bundle SHA256SUMS.txt.sigstore.json \
  --certificate-identity-regexp '^https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt --ignore-missing

# 2) GitHub CLI ile / with the GitHub CLI
gh attestation verify esign-0.8.0-windows-x64.msi -R dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a
```

Windows PowerShell'de özet / checksum on Windows: `Get-FileHash .\esign-0.8.0-windows-x64.msi -Algorithm SHA256`
