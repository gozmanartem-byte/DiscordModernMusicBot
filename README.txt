ModernMusicBot

For full setup and GitHub publish notes, see README_INSTALLATION.md.

1. Double-click start.command to launch
2. Double-click stop.command to stop
3. start.command uses ModernMusicBot.properties
4. Legacy application.properties is auto-migrated to ModernMusicBot.properties

This version uses Java 25 and JDave for Discord voice encryption support.

If you want a config file for this bot, copy ModernMusicBot.properties.example to ModernMusicBot.properties.
Do not publish ModernMusicBot.properties with a real token.

Commands:
!play <url or search>
!skip
!stop
!leave
!volume            (show current)
!volume <0-200>
!bass              (show current)
!bass <0-5>
!loud
!normal
!queue
!debugaudio
!help
