#!/bin/zsh
set -e
cd "$(dirname "$0")"
source "$HOME/.zshrc"
JAVA25_HOME="$HOME/.jdks/jdk-25.0.2+10/Contents/Home"
CONFIG_FILE="ModernMusicBot.properties"
LEGACY_CONFIG_FILE="application.properties"
if [[ ! -f "$CONFIG_FILE" && -f "$LEGACY_CONFIG_FILE" ]]; then
  echo "Migrating $LEGACY_CONFIG_FILE to $CONFIG_FILE"
  mv "$LEGACY_CONFIG_FILE" "$CONFIG_FILE"
fi
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing $CONFIG_FILE"
  read -r "reply?Press Enter to close..."
  exit 1
fi
if [[ ! -x "$JAVA25_HOME/bin/java" ]]; then
  echo "Java 25 not found at $JAVA25_HOME"
  read -r "reply?Press Enter to close..."
  exit 1
fi
export JAVA_HOME="$JAVA25_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q -DskipTests package
echo "Using config: $CONFIG_FILE"
"$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar target/modern-bot-1.0.0-jar-with-dependencies.jar "$CONFIG_FILE"
read -r "reply?Bot stopped. Press Enter to close..."
