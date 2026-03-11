package com.artem.musicbot;

import java.nio.file.Path;

public class ModernMusicBot {
    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("ModernMusicBot.properties");
        new BotRuntime().start(configPath, false, ignored -> { });
    }
}
