#!/usr/bin/env bash
# Gerçek kart olmadan test için SoftHSM2 üzerinde sahte bir "imza kartı" kurar.
# Sets up a throwaway SoftHSM2 token with a nonRepudiation test certificate.
# Gereken paketler (Debian/Ubuntu/Kali): softhsm2 opensc openssl
# Kullanım: source <(scripts/softhsm-setup.sh)   -> ortam değişkenlerini yazdırır
set -euo pipefail

WORK="${ESIGN_SOFTHSM_DIR:-$(mktemp -d)}"
PIN="${ESIGN_TEST_PIN:-$(od -An -N4 -tu4 /dev/urandom | tr -d ' ' | cut -c1-6)}"
SO_PIN="$(od -An -N4 -tu4 /dev/urandom | tr -d ' ')"
LIB="${SOFTHSM_LIB:-/usr/lib/softhsm/libsofthsm2.so}"

mkdir -p "$WORK/tokens"
cat > "$WORK/softhsm2.conf" <<CONF
directories.tokendir = $WORK/tokens
objectstore.backend = file
log.level = ERROR
CONF
export SOFTHSM2_CONF="$WORK/softhsm2.conf"

softhsm2-util --init-token --free --label esign-test --pin "$PIN" --so-pin "$SO_PIN" >/dev/null

openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
  -subj "/C=TR/CN=TEST IMZACI (SAHTE)" \
  -addext "keyUsage=critical,nonRepudiation" \
  -keyout "$WORK/key.pem" -out "$WORK/cert.pem" 2>/dev/null
openssl pkcs8 -topk8 -nocrypt -in "$WORK/key.pem" -outform DER -out "$WORK/key.der"
openssl x509 -in "$WORK/cert.pem" -outform DER -out "$WORK/cert.der"

for obj in "key.der privkey" "cert.der cert"; do
  set -- $obj
  pkcs11-tool --module "$LIB" --login --pin "$PIN" --write-object "$WORK/$1" \
    --type "$2" --id 01 --label imza >/dev/null 2>&1
done
rm -f "$WORK/key.pem" "$WORK/key.der"   # özel anahtar yalnızca token içinde kalsın

echo "export SOFTHSM2_CONF='$SOFTHSM2_CONF'"
echo "export ESIGN_TEST_PKCS11_LIB='$LIB'"
echo "export ESIGN_TEST_PIN='$PIN'"
