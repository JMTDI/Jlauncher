#!/usr/bin/env bash
# Builds and signs JLauncher without Gradle.
# Needs: JDK 8+ (javac), aapt, dx (dalvik-exchange), zipalign, apksigner, and an android.jar (API 34).
# Signing comes from environment variables so no secrets live in this folder:
#   KEYSTORE=/path/to/file.jks  KS_PASS=...  KEY_ALIAS=...  KEY_PASS=...
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR="${ANDROID_JAR:-android.jar}"
DX="${DX:-/usr/lib/android-sdk/build-tools/debian/dx}"
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/gen"

echo "[1/6] resources + manifest"
aapt package -f -m -J "$OUT/gen" -M app/AndroidManifest.xml -S app/res \
     -I "$ANDROID_JAR" --min-sdk-version 26 --target-sdk-version 34 \
     --version-code 5 --version-name 1.4 -F "$OUT/unsigned-res.apk"

echo "[2/6] compile"
# No lambdas: dx cannot desugar them. Java 8 bytecode only.
javac --release 8 -Xlint:-options -cp "$ANDROID_JAR" -d "$OUT/classes" \
      $(find app/src "$OUT/gen" -name '*.java')

echo "[3/6] dex"
"$DX" --dex --min-sdk-version=26 --output="$OUT/classes.dex" "$OUT/classes"

echo "[4/6] package"
cp "$OUT/unsigned-res.apk" "$OUT/unaligned.apk"
( cd "$OUT" && aapt add -k unaligned.apk classes.dex >/dev/null )

echo "[5/6] align"
zipalign -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"

echo "[6/6] sign"
apksigner sign --ks "${KEYSTORE:?set KEYSTORE}" --ks-pass "pass:${KS_PASS:?set KS_PASS}" \
         --ks-key-alias "${KEY_ALIAS:?set KEY_ALIAS}" --key-pass "pass:${KEY_PASS:-$KS_PASS}" \
         --out "$OUT/JLauncher.apk" "$OUT/aligned.apk"
apksigner verify --verbose "$OUT/JLauncher.apk"
echo "Built $OUT/JLauncher.apk"
