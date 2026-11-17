# ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS

Linux ve macOS üzerinde nitelikli elektronik imza (AKİS akıllı kart / USB token, PKCS#11) ile
CAdES, PAdES ve XAdES imza üretmeyi ve doğrulamayı hedefleyen açık kaynak Java platformu.

*Open-source Java platform for qualified e-signatures (CAdES/PAdES/XAdES) on Linux and macOS via PKCS#11 smart cards.*

> **Durum: Faz 3 / 7.** Karttaki sertifikaları listeler; CAdES-BES (`.imz` / `.p7s`), PAdES-BES (PDF) ve XAdES-BES (XML) imza üretir.
> Zaman damgası (T), uzun dönem doğrulama (LT/LTA) ve İmzager uyumluluk testleri sonraki fazlardadır.
> Bu yazılım BTK veya TÜBİTAK tarafından onaylanmış/sertifikalandırılmış değildir.
> Güvenli elektronik imzanın hukuki geçerliliği (5070 sayılı Kanun) nitelikli sertifika ve
> güvenli imza oluşturma aracından gelir; yazılımdan değil.

## Modüller

| Modül | Görev |
| --- | --- |
| `esign-core` | Port arayüzleri (`SigningToken`) ve alan modeli; dış bağımlılığı yok |
| `esign-token-pkcs11` | JDK SunPKCS11 ile kart erişimi (AKİS, SoftHSM, diğer PKCS#11) |
| `esign-dss-support` | Kart ile [EU DSS](https://github.com/esig/dss) (LGPL-2.1) arasındaki ortak imza köprüsü |
| `esign-cades` | CAdES-BES: Word, Excel, ZIP ve diğer tüm dosyalar |
| `esign-pades` | PAdES-BES: PDF içine imza, çoklu imza (artımlı güncelleme) |
| `esign-xades` | XAdES-BES: XML içine (enveloped) imza |
| `esign-cli` | `esign` komut satırı aracı |

## Gereksinimler (Debian / Ubuntu / Kali)

```bash
sudo apt install openjdk-21-jdk maven pcscd libccid
sudo systemctl enable --now pcscd   # Kali'de varsayılan kapalı olabilir
```

AKİS kart sürücüsünü (PKCS#11 kütüphanesi, ör. `libakisp11.so`) TÜBİTAK BİLGEM'den indirip kurun.

## Derleme ve kullanım

```bash
mvn -B verify
java -jar esign-cli/target/esign.jar list-certs --login
# Kütüphane farklı yerdeyse:
java -jar esign-cli/target/esign.jar list-certs --lib /yol/libakisp11.so --login
```

# Word, Excel, PDF veya herhangi bir dosyayı imzala (İmzager gibi bütünleşik .imz):
java -jar esign-cli/target/esign.jar sign --in rapor.docx
# Ayrık imza (orijinal dosya değişmez, rapor.docx.p7s oluşur):
java -jar esign-cli/target/esign.jar sign --in rapor.docx --detached
# PDF → PAdES (sozlesme_imzali.pdf), XML → XAdES (yazi_imzali.xml); biçim uzantıdan seçilir:
java -jar esign-cli/target/esign.jar sign --in sozlesme.pdf --reason "Onaylıyorum"
java -jar esign-cli/target/esign.jar sign --in yazi.xml
# PDF'i İmzager gibi .imz olarak imzalamak için:
java -jar esign-cli/target/esign.jar sign --in sozlesme.pdf --format cades
```

PIN yalnızca terminalden sorulur; `--pin` argümanı bilerek desteklenmez. Var olan çıktı dosyasının üzerine yazılmaz.

İmzayı OpenSSL ile kriptografik olarak kontrol etmek (sertifika zinciri doğrulaması Faz 5'te):

```bash
openssl cms -verify -inform DER -in rapor.docx.imz -noverify -out /dev/null

## Kartsız test (SoftHSM2)

```bash
sudo apt install softhsm2 opensc
eval "$(scripts/softhsm-setup.sh)"
mvn -B verify          # entegrasyon testi de çalışır
```

## Yol haritası

1. ✅ Linux'ta kart erişimi, sertifika listeleme
2. ✅ CAdES-BES imza (`.imz`, `.p7s`)
3. ✅ PAdES ve XAdES (görünür imza alanı sonraki sürümde)
4. RFC 3161 zaman damgası
5. OCSP/CRL, zincir, LT/LTA
6. İmzager karşılıklı uyumluluk testleri, masaüstü arayüz
7. REST API

İmza motoru olarak [EU DSS](https://github.com/esig/dss) (LGPL-2.1) planlanmaktadır.

## Lisans

[Apache-2.0](LICENSE)
