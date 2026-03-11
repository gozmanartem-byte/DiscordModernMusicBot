#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -DskipTests package

JAR_PATH="$(find target -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "Built jar not found in target/"
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

exec "$JAVA_BIN" -cp "$JAR_PATH" com.artem.musicbot.ControlPanelApp
