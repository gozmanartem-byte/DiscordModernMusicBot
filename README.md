# DiscordModernMusicBot

![Version](https://img.shields.io/badge/Version-2.0.0-brightgreen)
![Java 25](https://img.shields.io/badge/Java-25-blue)
![Maven 3.9+](https://img.shields.io/badge/Maven-3.9%2B-C71A36)
![Platform](https://img.shields.io/badge/Platform-macOS%20%7C%20Windows%20%7C%20Linux-lightgrey)

Modern Java Discord music bot built with JDA, Lavaplayer, youtube-source, and JDave voice encryption support (including E2EE/DAVE).

**As of v2.0.0, ModernMusicBot is a native desktop app.** Download the installer for your platform, install it like any other program, enter your bot token, and start the bot — no Java, no terminal, no config files to hunt down.

## Features

- Plays music from URL or search query
- Queue management (`play`, `skip`, `stop`, `leave`)
- Audio controls (`volume`, `bass`, `loud`, `normal`)
- In-chat player panel (`player`) with control buttons
- Slash commands for major actions (`/play`, `/skip`, `/player`, etc.)
- Persistent per-server settings (prefix, language, DJ role, autoplay, command channel, blocked role)
- DJ/admin permission checks for sensitive commands
- Configurable bot language (`bot.language`)
- Debug command for audio diagnostics (`debugaudio`)
- Local web dashboard (`/`) and JSON metrics endpoint (`/metrics`) on localhost

## Supported Platforms

| Platform | Installer | Portable ZIP |
|---|---|---|
| macOS (Apple Silicon) | `.dmg` | ✓ |
| macOS (Intel) | `.dmg` | ✓ |
| Windows x86-64 | `.exe` | ✓ |
| Linux x86-64 | `.appimage` | ✓ |
| Linux ARM64 | `.appimage` | ✓ |

> Windows ARM is not supported.

## Which Version Should I Use?

**v2.0.0 (this release)** is recommended for everyone. It's a proper desktop app with native installers — no Java setup, no terminal.

**v1.0.0** is there if you need the raw, script-only version — for example, if you're running the bot headlessly on a server, deploying via Docker/CI, or just prefer full manual control with no bundled runtime. You can find it on the [Releases](../../releases) page.

## Getting Started (Recommended — Native Installer)

### Step 1: Create a Discord Bot Token

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **New Application**, give it a name, and click **Create**
3. Open the **Bot** tab
4. Click **Reset Token** and copy it — keep it private

### Step 2: Enable Required Intents

In the **Bot** tab, enable:

- `MESSAGE CONTENT INTENT`
- `SERVER MEMBERS INTENT`

### Step 3: Invite the Bot to Your Server

1. Go to **OAuth2 → URL Generator**
2. Select scope: `bot`
3. Select permissions: `View Channels`, `Send Messages`, `Connect`, `Speak`
4. Open the generated link and invite the bot

### Step 4: Install & Launch

1. Download the installer for your platform from the [Releases](../../releases) page
2. Install it like any normal application (`.dmg` on macOS, `.exe` on Windows)
3. Open **ModernMusicBot** — the control panel will appear
4. Enter your bot token in the settings form and click **Start**

> The native installers bundle their own Java runtime. No separate Java installation required.

### Step 4.1: macOS if "App can't be opened" (Gatekeeper)

If macOS blocks ModernMusicBot with a warning that it cannot be opened:

1. In Finder, open **Applications**
2. Right-click **ModernMusicBot.app** and click **Open**
3. Click **Open** again in the security prompt

If that still does not appear:

1. Go to **System Settings -> Privacy & Security**
2. Scroll to the security warning for ModernMusicBot
3. Click **Open Anyway**, then confirm

If you are running the app from a ZIP and macOS still blocks it, remove quarantine flags:

```bash
xattr -dr com.apple.quarantine /Applications/ModernMusicBot.app
```

Then launch the app again.

If macOS says the app is **"corrupted"** and should be moved to Trash, run:

```bash
xattr -dr com.apple.quarantine /Applications/ModernMusicBot.app
codesign --force --deep --sign - /Applications/ModernMusicBot.app
```

Then start ModernMusicBot again.

## Portable ZIP Setup (Advanced)

If you prefer the ZIP release or are on Linux:

### Requirements

- Java 25
- Maven 3.9+
- Discord bot token

1. Extract the ZIP and open the folder
2. Copy the config template:

```bash
cp ModernMusicBot.properties.example ModernMusicBot.properties
```

3. Open `ModernMusicBot.properties` and set your token:

```properties
bot.token=YOUR_DISCORD_BOT_TOKEN
bot.prefix=!
bot.language=en
bot.dashboard.enabled=true
bot.dashboard.port=8090
```

Local runtime observability:

- Dashboard: `http://127.0.0.1:8090/`
- Metrics JSON: `http://127.0.0.1:8090/metrics`

4. Launch the control panel:

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

Or start/stop the bot directly:

macOS: `./start.command` / `./stop.command`  
Linux: `./start.sh` / `./stop.sh`  
Windows: `start.bat` / `stop.bat`

## Building From Source

To build release ZIPs for all platforms:

```bash
./build-release-packages.sh
```

Output goes to `dist/releases/`. Native installers (`.dmg`, `.exe`) are built by the GitHub Actions release workflow.

Version naming behavior:

- Uses `RELEASE_VERSION` env var when provided.
- Uses the Git tag name automatically in the release workflow (e.g. `v2.0.0` → `2.0.0`).
- Falls back to Maven `project.version` otherwise.

Packaged targets:

- macOS (ZIP + `.dmg` installer)
- Linux x86-64 (ZIP)
- Linux ARM64 (ZIP)
- Windows x86-64 (ZIP + `.exe` installer)

## Commands

Text commands use your configured prefix (default `!`).

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
- `!remove <index>`
- `!shuffle`
- `!clear`
- `!loop <off|track|queue>`
- `!seek <seconds>`
- `!autoplay <on|off>`
- `!player`
- `!setprefix <value>` (admin)
- `!setlang <value>` (admin)
- `!setcommandchannel <#channel|channelId|off>` (admin)
- `!setblockedrole <@role|roleId|off>` (admin)
- `!settings`
- `!health`
- `!debugaudio`
- `!help`

Slash commands are also available after startup:

- `/play`, `/skip`, `/pause`, `/resume`, `/stop`, `/queue`, `/player`
- `/volume`, `/bass`, `/remove`, `/shuffle`, `/clear`, `/loop`, `/seek`, `/autoplay`
- `/setprefix`, `/setlang`, `/setdj`, `/setcommandchannel`, `/setblockedrole`, `/settings`, `/health`

Load-failure messages now include source-aware suggestions (for example, retry delay, search fallback, or alternate upload guidance).

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
