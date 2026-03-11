#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

CONFIG_FILE="ModernMusicBot.properties"

if [[ ! -f "$CONFIG_FILE" && -f "application.properties" ]]; then
  echo "Migrating application.properties to $CONFIG_FILE"
  mv application.properties "$CONFIG_FILE"
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing $CONFIG_FILE"
  echo "Copy ModernMusicBot.properties.example to $CONFIG_FILE and set your bot token."
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="$(command -v java || true)"
fi

if [[ -z "$JAVA_BIN" ]]; then
  echo "Java not found. Install Java 25 and try again."
  exit 1
fi

mvn -q -DskipTests package

JAR_PATH="$(find target -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "Built jar not found in target/"
  exit 1
fi

echo "Using config: $CONFIG_FILE"
exec "$JAVA_BIN" --enable-native-access=ALL-UNNAMED -jar "$JAR_PATH" "$CONFIG_FILE"
