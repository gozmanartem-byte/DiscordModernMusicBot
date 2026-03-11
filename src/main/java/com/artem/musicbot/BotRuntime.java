package com.artem.musicbot;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class BotRuntime {
    private JDA jda;
    private LocalDashboardServer dashboardServer;
    private MusicController musicController;
    private GuildSettingsStore settingsStore;

    public synchronized boolean isRunning() {
        return jda != null;
    }

    public synchronized void start(Path configPath, boolean waitUntilReady, Consumer<String> logger) throws Exception {
        if (jda != null) {
            throw new IllegalStateException("Bot is already running.");
        }

        Path resolvedConfigPath = configPath.toAbsolutePath().normalize();
        Path stateDirectory = resolvedConfigPath.getParent() == null
                ? Path.of(".").toAbsolutePath().normalize()
                : resolvedConfigPath.getParent();

        BotConfig config = BotConfig.load(resolvedConfigPath);
        I18n i18n = new I18n(config.languageCode());
        GuildSettingsStore settingsStore = new GuildSettingsStore(stateDirectory.resolve("guild-settings.db"), config.prefix(), config.languageCode());

        AudioModuleConfig audioModuleConfig = new AudioModuleConfig()
            .withDaveSessionFactory(new JDaveSessionFactory())
            .withAudioSendFactory(new DefaultSendFactory());

        MusicController musicController = new MusicController(config, i18n, settingsStore);
        this.musicController = musicController;
        this.settingsStore = settingsStore;

        JDA built = JDABuilder.createDefault(config.token(), EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.MESSAGE_CONTENT))
            .disableCache(
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.SCHEDULED_EVENTS)
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .setActivity(Activity.playing(i18n.t("status.waiting")))
            .setStatus(OnlineStatus.ONLINE)
            .setAudioModuleConfig(audioModuleConfig)
                .addEventListeners(new CommandListener(
                    config.prefix(),
                    musicController,
                    i18n,
                    settingsStore,
                    config.dashboardEnabled(),
                    config.dashboardPort()))
            .build();

        if (waitUntilReady) {
            built.awaitReady();
        }

        jda = built;

        if (config.dashboardEnabled()) {
            dashboardServer = new LocalDashboardServer(
                    config.dashboardPort(),
                    musicController::metricsSnapshot,
                    musicController::healthSummary,
                    () -> this.jda == null ? "stopped" : this.jda.getStatus().name(),
                    Instant.now()
            );
            dashboardServer.start();
            logger.accept("Dashboard started at " + dashboardServer.baseUrl());
        }

        logger.accept("Config path: " + resolvedConfigPath);
        logger.accept("State path: " + stateDirectory);
        logger.accept("Bot started successfully.");
    }

    public synchronized void stop(Consumer<String> logger) {
        if (jda == null) {
            logger.accept("Bot is not running.");
            return;
        }

        jda.shutdownNow();
        jda = null;
        musicController = null;
        settingsStore = null;

        if (dashboardServer != null) {
            dashboardServer.stop();
            dashboardServer = null;
        }

        logger.accept("Bot stopped.");
    }

    public synchronized List<GuildRef> guildRefs() {
        if (jda == null) {
            return List.of();
        }
        return jda.getGuilds().stream()
                .map(guild -> new GuildRef(guild.getIdLong(), guild.getName()))
                .toList();
    }

    public synchronized List<ChannelRef> textChannelRefs(long guildId) {
        if (jda == null) {
            return List.of();
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return List.of();
        }
        return guild.getTextChannels().stream()
                .map(channel -> new ChannelRef(channel.getIdLong(), channel.getName()))
                .toList();
    }

    public synchronized void addSongFromDesktop(long guildId, long channelId, String query) {
        if (musicController == null) {
            throw new IllegalStateException("Bot is not running.");
        }
        TextChannel channel = resolveTextChannel(guildId, channelId);
        if (channel == null) {
            throw new IllegalStateException("Selected text channel is unavailable.");
        }
        musicController.enqueueFromControlPanel(channel, query);
    }

    public synchronized List<SearchTrackOptionRef> searchTracksFromDesktop(String query, int limit) {
        if (musicController == null) {
            throw new IllegalStateException("Bot is not running.");
        }
        return musicController.searchTopTracksForDesktop(query, limit).stream()
                .map(option -> new SearchTrackOptionRef(option.title(), option.uri()))
                .toList();
    }

    public synchronized void pauseFromDesktop(long guildId, long channelId) {
        TextChannel channel = requireTextChannel(guildId, channelId);
        musicController.pause(channel);
    }

    public synchronized void resumeFromDesktop(long guildId, long channelId) {
        TextChannel channel = requireTextChannel(guildId, channelId);
        musicController.resume(channel);
    }

    public synchronized void skipFromDesktop(long guildId, long channelId) {
        TextChannel channel = requireTextChannel(guildId, channelId);
        musicController.skip(channel);
    }

    public synchronized void stopFromDesktop(long guildId, long channelId) {
        TextChannel channel = requireTextChannel(guildId, channelId);
        musicController.stop(channel);
    }

    public synchronized void launchPlayerPanelFromDesktop(long guildId, long channelId) {
        TextChannel channel = requireTextChannel(guildId, channelId);
        musicController.sendPlayerPanelFromDesktop(channel);
    }

    public synchronized String playerSummary() {
        if (musicController == null) {
            return "Bot is not running.";
        }
        MusicController.MetricsSnapshot metrics = musicController.metricsSnapshot();
        return String.join("\n",
                "State: " + metrics.nowPlayingState(),
                "Track: " + metrics.nowPlayingTitle(),
                "Queued tracks: " + metrics.queuedTracks(),
                "Active players: " + metrics.activePlayers());
    }

    public synchronized String playerSummaryForGuild(long guildId) {
        if (musicController == null) {
            return "Bot is not running.";
        }
        return musicController.desktopPlayerSummary(guildId);
    }

    public synchronized long preferredTextChannelId(long guildId) {
        if (musicController == null) {
            return 0L;
        }
        return musicController.preferredTextChannelId(guildId);
    }

    private TextChannel requireTextChannel(long guildId, long channelId) {
        if (musicController == null) {
            throw new IllegalStateException("Bot is not running.");
        }
        TextChannel channel = resolveTextChannel(guildId, channelId);
        if (channel == null) {
            throw new IllegalStateException("Selected text channel is unavailable.");
        }
        return channel;
    }

    private TextChannel resolveTextChannel(long guildId, long channelId) {
        if (jda == null) {
            return null;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return null;
        }
        return guild.getTextChannelById(channelId);
    }

    public record GuildRef(long id, String name) {
    }

    public record ChannelRef(long id, String name) {
    }

    public record SearchTrackOptionRef(String title, String uri) {
    }
}
