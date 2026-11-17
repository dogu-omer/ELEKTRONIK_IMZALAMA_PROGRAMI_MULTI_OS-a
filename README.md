# esign — Linux ve macOS için Elektronik İmza / E-Signature for Linux and macOS

[![ci](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/actions/workflows/ci.yml/badge.svg)](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/actions/workflows/ci.yml)
[![Lisans: GPL-3.0](https://img.shields.io/badge/lisans-GPL--3.0-blue.svg)](LICENSE)

**TR —** AKİS akıllı kart veya USB token (PKCS#11) ile **CAdES**, **PAdES** ve **XAdES** formatında
elektronik imza üreten, açık kaynak, Java tabanlı komut satırı aracı. İsteğe bağlı **RFC 3161 zaman
damgası** ile BES veya T profilinde imza atar. İmza motoru olarak Avrupa Komisyonu'nun
[EU DSS](https://github.com/esig/dss) kütüphanesini kullanır.

**EN —** Open-source Java command-line tool that creates **CAdES**, **PAdES** and **XAdES** signatures
with a PKCS#11 smart card or USB token (e.g. Turkish AKİS cards), with optional **RFC 3161
timestamping** (BES, T, LT, LTA profiles) and **validates** signatures (ETSI EN 319 102-1, OCSP/CRL). Built on the European Commission's [EU DSS](https://github.com/esig/dss).

> ⚠️ **Durum / Status: v0.4.0 — Faz 6 / 7 sürüyor.** İmza üretimi, doğrulama, OCSP/CRL, LT/LTA ve görünür PDF imzası hazırdır;
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
| REST API | ⏳ Faz 7 | — |

## 2. Kurulum / Installation

Hazır paketler [Releases](https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a/releases)
sayfasındadır. Paketler kendi Java çalışma ortamını içerir; ayrıca Java kurmanız gerekmez.
*Packages ship their own Java runtime; no separate Java install is needed.*

| Sistem / OS | Dosya / File | Kurulum / Install |
| --- | --- | --- |
| Debian, Ubuntu, Kali, Pardus (amd64) | `esign_<sürüm>_amd64.deb` | `sudo apt install ./esign_*_amd64.deb` |
| macOS (Apple Silicon) | `esign-<sürüm>-macos-arm64.pkg` | Çift tıklayın / double-click |
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
mvn -B verify                                     # 78 test
java -jar esign-cli/target/esign.jar help

# Kartsız gerçek PKCS#11 testi (SoftHSM2) / real PKCS#11 tests without a card
sudo apt install softhsm2 opensc
eval "$(scripts/softhsm-setup.sh)"
mvn -B verify

# Gömülü JRE'li paket / native package with embedded JRE
packaging/jpackage.sh deb       # Linux (fakeroot gerekir)  → dist/esign_<sürüm>_amd64.deb
packaging/jpackage.sh pkg       # macOS                     → dist/esign-<sürüm>.pkg
```

Sürüm yayını / release: `pom.xml` dosyalarındaki sürümü artırıp `main`'e gönderin (veya `v*` etiketi
gönderin). GitHub Actions `release` iş akışı testleri çalıştırır; o sürüm henüz yayımlanmadıysa `v<sürüm>`
etiketini oluşturup `.deb`, `.pkg`, `esign.jar` ve `SHA256SUMS.txt` dosyalarını Releases'e yükler.
*Bump the version and push to `main`; the workflow tags and publishes it once.*

## 9. Mimari / Architecture

Her modül tek bir işten sorumludur ve yalnızca alttaki katmana bağımlıdır (hexagonal / ports & adapters).
*Each module has one job and depends only on the layer below.*

```text
esign-cli ──► esign-cades ─┐
          ├─► esign-pades ─┼─► esign-dss-support ──► esign-core (SigningToken portu, bağımlılıksız)
          ├─► esign-xades ─┤           │                    ▲
          ├─► esign-validation ┘       │                    │
          └─► esign-token-pkcs11 ──────┼────────────────────┘ (SunPKCS11 adaptörü)
                                       └─► EU DSS (LGPL-2.1): RFC 3161, OCSP/CRL, ETSI doğrulama
```

| Modül | Görev / Responsibility |
| --- | --- |
| `esign-core` | `SigningToken` portu, `CertificateInfo`; dış bağımlılık yok |
| `esign-token-pkcs11` | JDK SunPKCS11 ile kart erişimi (AKİS, SoftHSM, OpenSC) |
| `esign-dss-support` | Kart ↔ EU DSS köprüsü; `SignatureProfile` (B/T/LT/LTA), `Timestamping`, `TrustSettings` (kökler, OCSP/CRL) |
| `esign-validation` | `SignatureVerifier`: ETSI EN 319 102-1 doğrulama, sade sonuç + XML rapor |
| `esign-cades` / `-pades` / `-xades` | Biçime özgü imza üreticiler |
| `esign-cli` | `esign` komut satırı |

Yeni bir kart türü için yalnızca `SigningToken` uygulanır; yeni bir mevzuat profili için yalnızca ilgili
imzalayıcı değişir. Akademik açıklama: [docs/makale-taslagi.md](docs/makale-taslagi.md).

## 10. Yol haritası / Roadmap

1. ✅ Linux'ta kart erişimi, sertifika listeleme
2. ✅ CAdES-BES (`.imz`, `.p7s`)
3. ✅ PAdES-BES ve XAdES-BES
4. ✅ RFC 3161 zaman damgası (isteğe bağlı, T profili), `.deb` / macOS paketleri
5. ✅ İmza doğrulama (`esign verify`), OCSP/CRL, zincir, LT/LTA
6. ⏳ İmzager karşılıklı uyumluluk testleri, ✅ görünür PDF imzası (v0.4.0), ESHS kimlik doğrulamalı zaman damgası, masaüstü arayüz
7. ⏳ REST API (Spring Boot), Docker

## 11. Lisans / License

[GNU GPL v3.0](LICENSE). EU DSS LGPL-2.1, Bouncy Castle MIT benzeri, Apache PDFBox Apache-2.0
lisanslıdır; tümü GPL-3.0 ile uyumludur. / All dependencies are GPL-3.0 compatible.
