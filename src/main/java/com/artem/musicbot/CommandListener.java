package com.artem.musicbot;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
    private final String prefix;
    private final MusicController musicController;

    public CommandListener(String prefix, MusicController musicController) {
        this.prefix = prefix;
        this.musicController = musicController;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith(prefix)) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        String withoutPrefix = content.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return;
        }

        String[] parts = withoutPrefix.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case "play" -> {
                if (argument.isEmpty()) {
                    channel.sendMessage("Usage: " + prefix + "play <url or search>").queue();
                } else {
                    musicController.loadAndPlay(channel, event.getMember(), argument);
                }
            }
            case "skip" -> musicController.skip(channel);
            case "pause" -> musicController.pause(channel);
            case "resume" -> musicController.resume(channel);
            case "stop", "leave", "disconnect", "dc" -> musicController.stop(channel);
            case "volume", "vol" -> {
                if (argument.isBlank()) {
                    musicController.showVolume(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage("Usage: " + prefix + "volume <0-200>").queue();
                    } else {
                        musicController.setVolume(channel, value);
                    }
                }
            }
            case "bass" -> {
                if (argument.isBlank()) {
                    musicController.showBass(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage("Usage: " + prefix + "bass <0-5>").queue();
                    } else {
                        musicController.setBass(channel, value);
                    }
                }
            }
            case "loud" -> musicController.setLoudPreset(channel);
            case "normal", "reset" -> musicController.resetNormal(channel);
            case "earrape" -> channel.sendMessage("I can't add an earrape mode. Use " + prefix + "volume and " + prefix + "bass instead.").queue();
            case "queue" -> musicController.queue(channel);
            case "debugaudio", "dbg" -> musicController.debugAudio(channel);
            case "help" -> channel.sendMessage(helpText()).queue();
            default -> {
            }
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String helpText() {
        return String.join("\n",
                "Commands:",
                prefix + "play <url or search>",
                prefix + "skip",
                prefix + "pause",
                prefix + "resume",
                prefix + "stop",
                prefix + "leave",
                prefix + "volume <0-200>",
                prefix + "bass <0-5>",
                prefix + "loud",
                prefix + "normal",
                prefix + "queue",
                prefix + "debugaudio",
                prefix + "help");
    }
}
