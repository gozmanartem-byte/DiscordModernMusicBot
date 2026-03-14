#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$ROOT_DIR/dist/releases"
STAGING_DIR="$ROOT_DIR/dist/dmg-staging"
APP_NAME="ModernMusicBot"
VERSION="${RELEASE_VERSION:-}"
JAVA25_HOME_DEFAULT="$HOME/.jdks/jdk-25.0.2+10/Contents/Home"
DMG_BACKGROUND="$ROOT_DIR/release-assets/macos/dmg-background.png"
DMG_VOLUME_NAME="ModernMusicBot"
DMG_WINDOW_BOUNDS="200,120,1020,660"
DMG_ICON_SIZE=104
DMG_APP_POS="260,330"
DMG_APPS_POS="740,330"

normalize_version() {
  local candidate="$1"
  if [[ "$candidate" == v* ]]; then
    candidate="${candidate#v}"
  fi

  if [[ "$candidate" =~ ^[0-9]+(\.[0-9]+){1,3}([.-][0-9A-Za-z]+)?$ ]]; then
    echo "$candidate"
    return 0
  fi

  return 1
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

if [[ -n "$VERSION" ]]; then
  VERSION="$(normalize_version "$VERSION" || true)"
fi

if [[ -z "$VERSION" && -n "${GITHUB_REF_NAME:-}" ]]; then
  VERSION="$(normalize_version "$GITHUB_REF_NAME" || true)"
fi

if [[ -z "$VERSION" ]]; then
  VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1)"
  VERSION="$(normalize_version "$VERSION" || true)"
fi

if [[ -z "$VERSION" ]]; then
  echo "Could not resolve a valid semantic release version." >&2
  exit 1
fi

CF_BUNDLE_VERSION="${VERSION%%[-+]*}"

if [[ -z "${JAVA_HOME:-}" && -x "$JAVA25_HOME_DEFAULT/bin/java" ]]; then
  export JAVA_HOME="$JAVA25_HOME_DEFAULT"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

require_command mvn
require_command hdiutil

echo "Building macOS DMG for version $VERSION..."

# Build the macos package
rm -rf "$ROOT_DIR/target"
mvn -q -DskipTests package -Pmacos

# Find the built jar
built_jar="$(find "$ROOT_DIR/target" -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1)"
if [[ -z "$built_jar" ]]; then
  echo "Built jar not found" >&2
  exit 1
fi

# Setup DMG staging directory
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

# Create .app bundle structure
APP_BUNDLE="$STAGING_DIR/$APP_NAME.app"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"
mkdir -p "$APP_BUNDLE/Contents/Java"

# Add Applications link so users can drag-and-drop install from Finder.
ln -s /Applications "$STAGING_DIR/Applications"

# Copy jar to app bundle
cp "$built_jar" "$APP_BUNDLE/Contents/Java/DiscordModernMusicBot.jar"

# Copy icon
cp "$ROOT_DIR/src/main/resources/icon.icns" "$APP_BUNDLE/Contents/Resources/"

# Create launcher script
cat > "$APP_BUNDLE/Contents/MacOS/launcher.sh" << 'LAUNCHER_EOF'
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA25_HOME="$HOME/.jdks/jdk-25.0.2+10/Contents/Home"
APP_HOME="$HOME/.modernmusicbot"
CONFIG_FILE="$APP_HOME/ModernMusicBot.properties"
LOG_FILE="$APP_HOME/launcher.log"

mkdir -p "$APP_HOME"
touch "$LOG_FILE"
exec >>"$LOG_FILE" 2>&1
echo "[$(date '+%Y-%m-%d %H:%M:%S')] launcher start"

if [[ -x "$JAVA25_HOME/bin/java" ]]; then
  JAVA_BIN="$JAVA25_HOME/bin/java"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="$(command -v java || true)"
fi

if [[ -z "$JAVA_BIN" ]]; then
  osascript -e 'tell app "System Events" to display dialog "Java not found. Please install Java 25." buttons {"OK"} default button 1'
  exit 1
fi

cd "$APP_HOME"
"$JAVA_BIN" --enable-native-access=ALL-UNNAMED -jar "$DIR/../Java/DiscordModernMusicBot.jar" "$CONFIG_FILE"
exit_code=$?

if [[ $exit_code -ne 0 ]]; then
  osascript -e 'tell app "System Events" to display dialog "ModernMusicBot failed to start. Check ~/.modernmusicbot/launcher.log" buttons {"OK"} default button 1'
fi

exit $exit_code
LAUNCHER_EOF

chmod +x "$APP_BUNDLE/Contents/MacOS/launcher.sh"

# Create Info.plist
cat > "$APP_BUNDLE/Contents/Info.plist" << PLIST_EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>launcher.sh</string>
    <key>CFBundleIconFile</key>
    <string>icon</string>
    <key>CFBundleIdentifier</key>
    <string>com.artem.musicbot</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>ModernMusicBot</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${CF_BUNDLE_VERSION}</string>
    <key>CFBundleVersion</key>
    <string>${CF_BUNDLE_VERSION}</string>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST_EOF

# Create DMG
mkdir -p "$DIST_DIR"
dmg_path="$DIST_DIR/DiscordModernMusicBot-v${VERSION}-macos.dmg"

echo "Creating DMG at $dmg_path..."

# Remove existing DMG if it exists
rm -f "$dmg_path"

# Create temporary DMG
temp_dmg="/tmp/DiscordModernMusicBot-temp.dmg"
rm -f "$temp_dmg"

# Increase size for DMG (read-write so we can theme it)
hdiutil create -volname "$DMG_VOLUME_NAME" \
  -srcfolder "$STAGING_DIR" \
  -ov -format UDRW "$temp_dmg" -size 250m

mount_dir=""
if [[ -f "$DMG_BACKGROUND" ]]; then
  mount_dir="$(hdiutil attach -readwrite -noverify -noautoopen "$temp_dmg" | awk '/\/Volumes/ {print $3; exit}')"
  if [[ -n "$mount_dir" ]]; then
    mkdir -p "$mount_dir/.background"
    cp "$DMG_BACKGROUND" "$mount_dir/.background/dmg-background.png"
    chflags hidden "$mount_dir/.background" || true

    /usr/bin/osascript << EOF
tell application "Finder"
  tell disk "$DMG_VOLUME_NAME"
    open
    set current view of container window to icon view
    set toolbar visible of container window to false
    set statusbar visible of container window to false
    set the bounds of container window to {$DMG_WINDOW_BOUNDS}
    set viewOptions to the icon view options of container window
    set arrangement of viewOptions to not arranged
    set icon size of viewOptions to $DMG_ICON_SIZE
    set background picture of viewOptions to file ".background:dmg-background.png"
    set position of item "$APP_NAME.app" of container window to {$DMG_APP_POS}
    set position of item "Applications" of container window to {$DMG_APPS_POS}
    update without registering applications
    delay 1
    close
    open
    delay 1
  end tell
end tell
EOF
    hdiutil detach "$mount_dir" -quiet
  fi
fi

# Convert to compressed format
hdiutil convert "$temp_dmg" -format UDZO -o "$dmg_path"
rm -f "$temp_dmg"

# Cleanup
rm -rf "$STAGING_DIR"

echo "✓ DMG created: $dmg_path"
ls -lh "$dmg_path"
