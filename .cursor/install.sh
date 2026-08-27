#!/usr/bin/env bash
# Idempotent Cloud Agent setup for the Pulse ACR Android project.
# Installs the Android SDK components the Gradle build needs and points
# Gradle at them, then warms the build so dependencies are cached.
set -euo pipefail

# Repo root is the current working directory when Cursor runs the install phase.
REPO_ROOT="$(pwd)"

# Keep the SDK outside the repo so it survives checkouts and lands in the snapshot.
ANDROID_SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_DIR"
export ANDROID_SDK_ROOT="$ANDROID_SDK_DIR"

# Pinned command-line tools build (sdkmanager 22.0).
CMDLINE_TOOLS_VERSION="15859902"

# SDK packages required by app/build.gradle.kts and helper/build.gradle.kts
# (compileSdk 36.1 via AGP 9's release(36){ minorApiLevel = 1 }, build-tools 36.1.0).
SDK_PACKAGES=(
  "platform-tools"
  "platforms;android-36.1"
  "build-tools;36.1.0"
)

SDKMANAGER="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
  echo "==> Installing Android command-line tools ($CMDLINE_TOOLS_VERSION)"
  mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
  tmp_zip="$(mktemp --suffix=.zip)"
  curl -fsSL -o "$tmp_zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest" "$ANDROID_SDK_DIR/cmdline-tools/_tmp"
  unzip -q "$tmp_zip" -d "$ANDROID_SDK_DIR/cmdline-tools/_tmp"
  mv "$ANDROID_SDK_DIR/cmdline-tools/_tmp/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
  rmdir "$ANDROID_SDK_DIR/cmdline-tools/_tmp" 2>/dev/null || true
  rm -f "$tmp_zip"
fi

echo "==> Accepting SDK licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

echo "==> Installing SDK packages: ${SDK_PACKAGES[*]}"
"$SDKMANAGER" --install "${SDK_PACKAGES[@]}" >/dev/null

echo "==> Writing local.properties"
printf 'sdk.dir=%s\n' "$ANDROID_SDK_DIR" > "$REPO_ROOT/local.properties"

echo "==> Warming Gradle build (assembleDebug for both modules)"
(cd "$REPO_ROOT" && ./gradlew --no-daemon :app:assembleDebug :helper:assembleDebug)

echo "==> Android environment ready. SDK at $ANDROID_SDK_DIR"
