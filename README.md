# DiscordModernMusicBot

![Java 25](https://img.shields.io/badge/Java-25-blue)
![Maven 3.9+](https://img.shields.io/badge/Maven-3.9%2B-C71A36)
![Platform macOS](https://img.shields.io/badge/Platform-macOS-lightgrey)

Modern Java Discord music bot built with JDA, Lavaplayer, youtube-source, and JDave voice encryption support.

Simple goal: run a stable Discord music bot locally with one config file and two launcher scripts.

## Features

- Plays music from URL or search query
- Queue management (`play`, `skip`, `stop`, `leave`)
- Audio controls (`volume`, `bass`, `loud`, `normal`)
- Debug command for audio diagnostics (`debugaudio`)
- macOS launcher scripts (`start.command`, `stop.command`)

## Requirements

- Java 25
- Maven 3.9+
- Discord bot token
- macOS (launcher scripts are macOS-first)

## Quick Start

1. Clone this repository.
2. In project root, copy config template:

```bash
cp ModernMusicBot.properties.example ModernMusicBot.properties
```

3. Open `ModernMusicBot.properties` and set your token:

```properties
bot.token=YOUR_DISCORD_BOT_TOKEN
bot.prefix=!
```

4. Start bot:

```bash
./start.command
```

5. Stop bot:

```bash
./stop.command
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
- `!debugaudio`
- `!help`

## Security

- Never commit `ModernMusicBot.properties` with a real token.
- If a token is exposed, rotate it immediately in Discord Developer Portal.

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
