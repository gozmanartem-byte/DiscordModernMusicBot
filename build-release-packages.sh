#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$ROOT_DIR/dist/releases"
STAGING_DIR="$ROOT_DIR/dist/staging"
VERSION="${RELEASE_VERSION:-}"
JAVA25_HOME_DEFAULT="$HOME/.jdks/jdk-25.0.2+10/Contents/Home"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

copy_common_files() {
  local target_dir="$1"
  cp "$ROOT_DIR/ModernMusicBot.properties.example" "$target_dir/"
  cp "$ROOT_DIR/README.md" "$target_dir/"
  cp "$ROOT_DIR/README_INSTALLATION.md" "$target_dir/"
  cp "$ROOT_DIR/LICENSE" "$target_dir/"
}

build_package() {
  local profile="$1"
  local platform_dir="$2"
  local asset_name="$3"

  echo "Building $asset_name"
  rm -rf "$ROOT_DIR/target"
  mvn -q -DskipTests package -P"$profile"

  local built_jar
  built_jar="$(find "$ROOT_DIR/target" -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1)"
  if [[ -z "$built_jar" ]]; then
    echo "Built jar not found for profile $profile" >&2
    exit 1
  fi

  local package_dir="$STAGING_DIR/$asset_name"
  rm -rf "$package_dir"
  mkdir -p "$package_dir"

  cp "$built_jar" "$package_dir/DiscordModernMusicBot.jar"
  copy_common_files "$package_dir"
  cp "$ROOT_DIR/release-assets/$platform_dir"/* "$package_dir/"

  if [[ "$platform_dir" != "windows" ]]; then
    chmod +x "$package_dir"/* || true
  fi

  (
    cd "$STAGING_DIR"
    zip -qr "$DIST_DIR/$asset_name.zip" "$asset_name"
  )
}

require_command mvn
require_command zip

if [[ -z "$VERSION" && -n "${GITHUB_REF_NAME:-}" ]]; then
  VERSION="$GITHUB_REF_NAME"
fi

if [[ "$VERSION" == v* ]]; then
  VERSION="${VERSION#v}"
fi

if [[ -z "$VERSION" ]]; then
  VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1)"
fi

if [[ -z "${JAVA_HOME:-}" && -x "$JAVA25_HOME_DEFAULT/bin/java" ]]; then
  export JAVA_HOME="$JAVA25_HOME_DEFAULT"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

rm -rf "$DIST_DIR" "$STAGING_DIR"
mkdir -p "$DIST_DIR" "$STAGING_DIR"

build_package "macos" "macos" "DiscordModernMusicBot-v${VERSION}-macos"
build_package "linux-x86-64" "linux" "DiscordModernMusicBot-v${VERSION}-linux-x86-64"
build_package "linux-aarch64" "linux" "DiscordModernMusicBot-v${VERSION}-linux-aarch64"
build_package "windows-x86-64" "windows" "DiscordModernMusicBot-v${VERSION}-windows-x86-64"

echo
echo "Release packages created in: $DIST_DIR"
ls -1 "$DIST_DIR"