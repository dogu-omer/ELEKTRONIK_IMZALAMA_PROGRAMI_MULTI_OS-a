#!/usr/bin/env bash
# Gömülü JRE ile yerel paket üretir (Java kurulu olmasa da çalışır).
# Builds a native installer with an embedded, jlink-trimmed JRE.
# Kullanım / usage: packaging/jpackage.sh deb|rpm|pkg|dmg|msi|exe|app-image   (önce: mvn -B package)
# Windows'ta Git Bash ile çalışır; msi/exe için WiX 3 (candle.exe/light.exe) PATH'te olmalıdır.
set -euo pipefail

TYPE="${1:?paket türü: deb|rpm|pkg|dmg|msi|exe|app-image}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/esign-cli/target/esign.jar"
OUT="$ROOT/dist"
[[ -f "$JAR" ]] || { echo "Önce derleyin: mvn -B package" >&2; exit 1; }

if command -v unzip >/dev/null 2>&1; then
  VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: *//p' | tr -d '\r')"
else  # Git Bash'te unzip olmayabilir / no unzip on some Windows shells
  VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$ROOT/pom.xml" | head -1)"
fi
VERSION="${VERSION%%-*}"   # 0.2.0-SNAPSHOT -> 0.2.0
APP_VERSION="$VERSION"
if [[ "$(uname -s)" == "Darwin" && "$APP_VERSION" == 0.* ]]; then
  # macOS paketleri 0 ile başlayan sürüm kabul etmez (CFBundleVersion). 0.x.y -> 1.x.y
  APP_VERSION="1.${APP_VERSION#0.}"
fi

# jdeps çıktısı + SunPKCS11 (kart erişimi), karakter setleri ve yerel ayarlar
MODULES="java.base,java.compiler,java.desktop,java.naming,java.security.jgss,java.sql,java.xml.crypto,\
jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.charsets,jdk.localedata,jdk.zipfs,java.net.http,\
java.logging,java.management,jdk.unsupported"

# Windows'ta (Git Bash) jpackage.exe yerel yol ister / native Windows paths for jpackage.exe
p() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi; }

STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
cp "$JAR" "$STAGE/"
cp "$ROOT/LICENSE" "$STAGE/LICENSE"
mkdir -p "$OUT"

# Windows konsolunda Türkçe karakterler için stdout kodlaması konsolun kod sayfasına bırakılır.
JOPTS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
[[ "$TYPE" == msi || "$TYPE" == exe ]] && JOPTS="-Dfile.encoding=UTF-8"

ARGS=(--type "$TYPE" --name esign --app-version "$APP_VERSION"
  --input "$(p "$STAGE")" --main-jar esign.jar --main-class io.github.doguomer.esign.cli.Main
  --add-modules "$MODULES" --jlink-options "--strip-debug --no-man-pages --no-header-files"
  --java-options "$JOPTS"
  --description "E-imza (CAdES/PAdES/XAdES, PKCS#11/AKIS) - e-signature CLI ve masaustu"
  --vendor "Omer Dogu" --copyright "GPL-3.0-or-later" --license-file "$(p "$ROOT/LICENSE")"
  --dest "$(p "$OUT")")

# Masaüstü arayüzü için ikinci başlatıcı (Linux menü kısayolu). macOS'ta uygulama Finder'dan açılınca
# (terminal yoksa) arayüz kendiliğinden başlar. Second launcher for the desktop UI.
cat > "$STAGE/esign-gui.properties" <<PROPS
main-class=io.github.doguomer.esign.desktop.DesktopApp
description=esign - Elektronik Imza (masaustu)
linux-shortcut=true
win-console=false
PROPS

case "$TYPE" in
  deb) ARGS+=(--linux-package-name esign --linux-deb-maintainer "dogu-omer@users.noreply.github.com"
              --linux-app-category utils --linux-package-deps "pcscd" --linux-menu-group "Office;Utility" --linux-shortcut
              --resource-dir "$ROOT/packaging/linux"
              --add-launcher "esign-gui=$STAGE/esign-gui.properties") ;;
  rpm) ARGS+=(--linux-package-name esign --linux-app-category Utilities
              --add-launcher "esign-gui=$STAGE/esign-gui.properties") ;;
  msi|exe) # Ana başlatıcı esign.exe konsol uygulamasıdır (komut satırı); esign-gui.exe penceredir.
           # Kısayollar ve PATH packaging/windows/main.wxs içinde tanımlıdır (jpackage'ın --win-menu seçeneği ana
           # başlatıcıya da kısayol koyardı). Shortcuts + PATH are defined in main.wxs.
           ARGS+=(--verbose --win-console --win-dir-chooser
              --win-upgrade-uuid "83e6b761-5324-41fe-bb3c-9b0039cf21ad"
              --win-help-url "https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a"
              --resource-dir "$(p "$ROOT/packaging/windows")"
              --add-launcher "esign-gui=$(p "$STAGE/esign-gui.properties")") ;;
  pkg|dmg) ARGS+=(--mac-package-identifier io.github.doguomer.esign --mac-package-name esign) ;;
esac
if [[ "$TYPE" == "app-image" ]]; then
  # app-image lisans dosyası seçeneğini kabul etmez
  ARGS=(--type app-image --name esign --app-version "$APP_VERSION" --input "$STAGE" --main-jar esign.jar
    --main-class io.github.doguomer.esign.cli.Main --add-modules "$MODULES"
    --jlink-options "--strip-debug --no-man-pages --no-header-files" --java-options "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
    --dest "$OUT")
fi

jpackage "${ARGS[@]}"
ls -la "$OUT"
