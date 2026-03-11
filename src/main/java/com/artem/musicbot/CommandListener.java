package com.artem.musicbot;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
    private final String prefix;
    private final MusicController musicController;
    private final I18n i18n;

    public CommandListener(String prefix, MusicController musicController, I18n i18n) {
        this.prefix = prefix;
        this.musicController = musicController;
        this.i18n = i18n;
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
            case "loud" -> musicController.setLoudPreset(channel, prefix);
            case "normal", "reset" -> musicController.resetNormal(channel);
            case "earrape" -> channel.sendMessage("I can't add an earrape mode. Use " + prefix + "volume and " + prefix + "bass instead.").queue();
            case "queue" -> musicController.queue(channel);
            case "player", "panel" -> {
                channel.sendMessage("**" + i18n.t("player.title") + "**\n" + i18n.t("player.hint", prefix))
                    .addComponents(ActionRow.of(
                                Button.secondary("player:pause", i18n.t("player.pause")),
                                Button.success("player:resume", i18n.t("player.resume")),
                                Button.primary("player:skip", i18n.t("player.skip")),
                                Button.danger("player:stop", i18n.t("player.stop"))
                    ))
                    .addComponents(ActionRow.of(
                                Button.primary("player:queue", i18n.t("player.queue")),
                                Button.secondary("player:voldown", i18n.t("player.voldown")),
                                Button.secondary("player:volup", i18n.t("player.volup"))
                    ))
                        .queue();
                channel.sendMessage(i18n.t("player.posted")).queue();
            }
            case "debugaudio", "dbg" -> musicController.debugAudio(channel);
            case "help" -> channel.sendMessage(helpText()).queue();
            default -> {
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild() || !event.getComponentId().startsWith("player:")) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            event.deferEdit().queue();
            return;
        }

        switch (event.getComponentId()) {
            case "player:pause" -> musicController.pause(channel);
            case "player:resume" -> musicController.resume(channel);
            case "player:skip" -> musicController.skip(channel);
            case "player:stop" -> musicController.stop(channel);
            case "player:queue" -> musicController.queue(channel);
            case "player:volup" -> musicController.adjustVolume(channel, 10);
            case "player:voldown" -> musicController.adjustVolume(channel, -10);
            default -> {
            }
        }

        event.deferEdit().queue();
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
                prefix + "player",
                prefix + "debugaudio",
                prefix + "help");
    }
}
