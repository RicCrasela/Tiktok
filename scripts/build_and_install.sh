#!/usr/bin/env bash
set -euo pipefail

# Build and install APK on a connected Android device via adb.
# Usage:
#   ./scripts/build_and_install.sh [--package com.example.app] [--variant debug|release] [--skip-clean]
#
# Examples:
#   ./scripts/build_and_install.sh
#   ./scripts/build_and_install.sh --package com.my.app --variant debug
#   ./scripts/build_and_install.sh --variant release
#
# Prerequisites:
# - JDK installed
# - ANDROID_HOME / ANDROID_SDK_ROOT configured
# - adb available in PATH
# - A device connected with USB debugging enabled or an emulator running

PKG="com.bytedance.tiktok"
VARIANT="debug"
DO_CLEAN=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package)
      PKG="$2"; shift 2;;
    --variant)
      VARIANT="$2"; shift 2;;
    --skip-clean)
      DO_CLEAN=0; shift 1;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$DO_CLEAN" == "1" ]]; then
  echo "[1/4] Cleaning..."
  ./gradlew clean
else
  echo "[1/4] Skipping clean."
fi

if [[ "$VARIANT" == "release" ]]; then
  echo "[2/4] Building release APK..."
  ./gradlew :app:assembleRelease
  APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
  echo "[2/4] Building debug APK..."
  ./gradlew :app:assembleDebug
  APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "Error: APK not found at $APK_PATH"
  exit 1
fi

echo "[3/4] Installing APK to device..."
adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -q . || {
  echo "No connected device found. Start an emulator or connect a device, then retry."
  exit 1
}

adb install -r "$APK_PATH"

echo "[4/4] Launching app ($PKG)..."
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1

echo "Success."