#!/bin/zsh
set -e
cd "$(dirname "$0")"

JAVA25_HOME="$HOME/.jdks/jdk-25.0.2+10/Contents/Home"
if [[ -x "$JAVA25_HOME/bin/java" ]]; then
  export JAVA_HOME="$JAVA25_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

mvn -q -DskipTests package

JAR_PATH=$(find target -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1)
if [[ -z "$JAR_PATH" ]]; then
  echo "Built jar not found in target/"
  read -r "reply?Press Enter to close..."
  exit 1
fi

"$JAVA_HOME/bin/java" -cp "$JAR_PATH" com.artem.musicbot.ControlPanelApp
