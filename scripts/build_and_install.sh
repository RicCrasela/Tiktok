#!/usr/bin/env bash
set -euo pipefail

# Build and install APK on a connected Android device via adb.
# Also supports building an AAB for Play Store.
#
# Usage:
#   ./scripts/build_and_install.sh [--package com.example.app] [--variant debug|release] [--skip-clean] [--aab]
#
# Examples:
#   ./scripts/build_and_install.sh
#   ./scripts/build_and_install.sh --package com.my.app --variant debug
#   ./scripts/build_and_install.sh --variant release
#   ./scripts/build_and_install.sh --aab --variant release
#
# Prerequisites:
# - JDK installed
# - ANDROID_HOME / ANDROID_SDK_ROOT configured
# - adb available in PATH
# - A device connected with USB debugging enabled or an emulator running (for APK install)

PKG=""
VARIANT="debug"
DO_CLEAN=1
BUILD_AAB=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package)
      PKG="$2"; shift 2;;
    --variant)
      VARIANT="$2"; shift 2;;
    --skip-clean)
      DO_CLEAN=0; shift 1;;
    --aab)
      BUILD_AAB=1; shift 1;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Auto-detect package from AndroidManifest.xml if not provided
if [[ -z "$PKG" ]]; then
  if [[ -f "app/src/main/AndroidManifest.xml" ]]; then
    PKG="$(grep -oP '(?<=package=\")[^\"]+' app/src/main/AndroidManifest.xml | head -n1 || true)"
  fi
  if [[ -z "$PKG" ]]; then
    echo "Warning: Could not auto-detect packageName. Falling back to com.bytedance.tiktok"
    PKG="com.bytedance.tiktok"
  fi
fi

if [[ "$DO_CLEAN" == "1" ]]; then
  echo "[1/5] Cleaning..."
  ./gradlew clean
else
  echo "[1/5] Skipping clean."
fi

if [[ "$BUILD_AAB" == "1" ]]; then
  echo "[2/5] Building App Bundle (AAB) for $VARIANT ..."
  ./gradlew :app:bundle$(tr '[:lower:]' '[:upper:]' <<< ${VARIANT:0:1})${VARIANT:1}
  AAB_PATH="app/build/outputs/bundle/${VARIANT}/app-${VARIANT}.aab"
  if [[ ! -f "$AAB_PATH" ]]; then
    echo "Error: AAB not found at $AAB_PATH"
    exit 1
  fi
  echo "AAB built at: $AAB_PATH"
else
  echo "[2/5] Skipping AAB build."
fi

if [[ "$VARIANT" == "release" ]]; then
  echo "[3/5] Building release APK..."
  ./gradlew :app:assembleRelease
  APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
  echo "[3/5] Building debug APK..."
  ./gradlew :app:assembleDebug
  APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "Error: APK not found at $APK_PATH"
  exit 1
fi

echo "[4/5] Installing APK to device..."
adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -q . || {
  echo "No connected device found. Start an emulator or connect a device, then retry."
  exit 1
}

adb install -r "$APK_PATH"

echo "[5/5] Launching app ($PKG)..."
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1

echo "Success.
APK: $APK_PATH"
if [[ "$BUILD_AAB" == "1" ]]; then
  echo "AAB: $AAB_PATH"
fi