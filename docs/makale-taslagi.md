# Linux ve macOS İçin Açık Kaynak, Modüler Bir Elektronik İmza Platformu: PKCS#11 Tabanlı CAdES, PAdES ve XAdES Uygulaması

**Taslak — v0.2.0 (17 Kasım 2026).** Yazar: Ömer Doğu. Bu metin bir çalışma taslağıdır; sonuçlar
yazılımın v0.2.0 sürümüne aittir ve hakemli yayın öncesinde genişletilmelidir.

---

## Özet

Türkiye'de güvenli elektronik imza, 5070 sayılı Elektronik İmza Kanunu uyarınca nitelikli elektronik
sertifika ve güvenli imza oluşturma aracı (çoğunlukla AKİS akıllı kart) ile oluşturulmaktadır. Yaygın
imza istemcileri ağırlıklı olarak Windows'a ve lisanslı kütüphanelere bağımlıdır; bu durum GNU/Linux ve
macOS kullanan kamu kurumları, üniversiteler ve geliştiriciler için taşınabilirlik ve denetlenebilirlik
sorunları doğurmaktadır. Bu çalışmada, Avrupa Komisyonu'nun açık kaynak EU DSS kütüphanesi üzerine
kurulu, PKCS#11 aracılığıyla akıllı kart kullanan ve CAdES, PAdES ve XAdES biçimlerinde imza üreten
modüler bir platform sunulmaktadır. Platform, altıgen (ports & adapters) mimariyle kart erişimini imza
biçimlerinden ayırır; isteğe bağlı RFC 3161 zaman damgası ile ETSI baseline B (BES) ve T profillerini
destekler; uzun dönem LT/LTA profillerini ve ETSI EN 319 102-1 uyumlu imza doğrulamasını (zincir,
OCSP/CRL, zaman damgası) sağlar. Gerçek PKCS#11 davranışı SoftHSM2 sanal belirteci, iptal ve uzun dönem
senaryoları ise bellek içi bir test PKI'si (kök CA, CRL, RFC 3161 sunucusu) ile otomatik testlere dahil
edilmiş; 78 birim ve entegrasyon testi başarıyla çalıştırılmıştır. Çalışma ayrıca yazılımın teknik olarak
imza üretmesi ile hukuki geçerlilik arasındaki farkı tartışmakta ve İmzager karşılıklı uyumluluğu için bir
yol haritası önermektedir.

**Anahtar kelimeler:** elektronik imza, CAdES, PAdES, XAdES, PKCS#11, zaman damgası, 5070 sayılı Kanun, açık kaynak, GNU/Linux

## Abstract

In Turkey, secure electronic signatures under Law No. 5070 are created with a qualified certificate and a
secure signature-creation device, usually an AKİS smart card. Most signing clients depend on Windows and
proprietary libraries, which limits portability and auditability for GNU/Linux and macOS users. This
paper presents a modular, open-source platform built on the European Commission's EU DSS library that
signs with smart cards via PKCS#11 and produces CAdES, PAdES and XAdES signatures. A hexagonal
architecture separates card access from signature formats, and optional RFC 3161 timestamping selects
between the ETSI baseline B and T levels; LT/LTA levels and ETSI EN 319 102-1 validation (chain,
OCSP/CRL, timestamps) are provided. Real PKCS#11 behaviour is exercised through a SoftHSM2 virtual token
and revocation scenarios through an in-memory test PKI; 78 unit and integration tests pass. We discuss the
gap between technically producing a signature and its legal effect, and outline a roadmap towards
interoperability with the reference client İmzager.

**Keywords:** electronic signature, CAdES, PAdES, XAdES, PKCS#11, timestamping, open source, GNU/Linux

---

## 1. Giriş

Elektronik imza, kamu ve özel sektörde belge akışlarının dijitalleşmesinin temel yapı taşıdır.
5070 sayılı Kanun'un 5. maddesine göre güvenli elektronik imza, elle atılan imza ile aynı hukuki
sonucu doğurur; ancak resmî şekle veya özel merasime tabi işlemler ve teminat sözleşmeleri bu kapsamın
dışındadır. Güvenli imzanın ön koşulları yazılımdan çok sertifika ve donanımla ilgilidir; yine de imzayı
standartlara uygun biçimde paketleyen, kullanıcının kontrolünü koruyan ve denetlenebilir olan bir istemci
yazılımı zorunludur.

Mevcut durumda üç sorun öne çıkmaktadır: (i) istemcilerin işletim sistemi bağımlılığı, (ii) kaynak kodu
kapalı kütüphanelerin denetlenememesi, (iii) imza üretimi ile doğrulama, zaman damgası ve uzun dönem
arşivleme işlevlerinin tek parça yazılımlarda iç içe geçmesi. Bu çalışmanın katkıları şunlardır:

1. PKCS#11 üzerinden AKİS ve benzeri kartlarla çalışan, Linux ve macOS'ta gömülü çalışma ortamıyla
   dağıtılan açık kaynak bir imza istemcisi;
2. kart erişimi, imza biçimleri ve zaman damgasını birbirinden bağımsız modüllere ayıran bir mimari;
3. gerçek PKCS#11 davranışını donanımsız test eden yeniden üretilebilir bir test düzeni;
4. teknik uygunluk ile hukuki geçerlilik arasındaki sınırın açıkça belgelenmesi.

## 2. Arka plan ve ilgili çalışmalar

| Standart | Kapsam |
| --- | --- |
| RFC 5652 (CMS) | İmzalı veri yapısı; CAdES'in temeli |
| ETSI EN 319 122 (CAdES) | CMS tabanlı gelişmiş imza; `.imz` / `.p7s` |
| ETSI EN 319 142 (PAdES) | PDF içine gömülü imza; artımlı güncelleme ile çoklu imza |
| ETSI EN 319 132 (XAdES) | XML-DSig tabanlı gelişmiş imza |
| RFC 3161 | Zaman damgası protokolü (TSP) |
| PKCS#11 (OASIS) | Akıllı kart/HSM için kriptografik belirteç arayüzü |

ETSI baseline profilleri B (temel), T (zaman damgalı), LT (doğrulama verisi gömülü) ve LTA (arşiv zaman
damgalı) seviyelerinden oluşur. Türkiye'deki uygulamada BES ve ES-T adları da kullanılmaktadır.

İlgili yazılımlar: TÜBİTAK BİLGEM'in İmzager istemcisi ve MA3 API'si Türkiye'de referans kabul
edilmektedir; ancak MA3 lisans gerektirir. EU DSS (LGPL-2.1) Avrupa'da eIDAS kapsamında yaygın
kullanılan açık kaynak bir kütüphanedir ve bu çalışmada imza motoru olarak seçilmiştir.

## 3. Yöntem ve mimari

### 3.1 Tasarım ilkeleri

- **Tek sorumluluk ve en az bağımlılık:** her Maven modülü yalnızca bir iş yapar.
- **Port ve adaptör:** `SigningToken` arayüzü (port) imza işlemini soyutlar; PKCS#11 bir adaptördür.
- **Özel anahtar kartta kalır:** yazılım yalnızca imzalanacak özeti karta gönderir.
- **Güvenli varsayılanlar:** PIN argüman olarak alınmaz, çıktı dosyalarının üzerine yazılmaz.

### 3.2 Modüller

| Modül | Bağımlılık | Görev |
| --- | --- | --- |
| `esign-core` | yok | `SigningToken`, `CertificateInfo` |
| `esign-token-pkcs11` | core, JDK SunPKCS11 | Kart oturumu, sertifika listeleme, kartta imza |
| `esign-dss-support` | core, EU DSS | İki aşamalı DSS akışı; `SignatureProfile`, `Timestamping`, `TrustSettings` |
| `esign-cades/pades/xades` | dss-support | Biçime özgü parametreler |
| `esign-validation` | dss-support, EU DSS | ETSI EN 319 102-1 doğrulama, sade sonuç ve XML rapor |
| `esign-cli` | tümü | Komut satırı |

### 3.3 İmza akışı

1. Kullanıcı dosyayı seçer; biçim uzantıdan belirlenir (PDF → PAdES, XML → XAdES, diğer → CAdES).
2. DSS, imzalanacak öznitelikleri (imzacı sertifikası özeti, imza zamanı, belge özeti) hazırlar
   (`getDataToSign`).
3. Bu veri SHA-256 ile kartta imzalanır; özel anahtar karttan çıkmaz.
4. Zaman damgası açıksa imza değerinin özeti RFC 3161 sunucusuna gönderilir ve dönen belirteç imzanın
   imzasız özniteliğine eklenir (T profili).
5. Sonuç yalnızca tüm adımlar başarılıysa diske yazılır.

### 3.4 Zaman damgasının isteğe bağlı oluşu

`Timestamping` değer nesnesi iki durumu temsil eder: `none()` → BES, `online(url)` → T. Böylece imza
üreticileri hangi profili üreteceğini tek bir yerden öğrenir; yeni bir zaman damgası sağlayıcısı
(ör. kimlik doğrulamalı ESHS istemcisi) yalnızca bu sınıfa yeni bir fabrika yöntemi eklenerek
desteklenebilir. Kimlik bilgisi içeren URL'ler güvenlik nedeniyle reddedilir.

### 3.5 Uzun dönem profiller ve doğrulama

`SignatureSettings` değer nesnesi profil (B/T/LT/LTA), zaman damgası ve güven ayarlarını tutarlılık
kurallarıyla birleştirir: T ve üstü zaman damgası; LT ve LTA ayrıca güvenilir kök ve çevrimiçi iptal
kaynağı (OCSP/CRL) ister. LT imzaya zincir ve iptal yanıtlarını gömer; LTA arşiv zaman damgası ekler.
Doğrulama (`SignatureVerifier`) EU DSS'in ETSI EN 319 102-1 uygulamasını kullanır ve sonucu üç sade
kategoriye (GEÇERLİ, BELİRSİZ, GEÇERSİZ) indirger; ayrıntılı XML rapor denetim için saklanabilir.
Kök sertifikalar pakete gömülmez; kullanıcı resmî kaynaktan alır.

## 4. Uygulama

Platform Java 21 ile yazılmış, EU DSS 6.5.1 ve Bouncy Castle 1.86 kullanmaktadır. Dağıtım `jpackage`
ile jlink'le küçültülmüş gömülü bir çalışma ortamı içerir; `jdk.crypto.cryptoki` modülü PKCS#11
erişimi için açıkça eklenmiştir. Paketler GitHub Actions üzerinde Linux (`.deb`) ve macOS (`.pkg`)
için üretilmekte, SHA-256 özetleriyle yayımlanmaktadır.

## 5. Test ve değerlendirme

### 5.1 Test düzeni

Gerçek kart olmadan PKCS#11 davranışını sınamak için SoftHSM2 üzerinde, `nonRepudiation` anahtar
kullanımına sahip sahte bir imza sertifikası oluşturan betik (`scripts/softhsm-setup.sh`) yazılmıştır.
Zaman damgası testleri için Bouncy Castle ile bellek içi bir RFC 3161 sunucusu (`TestTsa`), iptal ve uzun
dönem testleri için kök CA, imzacı ve TSA sertifikaları ile istek üzerine CRL üreten bir test PKI'si
(`TestPki`) kullanılmıştır.

### 5.2 Sonuçlar (v0.4.0)

| Test grubu | Sayı | Doğrulanan özellik |
| --- | --- | --- |
| Çekirdek ve PKCS#11 | 14 | Sertifika ayrıştırma, kütüphane bulma, SoftHSM'de imza |
| Profil ve zaman damgası ayarları | 6 | BES/T/LT/LTA tutarlılığı, geçersiz/kimlik bilgili URL reddi |
| CAdES | 7 | Bütünleşik/ayrık imza, BES öznitelikleri, değiştirilmiş belge tespiti, T profili |
| PAdES | 10 | ETSI.CAdES.detached, çoklu imza, artımlı güncelleme, T profili, görünür imza kutusu |
| XAdES | 4 | Enveloped imza, SigningCertificateV2, SignatureTimeStamp |
| Doğrulama | 9 | Geçerli BES, güvenilmeyen kök, değiştirilmiş belge, T/LT/LTA, iptal edilmiş sertifika |
| CLI | 28 | Argüman ayrıştırma, PIN reddi, TSA/profil seçimi, `verify` çıkış kodları |
| **Toplam** | **78** | **78 başarılı, 0 başarısız** |

Ek olarak `.deb` paketi temiz bir sisteme kurulmuş ve SoftHSM belirteci ile `esign sign` komutunun
ürettiği CAdES dosyası `openssl cms -verify` ile kriptografik olarak doğrulanmıştır. Gerçek bir ESHS zaman
damgası sunucusuyla uçtan uca test bu sürümde yapılamamıştır.

### 5.3 Değerlendirme ölçütleri (gelecek çalışma)

1. İmzager ile karşılıklı doğrulama oranı (her biçim için n belge);
2. ETSI/EU DSS doğrulayıcısı ile uyumluluk raporu;
3. İmza süresi (kart gecikmesi dahil) ve paket boyutu;
4. Farklı dağıtımlarda (Debian, Ubuntu, Pardus, Kali, macOS) kurulum başarısı.

## 6. Hukuki değerlendirme

5070 sayılı Kanun'un 4. maddesindeki güvenli elektronik imza tanımı; imzanın münhasıran imza sahibine
bağlı olmasını, imza sahibinin tasarrufundaki güvenli imza oluşturma aracıyla oluşturulmasını,
nitelikli sertifikaya dayanmasını ve sonradan yapılan değişikliklerin tespit edilebilmesini arar. Bu
koşulların ilk üçü kart ve sertifika ile; sonuncusu ise standart imza biçimleriyle sağlanır. Dolayısıyla
yazılım, hukuki geçerliliğin **gerekli fakat yeterli olmayan** bir bileşenidir. 6100 sayılı HMK'nın 205.
maddesi güvenli elektronik imzalı verileri senet hükmünde saydığından, uzun dönem doğrulanabilirlik
(LT/LTA) delil değeri açısından kritiktir. Sertifikalarda yer alan kişisel veriler 6698 sayılı KVKK
kapsamında olup yazılım bu verileri saklamaz veya üçüncü taraflara iletmez.

## 7. Sınırlılıklar

- Doğrulama ve LT/LTA, gerçek Kamu SM/ESHS altyapısında uçtan uca test edilmemiştir (yalnızca test PKI'si).
- İmzager ile karşılıklı uyumluluk henüz ölçülmemiştir.
- Kimlik doğrulamalı ESHS zaman damgası protokolleri desteklenmemektedir.
- Yazılım herhangi bir kurum tarafından sertifikalandırılmamıştır.
- macOS paketi notarize edilmemiştir; yalnızca Apple Silicon için paket üretilmektedir.

## 8. Sonuç ve gelecek çalışmalar

Bu çalışma, açık kaynak bileşenlerle Linux ve macOS üzerinde standartlara uygun elektronik imza
üretmenin mümkün olduğunu ve modüler bir mimarinin mevzuat veya donanım değişikliklerine karşı
bakım maliyetini düşürdüğünü göstermektedir. Doğrulama ve LT/LTA v0.3.0 ile eklenmiştir. Sonraki adımlar:
(1) gerçek Kamu SM kökleri ve ESHS ile uçtan uca test, (2) İmzager karşılıklı test kümesi, (3) REST API ve
masaüstü arayüz, (4) ESHS kimlik doğrulamalı zaman damgası istemcisi.

## Kaynaklar

1. 5070 sayılı Elektronik İmza Kanunu, Resmî Gazete, 23.01.2004, S. 25355.
2. 6100 sayılı Hukuk Muhakemeleri Kanunu, md. 205.
3. 6698 sayılı Kişisel Verilerin Korunması Kanunu.
4. ETSI EN 319 122-1, *CAdES digital signatures; Part 1: Building blocks and CAdES baseline signatures*.
5. ETSI EN 319 142-1, *PAdES digital signatures; Part 1: Building blocks and PAdES baseline signatures*.
6. ETSI EN 319 132-1, *XAdES digital signatures; Part 1: Building blocks and XAdES baseline signatures*.
7. RFC 3161, *Internet X.509 PKI Time-Stamp Protocol (TSP)*, IETF, 2001.
8. RFC 5652, *Cryptographic Message Syntax (CMS)*, IETF, 2009.
9. OASIS, *PKCS #11 Cryptographic Token Interface Base Specification*.
10. European Commission, *Digital Signature Service (DSS)*, https://github.com/esig/dss.

> Yayın öncesi kontrol listesi: Resmî Gazete künyesini ve ETSI sürüm numaralarını resmî kaynaktan
> doğrulayın; İmzager deney sonuçlarını ekleyin.
