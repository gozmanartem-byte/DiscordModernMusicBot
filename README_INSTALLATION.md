# ModernMusicBot Installation Guide

This guide is for first-time setup and running the bot locally.

## 1. Requirements

- macOS (this project currently includes macOS launcher scripts)
- Java 25
- Maven 3.9+
- Discord bot token

## 2. Create a Discord Bot

1. Open Discord Developer Portal.
2. Create an application.
3. Open `Bot` tab and click `Add Bot`.
4. Enable intents:
   - `MESSAGE CONTENT INTENT`
   - `SERVER MEMBERS INTENT` (recommended)
5. Copy bot token.

## 3. Invite Bot to Server

1. Open `OAuth2` -> `URL Generator`.
2. Select scope: `bot`.
3. Select permissions:
   - View Channels
   - Send Messages
   - Connect
   - Speak
4. Open generated URL and invite bot.

## 4. Configure Project

1. In `ModernMusicBot`, copy:
   - `ModernMusicBot.properties.example` -> `ModernMusicBot.properties`
2. Set token in `ModernMusicBot.properties`:

```properties
bot.token=YOUR_DISCORD_BOT_TOKEN
bot.prefix=!
```

## 5. Run

- Start: `start.command`
- Stop: `stop.command`

`start.command` will build and run the jar automatically.

## 6. Commands

- `!play <url or search>`
- `!skip`
- `!stop`
- `!leave`
- `!volume`
- `!volume <0-200>`
- `!bass`
- `!bass <0-5>`
- `!loud`
- `!normal`
- `!queue`
- `!debugaudio`
- `!help`

## Security Notes

- Do not commit `ModernMusicBot.properties` with real token.
- If a token was exposed, rotate it in Developer Portal immediately.
