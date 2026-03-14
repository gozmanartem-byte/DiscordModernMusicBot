package com.artem.musicbot;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandListener extends ListenerAdapter {
    private static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("^<#(\\d+)>$");
    private static final Pattern ROLE_MENTION_PATTERN = Pattern.compile("^<@&(\\d+)>$");
    private static final int QUEUE_PAGE_SIZE = 10;
    private static final long EMPTY_VOICE_DISCONNECT_DELAY_MINUTES = 5L;

    private final String defaultPrefix;
    private final MusicController musicController;
    private final GuildSettingsStore settingsStore;
    private final boolean dashboardEnabled;
    private final int dashboardPort;
    private final ScheduledExecutorService disconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "auto-disconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, ScheduledFuture<?>> pendingDisconnects = new ConcurrentHashMap<>();

    public CommandListener(String defaultPrefix, MusicController musicController, I18n i18n, GuildSettingsStore settingsStore, boolean dashboardEnabled, int dashboardPort) {
        this.defaultPrefix = defaultPrefix;
        this.musicController = musicController;
        this.settingsStore = settingsStore;
        this.dashboardEnabled = dashboardEnabled;
        this.dashboardPort = dashboardPort;
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash("play", "Play from URL or search")
                                .addOption(OptionType.STRING, "query", "URL or search query", true),
                        Commands.slash("skip", "Skip current track"),
                        Commands.slash("pause", "Pause playback"),
                        Commands.slash("resume", "Resume playback"),
                        Commands.slash("stop", "Stop and disconnect"),
                        Commands.slash("queue", "Show queue"),
                        Commands.slash("player", "Show interactive player panel"),
                        Commands.slash("volume", "Set or show volume")
                                .addOption(OptionType.INTEGER, "value", "0-200", false),
                        Commands.slash("bass", "Set or show bass")
                                .addOption(OptionType.INTEGER, "value", "0-5", false),
                        Commands.slash("remove", "Remove a queue item by index")
                                .addOption(OptionType.INTEGER, "index", "Queue position (starting at 1)", true),
                        Commands.slash("shuffle", "Shuffle queue"),
                        Commands.slash("clear", "Clear queue"),
                        Commands.slash("loop", "Set loop mode")
                                .addOption(OptionType.STRING, "mode", "off|track|queue", true),
                        Commands.slash("seek", "Seek current track in seconds")
                                .addOption(OptionType.INTEGER, "seconds", "Position in seconds", true),
                        Commands.slash("autoplay", "Enable/disable autoplay")
                                .addOption(OptionType.BOOLEAN, "enabled", "true or false", true),
                        Commands.slash("settings", "Show guild settings"),
                        Commands.slash("setprefix", "Set guild text-command prefix")
                                .addOption(OptionType.STRING, "prefix", "New prefix", true),
                        Commands.slash("setlang", "Set guild language for player/status")
                                .addOption(OptionType.STRING, "language", "Language code or name", true),
                        Commands.slash("setdj", "Set DJ role (admins always allowed)")
                                .addOption(OptionType.ROLE, "role", "Role to require for DJ commands", true),
                        Commands.slash("setcommandchannel", "Restrict bot commands to one channel (off to disable)")
                            .addOption(OptionType.CHANNEL, "channel", "Allowed command channel", false)
                            .addOption(OptionType.BOOLEAN, "off", "Set true to disable restriction", false),
                        Commands.slash("setblockedrole", "Block one role from using bot commands (off to disable)")
                            .addOption(OptionType.ROLE, "role", "Role to block", false)
                            .addOption(OptionType.BOOLEAN, "off", "Set true to disable restriction", false),
                        Commands.slash("dashboard", "Show local dashboard URL"),
                        Commands.slash("health", "Show runtime health summary"),
                        Commands.slash("debugaudio", "Show audio debug details")
                )
                .queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        GuildSettings settings = settingsStore.get(event.getGuild().getIdLong());
        I18n i18n = i18nFor(settings);
        String prefix = settings.prefix();
        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith(prefix)) {
            return;
        }

        String withoutPrefix = content.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return;
        }

        String[] parts = withoutPrefix.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        if (!canUseTextCommands(channel, event.getMember(), settings)) {
            return;
        }

        switch (command) {
            case "play" -> {
                if (argument.isEmpty()) {
                    channel.sendMessage(i18n.t("usage.play", prefix)).queue();
                } else {
                    musicController.loadAndPlay(channel, event.getMember(), argument);
                }
            }
            case "skip" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.skip(channel));
            case "pause" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.pause(channel));
            case "resume" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.resume(channel));
            case "stop", "leave", "disconnect", "dc" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.stop(channel));
            case "volume", "vol" -> {
                if (argument.isBlank()) {
                    musicController.showVolume(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage(i18n.t("usage.volume", prefix)).queue();
                    } else {
                        doDjChecked(channel, event.getMember(), settings, () -> musicController.setVolume(channel, value));
                    }
                }
            }
            case "bass" -> {
                if (argument.isBlank()) {
                    musicController.showBass(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage(i18n.t("usage.bass", prefix)).queue();
                    } else {
                        doDjChecked(channel, event.getMember(), settings, () -> musicController.setBass(channel, value));
                    }
                }
            }
            case "loud", "normal", "reset", "earrape" -> channel.sendMessage(i18n.t("earrape.hostOnly")).queue();
            case "queue" -> musicController.queue(channel);
            case "remove" -> {
                Integer value = parseInt(argument);
                if (value == null) {
                    channel.sendMessage(i18n.t("usage.remove", prefix)).queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.remove(channel, value));
                }
            }
            case "shuffle" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.shuffleQueue(channel));
            case "clear" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.clearQueue(channel));
            case "loop" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.setLoop(channel, argument));
            case "seek" -> {
                Integer value = parseInt(argument);
                if (value == null) {
                    channel.sendMessage(i18n.t("usage.seek", prefix)).queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.seek(channel, value.longValue()));
                }
            }
            case "autoplay" -> {
                if (!"on".equalsIgnoreCase(argument) && !"off".equalsIgnoreCase(argument)) {
                    channel.sendMessage(i18n.t("usage.autoplay", prefix)).queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.setAutoplay(channel, "on".equalsIgnoreCase(argument)));
                }
            }
            case "player", "panel" -> musicController.sendPlayerPanel(channel, prefix);
            case "debugaudio" -> musicController.debugAudio(channel);
            case "setprefix" -> doAdminChecked(channel, event.getMember(), () -> setPrefix(channel, argument, settings));
            case "setlang" -> doAdminChecked(channel, event.getMember(), () -> setLanguage(channel, argument, settings));
            case "setdj" -> channel.sendMessage(i18n.t("setdj.slashOnly")).queue();
            case "setcommandchannel" -> doAdminChecked(channel, event.getMember(), () -> setCommandChannel(channel, argument, settings));
            case "setblockedrole" -> doAdminChecked(channel, event.getMember(), () -> setBlockedRole(channel, argument, settings));
            case "settings" -> channel.sendMessage(formatSettings(settings)).queue();
            case "dashboard" -> channel.sendMessage(dashboardMessage()).queue();
            case "health" -> channel.sendMessage(musicController.healthSummary()).queue();
            case "help" -> channel.sendMessage(i18n.t("help", prefix)).queue();
            default -> {
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (!event.isFromGuild() || guild == null || !(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        GuildSettings settings = settingsStore.get(guild.getIdLong());
        Member member = event.getMember();
        String prefix = settings.prefix();

        if (!canUseSlashCommands(event, channel, member, settings)) {
            return;
        }

        switch (event.getName()) {
            case "play" -> musicController.loadAndPlay(channel, member, event.getOption("query", "", OptionMapping::getAsString));
            case "skip" -> runDjSlash(event, member, settings, () -> musicController.skip(channel));
            case "pause" -> runDjSlash(event, member, settings, () -> musicController.pause(channel));
            case "resume" -> runDjSlash(event, member, settings, () -> musicController.resume(channel));
            case "stop" -> runDjSlash(event, member, settings, () -> musicController.stop(channel));
            case "queue" -> musicController.queue(channel);
            case "player" -> musicController.sendPlayerPanel(channel, prefix);
            case "volume" -> {
                OptionMapping value = event.getOption("value");
                if (value == null) {
                    musicController.showVolume(channel);
                } else {
                    runDjSlash(event, member, settings, () -> musicController.setVolume(channel, value.getAsInt()));
                }
            }
            case "bass" -> {
                OptionMapping value = event.getOption("value");
                if (value == null) {
                    musicController.showBass(channel);
                } else {
                    runDjSlash(event, member, settings, () -> musicController.setBass(channel, value.getAsInt()));
                }
            }
            case "remove" -> runDjSlash(event, member, settings, () -> musicController.remove(channel, event.getOption("index", 1, OptionMapping::getAsInt)));
            case "shuffle" -> runDjSlash(event, member, settings, () -> musicController.shuffleQueue(channel));
            case "clear" -> runDjSlash(event, member, settings, () -> musicController.clearQueue(channel));
            case "loop" -> runDjSlash(event, member, settings, () -> musicController.setLoop(channel, event.getOption("mode", "off", OptionMapping::getAsString)));
            case "seek" -> runDjSlash(event, member, settings, () -> musicController.seek(channel, event.getOption("seconds", 0L, OptionMapping::getAsLong)));
            case "autoplay" -> runDjSlash(event, member, settings, () -> musicController.setAutoplay(channel, event.getOption("enabled", false, OptionMapping::getAsBoolean)));
            case "settings" -> channel.sendMessage(formatSettings(settings)).queue();
            case "setprefix" -> runAdminSlash(event, member, () -> setPrefix(channel, event.getOption("prefix", defaultPrefix, OptionMapping::getAsString), settings));
            case "setlang" -> runAdminSlash(event, member, () -> setLanguage(channel, event.getOption("language", settings.language(), OptionMapping::getAsString), settings));
            case "setdj" -> runAdminSlash(event, member, () -> {
                Role role = event.getOption("role", null, OptionMapping::getAsRole);
                if (role == null) {
                    event.reply(i18nFor(settings).t("role.required")).setEphemeral(true).queue();
                    return;
                }
                GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), role.getIdLong(), settings.defaultVolume(), settings.autoplay(), settings.commandChannelId(), settings.blockedRoleId());
                settingsStore.upsert(next);
                channel.sendMessage(i18nFor(settings).t("dj.role.set", role.getName())).queue();
            });
            case "setcommandchannel" -> runAdminSlash(event, member, () -> {
                boolean disable = event.getOption("off", false, OptionMapping::getAsBoolean);
                var selected = event.getOption("channel", null, OptionMapping::getAsChannel);
                if (disable) {
                    GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), settings.djRoleId(), settings.defaultVolume(), settings.autoplay(), 0L, settings.blockedRoleId());
                    settingsStore.upsert(next);
                    event.reply(i18nFor(settings).t("restriction.command.disabled")).setEphemeral(true).queue();
                    return;
                }
                if (selected == null || !selected.getType().isMessage()) {
                    event.reply(i18nFor(settings).t("restriction.command.provideChannel")).setEphemeral(true).queue();
                    return;
                }
                GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), settings.djRoleId(), settings.defaultVolume(), settings.autoplay(), selected.getIdLong(), settings.blockedRoleId());
                settingsStore.upsert(next);
                event.reply(i18nFor(settings).t("restriction.commands.enabled", selected.getIdLong())).setEphemeral(true).queue();
            });
            case "setblockedrole" -> runAdminSlash(event, member, () -> {
                boolean disable = event.getOption("off", false, OptionMapping::getAsBoolean);
                Role selected = event.getOption("role", null, OptionMapping::getAsRole);
                if (disable) {
                    GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), settings.djRoleId(), settings.defaultVolume(), settings.autoplay(), settings.commandChannelId(), 0L);
                    settingsStore.upsert(next);
                    event.reply(i18nFor(settings).t("restriction.blockedRole.disabled")).setEphemeral(true).queue();
                    return;
                }
                if (selected == null) {
                    event.reply(i18nFor(settings).t("restriction.blockedRole.provideRole")).setEphemeral(true).queue();
                    return;
                }
                GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), settings.djRoleId(), settings.defaultVolume(), settings.autoplay(), settings.commandChannelId(), selected.getIdLong());
                settingsStore.upsert(next);
                event.reply(i18nFor(settings).t("restriction.blockedRole.name.enabled", selected.getName())).setEphemeral(true).queue();
            });
            case "dashboard" -> event.reply(dashboardMessage()).setEphemeral(true).queue();
            case "health" -> channel.sendMessage(musicController.healthSummary()).queue();
            case "debugaudio" -> musicController.debugAudio(channel);
            default -> {
            }
        }

        if (!event.isAcknowledged()) {
            event.deferReply(true).queue(hook -> hook.deleteOriginal().queue(
                    ignored -> {
                    },
                    ignored -> {
                    }
            ));
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (!event.isFromGuild() || guild == null) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            event.deferEdit().queue();
            return;
        }

        GuildSettings settings = settingsStore.get(guild.getIdLong());
        if (!canUseButtonCommands(event, channel, event.getMember(), settings)) {
            return;
        }

        if (event.getComponentId().startsWith("searchpick:")) {
            String[] parts = event.getComponentId().split(":");
            if (parts.length != 3) {
                event.reply(i18nFor(settings).t("search.invalid")).setEphemeral(true).queue();
                return;
            }

            Integer index = parseInt(parts[2]);
            if (index == null) {
                event.reply(i18nFor(settings).t("search.invalid")).setEphemeral(true).queue();
                return;
            }

            MusicController.SearchSelectionOutcome outcome = musicController.chooseSearchResult(channel, event.getMember(), parts[1], index);
            if (!outcome.success()) {
                event.reply(outcome.message()).setEphemeral(true).queue();
                return;
            }

            event.editMessage(outcome.message()).setComponents().queue();
            return;
        }

        if (event.getComponentId().startsWith("searchcancel:")) {
            String[] parts = event.getComponentId().split(":");
            if (parts.length != 2) {
                event.reply(i18nFor(settings).t("search.invalid")).setEphemeral(true).queue();
                return;
            }

            MusicController.SearchSelectionOutcome outcome = musicController.cancelSearchSelection(channel, event.getMember(), parts[1]);
            if (!outcome.success()) {
                event.reply(outcome.message()).setEphemeral(true).queue();
                return;
            }

            event.editMessage(outcome.message()).setComponents().queue();
            return;
        }

        if (event.getComponentId().startsWith("playerq:")) {
            Integer requestedPage = parseInt(event.getComponentId().substring("playerq:".length()));
            int page = requestedPage == null ? 0 : Math.max(0, requestedPage);
            event.editMessageEmbeds(buildQueuePageEmbed(channel, settings, page))
                    .setComponents(queuePageButtons(settings, channel, page))
                    .queue();
            return;
        }

        if ("player:queue".equals(event.getComponentId())) {
            doDjChecked(channel, event.getMember(), settings, () ->
                    event.replyEmbeds(buildQueuePageEmbed(channel, settings, 0))
                        .setComponents(queuePageButtons(settings, channel, 0))
                            .setEphemeral(true)
                            .queue());
            return;
        }

        if (!event.getComponentId().startsWith("player:")) {
            return;
        }

        boolean refreshPanelAfterAction = true;

        switch (event.getComponentId()) {
            case "player:pause" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.pause(channel));
            case "player:resume" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.resume(channel));
            case "player:skip" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.skip(channel));
            case "player:stop" -> {
                doDjChecked(channel, event.getMember(), settings, () -> musicController.stop(channel));
                refreshPanelAfterAction = false;
            }
            case "player:volup" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustVolume(channel, 10));
            case "player:voldown" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustVolume(channel, -10));
            case "player:bassup" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustBass(channel, 1));
            case "player:bassdown" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustBass(channel, -1));
            case "player:bassreset" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.setBass(channel, 0));
            case "player:qremove" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.remove(channel, 1));
            case "player:qclear" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.clearQueue(channel));
            case "player:refresh" -> {
            }
            default -> {
            }
        }

        if (refreshPanelAfterAction) {
            event.deferEdit().queue(ignored -> musicController.refreshPlayerPanel(event.getMessage(), settings.prefix()));
        } else {
            event.deferEdit().queue();
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        var selfState = guild.getSelfMember().getVoiceState();
        if (selfState == null || selfState.getChannel() == null) {
            cancelPendingDisconnect(guild.getIdLong());
            return;
        }

        if (!(selfState.getChannel() instanceof VoiceChannel voiceChannel)) {
            cancelPendingDisconnect(guild.getIdLong());
            return;
        }

        long humanCount = voiceChannel.getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .count();

        if (humanCount == 0) {
            scheduleDisconnectIfNeeded(guild, voiceChannel);
        } else {
            cancelPendingDisconnect(guild.getIdLong());
        }
    }

    private void scheduleDisconnectIfNeeded(Guild guild, VoiceChannel voiceChannel) {
        long guildId = guild.getIdLong();
        pendingDisconnects.compute(guildId, (ignored, existing) -> {
            if (existing != null && !existing.isDone() && !existing.isCancelled()) {
                return existing;
            }
            return disconnectScheduler.schedule(() -> performAutoDisconnect(guild, voiceChannel.getIdLong()),
                    EMPTY_VOICE_DISCONNECT_DELAY_MINUTES, TimeUnit.MINUTES);
        });
    }

    private void cancelPendingDisconnect(long guildId) {
        ScheduledFuture<?> future = pendingDisconnects.remove(guildId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void performAutoDisconnect(Guild guild, long voiceChannelId) {
        long guildId = guild.getIdLong();
        var selfState = guild.getSelfMember().getVoiceState();
        if (selfState == null || selfState.getChannel() == null || selfState.getChannel().getIdLong() != voiceChannelId) {
            cancelPendingDisconnect(guildId);
            return;
        }

        if (selfState.getChannel() instanceof VoiceChannel voiceChannel) {
            long humanCount = voiceChannel.getMembers().stream()
                    .filter(member -> !member.getUser().isBot())
                    .count();
            if (humanCount > 0) {
                cancelPendingDisconnect(guildId);
                return;
            }
        }

        TextChannel channel = resolvePreferredTextChannel(guild);
        musicController.stopFromAutoDisconnect(guild, channel);
        cancelPendingDisconnect(guildId);
    }

    private TextChannel resolvePreferredTextChannel(Guild guild) {
        long preferredId = musicController.preferredTextChannelId(guild.getIdLong());
        if (preferredId != 0L) {
            TextChannel preferred = guild.getTextChannelById(preferredId);
            if (preferred != null) {
                return preferred;
            }
        }
        return guild.getTextChannels().isEmpty() ? null : guild.getTextChannels().get(0);
    }

    private void setPrefix(TextChannel channel, String newPrefix, GuildSettings current) {
        I18n i18n = i18nFor(current);
        String trimmed = newPrefix == null ? "" : newPrefix.trim();
        if (trimmed.isEmpty() || trimmed.length() > 3) {
            channel.sendMessage(i18n.t("prefix.invalid")).queue();
            return;
        }

        GuildSettings next = new GuildSettings(current.guildId(), trimmed, current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay(), current.commandChannelId(), current.blockedRoleId());
        settingsStore.upsert(next);
        channel.sendMessage(i18n.t("prefix.updated", trimmed)).queue();
    }

    private void setLanguage(TextChannel channel, String languageInput, GuildSettings current) {
        I18n i18n = i18nFor(current);
        String normalized = I18n.Language.from(languageInput).code();
        GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), normalized, current.djRoleId(), current.defaultVolume(), current.autoplay(), current.commandChannelId(), current.blockedRoleId());
        settingsStore.upsert(next);
        musicController.refreshPresenceForGuild(channel.getGuild());
        channel.sendMessage(i18n.t("language.updated", normalized)).queue();
    }

    private void setCommandChannel(TextChannel channel, String argument, GuildSettings current) {
        I18n i18n = i18nFor(current);
        if (argument == null || argument.isBlank() || "off".equalsIgnoreCase(argument.trim())) {
            GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay(), 0L, current.blockedRoleId());
            settingsStore.upsert(next);
            channel.sendMessage(i18n.t("restriction.command.disabled")).queue();
            return;
        }

        Long channelId = parseChannelId(argument);
        if (channelId == null || channel.getGuild().getTextChannelById(channelId) == null) {
            channel.sendMessage(i18n.t("usage.setcommandchannel", current.prefix())).queue();
            return;
        }

        GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay(), channelId, current.blockedRoleId());
        settingsStore.upsert(next);
        channel.sendMessage(i18n.t("restriction.commands.enabled", channelId)).queue();
    }

    private void setBlockedRole(TextChannel channel, String argument, GuildSettings current) {
        I18n i18n = i18nFor(current);
        if (argument == null || argument.isBlank() || "off".equalsIgnoreCase(argument.trim())) {
            GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay(), current.commandChannelId(), 0L);
            settingsStore.upsert(next);
            channel.sendMessage(i18n.t("restriction.blockedRole.disabled")).queue();
            return;
        }

        Long roleId = parseRoleId(argument);
        if (roleId == null || channel.getGuild().getRoleById(roleId) == null) {
            channel.sendMessage(i18n.t("usage.setblockedrole", current.prefix())).queue();
            return;
        }

        GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay(), current.commandChannelId(), roleId);
        settingsStore.upsert(next);
        channel.sendMessage(i18n.t("restriction.blockedRole.enabled", roleId)).queue();
    }

    private boolean hasDjPermission(Member member, GuildSettings settings) {
        if (member == null) {
            return false;
        }

        if (member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }

        long djRoleId = settings.djRoleId();
        if (djRoleId == 0L) {
            return true;
        }

        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == djRoleId);
    }

    private void doDjChecked(TextChannel channel, Member member, GuildSettings settings, Runnable action) {
        I18n i18n = i18nFor(settings);
        if (!hasDjPermission(member, settings)) {
            channel.sendMessage(i18n.t("permission.dj")).queue();
            return;
        }
        action.run();
    }

    private void runDjSlash(SlashCommandInteractionEvent event, Member member, GuildSettings settings, Runnable action) {
        I18n i18n = i18nFor(settings);
        if (!hasDjPermission(member, settings)) {
            event.reply(i18n.t("permission.dj")).setEphemeral(true).queue();
            return;
        }
        action.run();
    }

    private void doAdminChecked(TextChannel channel, Member member, Runnable action) {
        I18n i18n = i18nFor(settingsStore.get(channel.getGuild().getIdLong()));
        if (member == null || !(member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR))) {
            channel.sendMessage(i18n.t("permission.admin")).queue();
            return;
        }
        action.run();
    }

    private void runAdminSlash(SlashCommandInteractionEvent event, Member member, Runnable action) {
        Guild guild = event.getGuild();
        I18n i18n = guild == null ? new I18n("en") : i18nFor(settingsStore.get(guild.getIdLong()));
        if (member == null || !(member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR))) {
            event.reply(i18n.t("permission.admin")).setEphemeral(true).queue();
            return;
        }
        action.run();
    }

    private Integer parseInt(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatSettings(GuildSettings settings) {
        return String.join("\n",
                "Settings:",
                "prefix=" + settings.prefix(),
                "language=" + settings.language(),
                "djRoleId=" + settings.djRoleId(),
                "defaultVolume=" + settings.defaultVolume(),
                "autoplay=" + settings.autoplay(),
                "commandChannelId=" + settings.commandChannelId(),
                "blockedRoleId=" + settings.blockedRoleId());
    }

    private String helpText(String prefix) {
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
                prefix + "queue",
                prefix + "remove <index>",
                prefix + "shuffle",
                prefix + "clear",
                prefix + "loop <off|track|queue>",
                prefix + "seek <seconds>",
                prefix + "autoplay <on|off>",
                prefix + "setprefix <value>",
                prefix + "setlang <value>",
                prefix + "setcommandchannel <#channel|channelId|off>",
                prefix + "setblockedrole <@role|roleId|off>",
                prefix + "settings",
                prefix + "dashboard",
                prefix + "player",
                prefix + "health",
                prefix + "debugaudio",
                prefix + "help");
    }

    private String dashboardMessage() {
        if (!dashboardEnabled) {
            return "Dashboard is disabled in config (`bot.dashboard.enabled=false`).";
        }
        return "Local dashboard: http://127.0.0.1:" + dashboardPort + "/\nMetrics JSON: http://127.0.0.1:" + dashboardPort + "/metrics";
    }

    private boolean canUseTextCommands(TextChannel channel, Member member, GuildSettings settings) {
        I18n i18n = i18nFor(settings);
        if (isAdmin(member)) {
            return true;
        }

        if (isBlockedByRole(member, settings)) {
            channel.sendMessage(i18n.t("blocked.role")).queue();
            return false;
        }

        if (settings.commandChannelId() != 0L && settings.commandChannelId() != channel.getIdLong()) {
            channel.sendMessage(i18n.t("restricted.commands", settings.commandChannelId())).queue();
            return false;
        }

        return true;
    }

    private boolean canUseSlashCommands(SlashCommandInteractionEvent event, TextChannel channel, Member member, GuildSettings settings) {
        I18n i18n = i18nFor(settings);
        if (isAdmin(member)) {
            return true;
        }

        if (isBlockedByRole(member, settings)) {
            event.reply(i18n.t("blocked.role")).setEphemeral(true).queue();
            return false;
        }

        if (settings.commandChannelId() != 0L && settings.commandChannelId() != channel.getIdLong()) {
            event.reply(i18n.t("restricted.commands", settings.commandChannelId())).setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    private boolean canUseButtonCommands(ButtonInteractionEvent event, TextChannel channel, Member member, GuildSettings settings) {
        I18n i18n = i18nFor(settings);
        if (isAdmin(member)) {
            return true;
        }

        if (isBlockedByRole(member, settings)) {
            event.reply(i18n.t("blocked.role")).setEphemeral(true).queue();
            return false;
        }

        if (settings.commandChannelId() != 0L && settings.commandChannelId() != channel.getIdLong()) {
            event.reply(i18n.t("restricted.buttons", settings.commandChannelId())).setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    private net.dv8tion.jda.api.components.MessageTopLevelComponent queuePageButtons(GuildSettings settings, TextChannel channel, int requestedPage) {
        I18n i18n = i18nFor(settings);
        List<String> queue = musicController.desktopQueueEntries(channel.getGuild().getIdLong());
        int totalPages = Math.max(1, (queue.size() + QUEUE_PAGE_SIZE - 1) / QUEUE_PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));

        boolean isFirst = page <= 0;
        boolean isLast = page >= totalPages - 1;

        return net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                net.dv8tion.jda.api.components.buttons.Button.secondary("playerq:" + Math.max(0, page - 1), i18n.t("player.queue.prev")).withDisabled(isFirst),
                net.dv8tion.jda.api.components.buttons.Button.secondary("playerq:" + Math.min(totalPages - 1, page + 1), i18n.t("player.queue.next")).withDisabled(isLast)
        );
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildQueuePageEmbed(TextChannel channel, GuildSettings settings, int requestedPage) {
        I18n i18n = i18nFor(settings);
        List<String> queue = musicController.desktopQueueEntries(channel.getGuild().getIdLong());
        int totalPages = Math.max(1, (queue.size() + QUEUE_PAGE_SIZE - 1) / QUEUE_PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(i18n.t("player.queue"));

        if (queue.isEmpty()) {
            builder.setDescription(i18n.t("player.queueEmpty"));
            builder.setFooter(i18n.t("player.queue.page", 1, 1));
            return builder.build();
        }

        int start = page * QUEUE_PAGE_SIZE;
        int end = Math.min(queue.size(), start + QUEUE_PAGE_SIZE);
        StringBuilder description = new StringBuilder();
        for (int i = start; i < end; i++) {
            description.append(i + 1).append(". ").append(queue.get(i)).append('\n');
        }

        builder.setDescription(description.toString().trim());
        builder.setFooter(i18n.t("player.queue.page", page + 1, totalPages));
        return builder.build();
    }

    private boolean isAdmin(Member member) {
        return member != null && (member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR));
    }

    private boolean isBlockedByRole(Member member, GuildSettings settings) {
        if (member == null || settings.blockedRoleId() == 0L) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == settings.blockedRoleId());
    }

    private Long parseChannelId(String value) {
        String trimmed = value == null ? "" : value.trim();
        Matcher mention = CHANNEL_MENTION_PATTERN.matcher(trimmed);
        if (mention.matches()) {
            return parseLong(mention.group(1));
        }
        return parseLong(trimmed);
    }

    private Long parseRoleId(String value) {
        String trimmed = value == null ? "" : value.trim();
        Matcher mention = ROLE_MENTION_PATTERN.matcher(trimmed);
        if (mention.matches()) {
            return parseLong(mention.group(1));
        }
        return parseLong(trimmed);
    }

    private Long parseLong(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private I18n i18nFor(GuildSettings settings) {
        return new I18n(settings.language());
    }
}
