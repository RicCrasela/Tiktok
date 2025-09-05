#!/usr/bin/env bash
set -euo pipefail

# Build and install debug APK on a connected Android device via adb.
# Usage:
#   ./scripts/build_and_install.sh
#
# Prerequisites:
# - JDK installed
# - ANDROID_HOME / ANDROID_SDK_ROOT configured
# - adb available in PATH
# - A device connected with USB debugging enabled or an emulator running

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

echo "[1/3] Cleaning..."
./gradlew clean

echo "[2/3] Building debug APK..."
./gradlew :app:assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK_PATH" ]]; then
  echo "Error: APK not found at $APK_PATH"
  exit 1
fi

echo "[3/3] Installing APK to device..."
adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -q . || {
  echo "No connected device found. Start an emulator or connect a device, then retry."
  exit 1
}

adb install -r "$APK_PATH"

echo "Done. Launching app..."
# Try to launch the main activity
adb shell monkey -p com.bytedance.tiktok -c android.intent.category.LAUNCHER 1

echo "Success."