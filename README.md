# ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS

Linux ve macOS üzerinde nitelikli elektronik imza (AKİS akıllı kart / USB token, PKCS#11) ile
CAdES, PAdES ve XAdES imza üretmeyi ve doğrulamayı hedefleyen açık kaynak Java platformu.

*Open-source Java platform for qualified e-signatures (CAdES/PAdES/XAdES) on Linux and macOS via PKCS#11 smart cards.*

> **Durum: Faz 1 / 7.** Şu an yalnızca karttaki sertifikaları listeler; henüz imza atmaz.
> Bu yazılım BTK veya TÜBİTAK tarafından onaylanmış/sertifikalandırılmış değildir.
> Güvenli elektronik imzanın hukuki geçerliliği (5070 sayılı Kanun) nitelikli sertifika ve
> güvenli imza oluşturma aracından gelir; yazılımdan değil.

## Modüller

| Modül | Görev |
| --- | --- |
| `esign-core` | Port arayüzleri (`SigningToken`) ve alan modeli; dış bağımlılığı yok |
| `esign-token-pkcs11` | JDK SunPKCS11 ile kart erişimi (AKİS, SoftHSM, diğer PKCS#11) |
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

PIN yalnızca terminalden sorulur; `--pin` argümanı bilerek desteklenmez.

## Kartsız test (SoftHSM2)

```bash
sudo apt install softhsm2 opensc
eval "$(scripts/softhsm-setup.sh)"
mvn -B verify          # entegrasyon testi de çalışır
```

## Yol haritası

1. ✅ Linux'ta kart erişimi, sertifika listeleme
2. CAdES-BES imza (`.imz`)
3. PAdES ve XAdES
4. RFC 3161 zaman damgası
5. OCSP/CRL, zincir, LT/LTA
6. İmzager karşılıklı uyumluluk testleri, masaüstü arayüz
7. REST API

İmza motoru olarak [EU DSS](https://github.com/esig/dss) (LGPL-2.1) planlanmaktadır.

## Lisans

[Apache-2.0](LICENSE)
