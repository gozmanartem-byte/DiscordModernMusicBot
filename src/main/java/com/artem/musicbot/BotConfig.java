package com.artem.musicbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public record BotConfig(String token, String prefix, String youtubePoToken, String youtubeVisitorData) {
    public static BotConfig load(Path path) throws IOException {
        if (looksLikeLegacyConfig(path)) {
            return loadLegacy(path);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }

        String token = required(properties, "bot.token");
        String prefix = properties.getProperty("bot.prefix", "!").trim();
        String youtubePoToken = properties.getProperty("youtube.poToken", "").trim();
        String youtubeVisitorData = properties.getProperty("youtube.visitorData", "").trim();
        return new BotConfig(token, prefix, youtubePoToken, youtubeVisitorData);
    }

    private static boolean looksLikeLegacyConfig(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.equals("config.txt") || name.endsWith(".txt");
    }

    private static BotConfig loadLegacy(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Properties properties = new Properties();
        for (String line : lines) {
            String trimmed = stripInlineComment(line).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex < 0) {
                continue;
            }

            String key = trimmed.substring(0, separatorIndex).trim();
            String value = trimmed.substring(separatorIndex + 1).trim();
            properties.setProperty(key, value);
        }

        String token = required(properties, "token");
        String prefix = properties.getProperty("prefix", "!").trim();
        if (prefix.isEmpty()) {
            prefix = "!";
        }

        return new BotConfig(token, prefix, "", "");
    }

    private static String stripInlineComment(String value) {
        int commentIndex = value.indexOf("//");
        if (commentIndex >= 0) {
            return value.substring(0, commentIndex);
        }

        return value;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }
}
