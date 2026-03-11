# DiscordModernMusicBot

![Java 25](https://img.shields.io/badge/Java-25-blue)
![Maven 3.9+](https://img.shields.io/badge/Maven-3.9%2B-C71A36)
![Platform macOS](https://img.shields.io/badge/Platform-macOS-lightgrey)

Modern Java Discord music bot built with JDA, Lavaplayer, youtube-source, and JDave voice encryption support.

Simple goal: run a stable Discord music bot locally with one config file and launcher scripts.

## Features

- Plays music from URL or search query
- Queue management (`play`, `skip`, `stop`, `leave`)
- Audio controls (`volume`, `bass`, `loud`, `normal`)
- In-chat player panel (`player`) with control buttons
- Configurable bot language (`bot.language`)
- Debug command for audio diagnostics (`debugaudio`)
- Launcher scripts for macOS, Linux, and Windows

## Requirements

- Java 25
- Maven 3.9+
- Discord bot token

## Beginner Setup (Step By Step)

If you are not technical, follow these steps exactly.

### 1. Create Discord Bot Token

1. Open `https://discord.com/developers/applications`
2. Click `New Application`
3. Enter any app name and click `Create`
4. Open the `Bot` tab
5. Click `Reset Token` (or `Add Bot` first if needed)
6. Copy the token and keep it private

### 2. Enable Required Intents

In the same `Bot` tab, enable:

- `MESSAGE CONTENT INTENT`
- `SERVER MEMBERS INTENT`

### 3. Invite Bot To Your Server

1. Open `OAuth2` -> `URL Generator`
2. Select scope: `bot`
3. Select permissions: `View Channels`, `Send Messages`, `Connect`, `Speak`
4. Open generated link and invite the bot

## Quick Start

1. Download this project (or a release zip) and open the folder.
2. In project root, copy config template:

```bash
cp ModernMusicBot.properties.example ModernMusicBot.properties
```

3. Open `ModernMusicBot.properties` and set your token:

```properties
bot.token=YOUR_DISCORD_BOT_TOKEN
bot.prefix=!
bot.language=en
```

### Easiest Option (Control Panel)

Use the desktop-like control panel with Start/Stop buttons and built-in settings form.

macOS:

```bash
./control-panel.command
```

Linux:

```bash
./control-panel.sh
```

Windows:

```bat
control-panel.bat
```

4. Start bot:

```bash
./start.command
```

Linux:

```bash
./start.sh
```

Windows:

```bat
start.bat
```

5. Stop bot:

```bash
./stop.command
```

Linux:

```bash
./stop.sh
```

Windows:

```bat
stop.bat
```

## Release Packages

To build downloadable release zips for macOS, Linux, and Windows:

```bash
./build-release-packages.sh
```

This creates zip files in `dist/releases/` that you can upload to the GitHub release page as assets.

Current packaged targets:

- macOS
- Linux x86-64
- Linux ARM64
- Windows x86-64

## Commands

- `!play <url or search>`
- `!skip`
- `!pause`
- `!resume`
- `!stop`
- `!leave`
- `!volume`
- `!volume <0-200>`
- `!bass`
- `!bass <0-5>`
- `!loud`
- `!normal`
- `!queue`
- `!player`
- `!debugaudio`
- `!help`

Supported language codes for `bot.language`:

- `en` (English)
- `ru` (Russian)
- `hy` (Armenian)
- `ka` (Georgian)
- `az` (Azerbaijani)
- `kk` (Kazakh)
- `uz` (Uzbek)
- `uk` (Ukrainian)
- `de` (German)
- `es` (Spanish)
- `it` (Italian)
- `pt` (Portuguese)
- `zh` (Chinese)
- `ja` (Japanese)

## Security

- Never commit `ModernMusicBot.properties` with a real token.
- If a token is exposed, rotate it immediately in Discord Developer Portal.
- Never paste your token in chat, screenshots, or public posts.

## Contributing

Contributions are welcome.

- Fork the repository
- Create a feature branch
- Commit your changes with clear messages
- Open a pull request with a short description and test steps

Please avoid committing secrets, local config files, or generated artifacts.

## License

This project is licensed under the MIT License. See `LICENSE`.

## More Details

See `README_INSTALLATION.md` for full installation and Discord app setup steps.
