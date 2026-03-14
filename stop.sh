#!/usr/bin/env bash
set -euo pipefail

PIDS="$(pgrep -f 'modern-bot-.*jar-with-dependencies.jar|DiscordModernMusicBot.jar' || true)"
if [[ -z "$PIDS" ]]; then
  echo "ModernMusicBot is not running."
  exit 0
fi

echo "Stopping ModernMusicBot..."
kill $PIDS || true
sleep 1

REMAINING="$(pgrep -f 'modern-bot-.*jar-with-dependencies.jar|DiscordModernMusicBot.jar' || true)"
if [[ -n "$REMAINING" ]]; then
  kill -9 $REMAINING || true
fi

echo "ModernMusicBot stopped."
