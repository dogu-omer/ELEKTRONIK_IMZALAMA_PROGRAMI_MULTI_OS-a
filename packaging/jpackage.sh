#!/usr/bin/env bash
# Gömülü JRE ile yerel paket üretir (Java kurulu olmasa da çalışır).
# Builds a native installer with an embedded, jlink-trimmed JRE.
# Kullanım / usage: packaging/jpackage.sh deb|rpm|pkg|dmg|app-image   (önce: mvn -B package)
set -euo pipefail

TYPE="${1:?paket türü: deb|rpm|pkg|dmg|app-image}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/esign-cli/target/esign.jar"
OUT="$ROOT/dist"
[[ -f "$JAR" ]] || { echo "Önce derleyin: mvn -B package" >&2; exit 1; }

VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: *//p' | tr -d '\r')"
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

STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
cp "$JAR" "$STAGE/"
cp "$ROOT/LICENSE" "$STAGE/LICENSE"
mkdir -p "$OUT"

ARGS=(--type "$TYPE" --name esign --app-version "$APP_VERSION"
  --input "$STAGE" --main-jar esign.jar --main-class io.github.doguomer.esign.cli.Main
  --add-modules "$MODULES" --jlink-options "--strip-debug --no-man-pages --no-header-files"
  --java-options "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
  --description "E-imza (CAdES/PAdES/XAdES, PKCS#11/AKIS) - e-signature CLI ve masaustu"
  --vendor "Omer Dogu" --copyright "GPL-3.0-or-later" --license-file "$ROOT/LICENSE"
  --dest "$OUT")

# Masaüstü arayüzü için ikinci başlatıcı (Linux menü kısayolu). macOS'ta uygulama Finder'dan açılınca
# (terminal yoksa) arayüz kendiliğinden başlar. Second launcher for the desktop UI.
cat > "$STAGE/esign-gui.properties" <<PROPS
main-class=io.github.doguomer.esign.desktop.DesktopApp
description=esign - Elektronik Imza (masaustu)
linux-shortcut=true
PROPS

case "$TYPE" in
  deb) ARGS+=(--linux-package-name esign --linux-deb-maintainer "dogu-omer@users.noreply.github.com"
              --linux-app-category utils --linux-package-deps "pcscd" --linux-menu-group "Office;Utility" --linux-shortcut
              --add-launcher "esign-gui=$STAGE/esign-gui.properties") ;;
  rpm) ARGS+=(--linux-package-name esign --linux-app-category Utilities
              --add-launcher "esign-gui=$STAGE/esign-gui.properties") ;;
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
