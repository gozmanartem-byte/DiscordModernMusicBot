package com.artem.musicbot;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Web;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class MusicController {
    private static final int DEFAULT_VOLUME = 100;
    private static final int MAX_VOLUME = 200;
    private static final int MAX_BASS = 5;
    private static final int BULK_DELETE_LIMIT = 100;
    private static final long PLAYER_PANEL_REFRESH_TICK_SECONDS = 1L;
    private static final long PLAYER_PANEL_PLAYING_REFRESH_MS = 1000L;
    private static final long PLAYER_PANEL_PAUSED_REFRESH_MS = 20000L;
    private static final int MIN_FRAME_BUFFER_MS = 1400;

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
    private final Map<Long, String> lastTrackQueries = new ConcurrentHashMap<>();
    private final Map<Long, TextChannel> lastTextChannels = new ConcurrentHashMap<>();
    private final I18n i18n;
    private final GuildSettingsStore settingsStore;
    private final AtomicLong loadSuccessCount = new AtomicLong();
    private final AtomicLong loadFailureCount = new AtomicLong();
    private final AtomicLong noMatchesCount = new AtomicLong();
    private final AtomicLong panelCreateCount = new AtomicLong();
    private final AtomicLong panelEditCount = new AtomicLong();
    private final AtomicLong panelSkipCount = new AtomicLong();
    private final AtomicLong searchSelectionCounter = new AtomicLong();
    private final Map<String, PendingSearch> pendingSearches = new ConcurrentHashMap<>();
    private final Map<Long, Long> playerPanelMessageIds = new ConcurrentHashMap<>();
    private final Map<Long, Long> playerPanelChannelIds = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> playerPanelRefreshInFlight = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> playerPanelRefreshPending = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> playerPanelAutoRefreshTasks = new ConcurrentHashMap<>();
    private final Map<Long, Long> playerPanelLastAutoRefreshMillis = new ConcurrentHashMap<>();
    private final Map<Long, String> playerPanelLastRenderedSignature = new ConcurrentHashMap<>();
    private final ScheduledExecutorService playerPanelRefresher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "player-panel-refresher");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, Long> stopCleanupUntilMillis = new ConcurrentHashMap<>();

    public MusicController(BotConfig config, I18n i18n, GuildSettingsStore settingsStore) {
        this.i18n = i18n;
        this.settingsStore = settingsStore;
        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
        this.playerManager.getConfiguration().setFrameBufferFactory(
            (bufferDuration, format, stopping) -> new NonAllocatingAudioFrameBuffer(Math.max(bufferDuration, MIN_FRAME_BUFFER_MS), format, stopping));
        this.playerManager.setItemLoaderThreadPoolSize(8);

        YoutubeAudioSourceManager youtubeSourceManager = new YoutubeAudioSourceManager(true,
                new Web(), new AndroidVr()
        );

        if (!config.youtubePoToken().isBlank() && !config.youtubeVisitorData().isBlank()) {
            Web.setPoTokenAndVisitorData(config.youtubePoToken(), config.youtubeVisitorData());
        }

        playerManager.registerSourceManager(youtubeSourceManager);
        AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public void loadAndPlay(TextChannel channel, Member member, String identifier) {
        I18n localI18n = guildI18n(channel.getGuild());
        stopCleanupUntilMillis.remove(channel.getGuild().getIdLong());
        String resolvedIdentifier = normalizeIdentifier(identifier);
        lastTextChannels.put(channel.getGuild().getIdLong(), channel);
        VoiceChannel voiceChannel = getUserVoiceChannel(member);
        if (voiceChannel == null) {
            channel.sendMessage(localI18n.t("join.voice")).queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        connect(channel.getGuild(), voiceChannel, musicManager);

        GuildVoiceState selfVoiceState = channel.getGuild().getSelfMember().getVoiceState();
        if (selfVoiceState != null && selfVoiceState.isGuildMuted()) {
            channel.sendMessage(localI18n.t("join.serverMuted")).queue();
        }

        channel.sendMessage(localI18n.t("loading", identifier)).queue();
        playerManager.loadItemOrdered(musicManager, resolvedIdentifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage(localI18n.t("queued", track.getInfo().title)).queue(
                    ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
                    ignored -> {
                    }
                );
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (isSearchIdentifier(resolvedIdentifier)) {
                    presentSearchChoices(channel, member, identifier, playlist, musicManager);
                    return;
                }

                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && !playlist.getTracks().isEmpty()) {
                    track = playlist.getTracks().get(0);
                }

                if (track == null) {
                    channel.sendMessage(localI18n.t("playlist.empty")).queue();
                    disconnectIfIdle(channel, musicManager);
                    return;
                }

                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage(localI18n.t("queued.playlist", track.getInfo().title)).queue(
                    ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
                    ignored -> {
                    }
                );
            }

            @Override
            public void noMatches() {
                noMatchesCount.incrementAndGet();
                channel.sendMessage(localI18n.t("nothing.found", identifier)).queue();
                disconnectIfIdle(channel, musicManager);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                loadFailureCount.incrementAndGet();
                channel.sendMessage(buildLoadFailureMessage(identifier, exception)).queue();
                disconnectIfIdle(channel, musicManager);
            }
        });
    }

    public void enqueueFromControlPanel(TextChannel channel, String identifier) {
        I18n localI18n = guildI18n(channel.getGuild());
        stopCleanupUntilMillis.remove(channel.getGuild().getIdLong());
        String resolvedIdentifier = normalizeIdentifier(identifier);
        lastTextChannels.put(channel.getGuild().getIdLong(), channel);

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        var connectedChannel = channel.getGuild().getAudioManager().getConnectedChannel();
        VoiceChannel voiceChannel;
        if (connectedChannel instanceof VoiceChannel existingVoiceChannel) {
            voiceChannel = existingVoiceChannel;
        } else {
            voiceChannel = resolveDesktopVoiceChannel(channel.getGuild());
            if (voiceChannel == null) {
                channel.sendMessage(localI18n.t("join.voice.desktop")).queue();
                return;
            }
            channel.sendMessage(localI18n.t("join.voice.desktop.connected", voiceChannel.getName())).queue();
        }

        connect(channel.getGuild(), voiceChannel, musicManager);

        channel.sendMessage(localI18n.t("loading", identifier)).queue();
        playerManager.loadItemOrdered(musicManager, resolvedIdentifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage(localI18n.t("queued", track.getInfo().title)).queue(
                        ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
                        ignored -> {
                        }
                );
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && !playlist.getTracks().isEmpty()) {
                    track = playlist.getTracks().get(0);
                }

                if (track == null) {
                    channel.sendMessage(localI18n.t("playlist.empty")).queue();
                    disconnectIfIdle(channel, musicManager);
                    return;
                }

                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage(localI18n.t("queued", track.getInfo().title)).queue(
                        ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
                        ignored -> {
                        }
                );
            }

            @Override
            public void noMatches() {
                noMatchesCount.incrementAndGet();
                channel.sendMessage(localI18n.t("nothing.found", identifier)).queue();
                disconnectIfIdle(channel, musicManager);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                loadFailureCount.incrementAndGet();
                channel.sendMessage(buildLoadFailureMessage(identifier, exception)).queue();
                disconnectIfIdle(channel, musicManager);
            }
        });
    }

    public List<SearchTrackOption> searchTopTracksForDesktop(String query, int limit) {
        String normalized = normalizeIdentifier(query);
        if (!isSearchIdentifier(normalized)) {
            normalized = "ytsearch:" + query.trim();
        }

        int maxResults = Math.max(1, Math.min(limit, 10));
        List<SearchTrackOption> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicLong errorCode = new AtomicLong(0L);
        final String[] failureMessage = {""};

        playerManager.loadItem(normalized, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                results.add(new SearchTrackOption(track.getInfo().title, track.getInfo().uri));
                latch.countDown();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                for (int i = 0; i < tracks.size() && i < maxResults; i++) {
                    AudioTrack track = tracks.get(i);
                    results.add(new SearchTrackOption(track.getInfo().title, track.getInfo().uri));
                }
                latch.countDown();
            }

            @Override
            public void noMatches() {
                latch.countDown();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                failed.set(true);
                errorCode.set(1L);
                failureMessage[0] = exception.getMessage() == null ? "unknown error" : exception.getMessage();
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(12, TimeUnit.SECONDS);
            if (!completed) {
                throw new IllegalStateException("Search timed out. Please try again.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Search interrupted.");
        }

        if (failed.get() && errorCode.get() == 1L) {
            throw new IllegalStateException("Search failed: " + failureMessage[0]);
        }

        return results;
    }

    public void sendPlayerPanelFromDesktop(TextChannel channel) {
        String prefix = settingsStore.get(channel.getGuild().getIdLong()).prefix();
        sendPlayerPanel(channel, prefix);
    }

    public SearchSelectionOutcome chooseSearchResult(TextChannel channel, Member member, String selectionId, int index) {
        PendingSearch pendingSearch = pendingSearches.get(selectionId);
        if (pendingSearch == null) {
            return SearchSelectionOutcome.error(guildI18n(channel.getGuild()).t("search.expired"));
        }

        if (!canUsePendingSearch(member, pendingSearch)) {
            return SearchSelectionOutcome.error(guildI18n(channel.getGuild()).t("search.owner.only.choose"));
        }

        if (index < 0 || index >= pendingSearch.tracks().size()) {
            return SearchSelectionOutcome.error(guildI18n(channel.getGuild()).t("search.invalid"));
        }

        AudioTrack track = pendingSearch.tracks().get(index).makeClone();
        pendingSearches.remove(selectionId);

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.scheduler.queue(track);
        lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
        loadSuccessCount.incrementAndGet();
        channel.sendMessage(guildI18n(channel.getGuild()).t("queued", track.getInfo().title)).queue(
            ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
            ignored -> {
            }
        );
        return SearchSelectionOutcome.success(guildI18n(channel.getGuild()).t("search.selected", track.getInfo().title));
    }

    public SearchSelectionOutcome cancelSearchSelection(TextChannel channel, Member member, String selectionId) {
        PendingSearch pendingSearch = pendingSearches.get(selectionId);
        if (pendingSearch == null) {
            return SearchSelectionOutcome.error(guildI18n(channel.getGuild()).t("search.expired"));
        }

        if (!canUsePendingSearch(member, pendingSearch)) {
            return SearchSelectionOutcome.error(guildI18n(channel.getGuild()).t("search.owner.only.cancel"));
        }

        pendingSearches.remove(selectionId);
        disconnectIfIdle(channel, getGuildMusicManager(channel.getGuild()));
        return SearchSelectionOutcome.success(guildI18n(channel.getGuild()).t("search.cancelled"));
    }

    public MetricsSnapshot metricsSnapshot() {
        int trackedGuilds = musicManagers.size();
        int activePlayers = 0;
        int queuedTracks = 0;

        for (GuildMusicManager manager : musicManagers.values()) {
            if (manager.player.getPlayingTrack() != null) {
                activePlayers++;
            }
            queuedTracks += manager.scheduler.getQueue().size();
        }

        String nowPlayingTitle = "none";
        long nowPlayingPositionMs = 0L;
        long nowPlayingDurationMs = 0L;
        String nowPlayingState = "idle";

        for (GuildMusicManager manager : musicManagers.values()) {
            AudioTrack current = manager.player.getPlayingTrack();
            if (current == null) {
                continue;
            }

            nowPlayingTitle = current.getInfo().title;
            nowPlayingPositionMs = manager.getCalculatedPositionMs();
            nowPlayingDurationMs = current.getDuration();
            nowPlayingState = manager.player.isPaused() ? "paused" : "playing";
            break;
        }

        return new MetricsSnapshot(
                trackedGuilds,
                activePlayers,
                queuedTracks,
                loadSuccessCount.get(),
                loadFailureCount.get(),
                noMatchesCount.get(),
                nowPlayingTitle,
                nowPlayingPositionMs,
                nowPlayingDurationMs,
                nowPlayingState
        );
    }

    public long preferredTextChannelId(long guildId) {
        Long trackedChannel = playerPanelChannelIds.get(guildId);
        if (trackedChannel != null) {
            return trackedChannel;
        }
        TextChannel last = lastTextChannels.get(guildId);
        return last == null ? 0L : last.getIdLong();
    }

    public String desktopPlayerSummary(long guildId) {
        GuildMusicManager manager = musicManagers.get(guildId);
        if (manager == null) {
            return "State: idle\nTrack: none\nQueue: empty";
        }

        AudioTrack current = manager.player.getPlayingTrack();
        String state = current == null ? "idle" : (manager.player.isPaused() ? "paused" : "playing");
        String track = current == null ? "none" : current.getInfo().title;

        StringBuilder summary = new StringBuilder()
                .append("State: ").append(state)
                .append("\nTrack: ").append(track)
                .append("\nQueue:\n");

        if (manager.scheduler.getQueue().isEmpty()) {
            summary.append("(empty)");
            return summary.toString();
        }

        int index = 1;
        for (AudioTrack queued : manager.scheduler.getQueue()) {
            summary.append(index++).append(". ").append(queued.getInfo().title).append('\n');
            if (index > 8) {
                summary.append("...");
                break;
            }
        }

        return summary.toString().trim();
    }

    public void skip(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack next = musicManager.scheduler.skip();
        if (next == null) {
            channel.sendMessage(localI18n.t("queue.empty")).queue();
        } else {
            channel.sendMessage(localI18n.t("skip.now", next.getInfo().title)).queue();
        }
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void pause(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();

        if (current == null) {
            channel.sendMessage(localI18n.t("play.none")).queue();
            return;
        }

        if (musicManager.player.isPaused()) {
            channel.sendMessage(localI18n.t("pause.already")).queue();
            return;
        }

        musicManager.markTrackPaused();
        musicManager.player.setPaused(true);
        channel.sendMessage(localI18n.t("pause.done")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void resume(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();

        if (current == null) {
            channel.sendMessage(localI18n.t("play.none")).queue();
            return;
        }

        if (!musicManager.player.isPaused()) {
            channel.sendMessage(localI18n.t("resume.already")).queue();
            return;
        }

        musicManager.markTrackResumed();
        musicManager.player.setPaused(false);
        channel.sendMessage(localI18n.t("resume.done")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void stop(TextChannel channel) {
        long guildId = channel.getGuild().getIdLong();
        stopCleanupUntilMillis.put(guildId, System.currentTimeMillis() + 10_000L);
        clearPersistentPlayerPanel(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.scheduler.stop();
        channel.getGuild().getAudioManager().closeAudioConnection();
        updatePresence(channel.getGuild());
        cleanupRecentChat(channel);
        // Run a second pass shortly after stop to catch straggler messages posted asynchronously.
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> cleanupRecentChat(channel));
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> cleanupRecentChat(channel));
    }

    public void setVolume(TextChannel channel, int volume) {
        I18n localI18n = guildI18n(channel.getGuild());
        if (volume < 0 || volume > MAX_VOLUME) {
            channel.sendMessage(localI18n.t("volume.range")).queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(volume);
        channel.sendMessage(localI18n.t("volume.set", volume)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void adjustVolume(TextChannel channel, int delta) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int current = musicManager.player.getVolume();
        int next = Math.max(0, Math.min(MAX_VOLUME, current + delta));
        musicManager.player.setVolume(next);
        channel.sendMessage(localI18n.t("volume.set", next)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void showVolume(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        channel.sendMessage(localI18n.t("volume.current", musicManager.player.getVolume())).queue();
    }

    public void setBass(TextChannel channel, int level) {
        I18n localI18n = guildI18n(channel.getGuild());
        if (level < 0 || level > MAX_BASS) {
            channel.sendMessage(localI18n.t("bass.range")).queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        applyBassBoost(musicManager, level);
        channel.sendMessage(localI18n.t("bass.set", level)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void adjustBass(TextChannel channel, int delta) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int current = musicManager.getBassLevel();
        int next = Math.max(0, Math.min(MAX_BASS, current + delta));
        applyBassBoost(musicManager, next);
        channel.sendMessage(localI18n.t("bass.set", next)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void showBass(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        channel.sendMessage(localI18n.t("bass.current", musicManager.getBassLevel())).queue();
    }

    public void resetNormal(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(DEFAULT_VOLUME);
        applyBassBoost(musicManager, 0);
        channel.sendMessage(localI18n.t("reset.done")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setLoudPreset(TextChannel channel, String prefix) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(MAX_VOLUME);
        applyBassBoost(musicManager, MAX_BASS);
        channel.sendMessage(localI18n.t("loud.done", prefix)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setEarRapeHostOnly(TextChannel channel, boolean enabled) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        if (enabled) {
            musicManager.player.setVolume(MAX_VOLUME);
            applyBassBoost(musicManager, MAX_BASS);
        } else {
            musicManager.player.setVolume(DEFAULT_VOLUME);
            applyBassBoost(musicManager, 0);
        }
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setEarRapeHostOnly(long guildId, boolean enabled) {
        GuildMusicManager musicManager = musicManagers.get(guildId);
        if (musicManager == null) {
            return;
        }

        if (enabled) {
            musicManager.player.setVolume(MAX_VOLUME);
            applyBassBoost(musicManager, MAX_BASS);
        } else {
            musicManager.player.setVolume(DEFAULT_VOLUME);
            applyBassBoost(musicManager, 0);
        }
    }

    public void queue(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();
        StringBuilder builder = new StringBuilder();
        if (current != null) {
            builder.append(localI18n.t("now.playing", current.getInfo().title)).append('\n');
        }
        if (musicManager.scheduler.getQueue().isEmpty()) {
            builder.append(localI18n.t("queue.empty"));
        } else {
            builder.append(localI18n.t("upcoming")).append('\n');
            int index = 1;
            for (AudioTrack track : musicManager.scheduler.getQueue()) {
                builder.append(index++).append(". ").append(track.getInfo().title).append('\n');
                if (index > 10) {
                    builder.append("...");
                    break;
                }
            }
        }
        channel.sendMessage(builder.toString()).queue();
    }

    public void remove(TextChannel channel, int index) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        boolean removed = musicManager.scheduler.removeAt(index);
        channel.sendMessage(removed ? localI18n.t("queue.removed", index) : localI18n.t("queue.invalidIndex")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void clearQueue(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int removed = musicManager.scheduler.clearQueue();
        channel.sendMessage(localI18n.t("queue.cleared", removed)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void shuffleQueue(TextChannel channel) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int shuffled = musicManager.scheduler.shuffleQueue();
        channel.sendMessage(shuffled == 0 ? localI18n.t("queue.empty") : localI18n.t("queue.shuffled", shuffled)).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setLoop(TextChannel channel, String mode) {
        I18n localI18n = guildI18n(channel.getGuild());
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        String normalized = mode == null ? "off" : mode.trim().toLowerCase();

        switch (normalized) {
            case "track" -> {
                musicManager.scheduler.setLoopTrack(true);
                channel.sendMessage(localI18n.t("loop.track")).queue();
            }
            case "queue" -> {
                musicManager.scheduler.setLoopQueue(true);
                channel.sendMessage(localI18n.t("loop.queue")).queue();
            }
            default -> {
                musicManager.scheduler.setLoopTrack(false);
                musicManager.scheduler.setLoopQueue(false);
                channel.sendMessage(localI18n.t("loop.off")).queue();
            }
        }
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void seek(TextChannel channel, long seconds) {
        I18n localI18n = guildI18n(channel.getGuild());
        if (seconds < 0) {
            channel.sendMessage(localI18n.t("seek.nonNegative")).queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        boolean ok = musicManager.scheduler.seekTo(seconds * 1000L);
        channel.sendMessage(ok ? localI18n.t("seek.done", seconds) : localI18n.t("play.none")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setAutoplay(TextChannel channel, boolean enabled) {
        I18n localI18n = guildI18n(channel.getGuild());
        long guildId = channel.getGuild().getIdLong();
        GuildSettings current = settingsStore.get(guildId);
        settingsStore.upsert(new GuildSettings(
                current.guildId(),
                current.prefix(),
                current.language(),
                current.djRoleId(),
                current.defaultVolume(),
                enabled,
                current.commandChannelId(),
                current.blockedRoleId()
        ));
        channel.sendMessage(enabled ? localI18n.t("autoplay.enabled") : localI18n.t("autoplay.disabled")).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public String healthSummary() {
        int guildCount = musicManagers.size();
        int activePlayers = 0;
        for (GuildMusicManager manager : musicManagers.values()) {
            if (manager.player.getPlayingTrack() != null) {
                activePlayers++;
            }
        }

        return String.join("\n",
                "Health:",
                "trackedGuilds=" + guildCount,
                "activePlayers=" + activePlayers,
            "managedQueues=" + guildCount,
            "panelCreates=" + panelCreateCount.get(),
            "panelEdits=" + panelEditCount.get(),
            "panelSkips=" + panelSkipCount.get());
    }

    public void sendPlayerPanel(TextChannel channel, String prefix) {
        refreshOrCreatePlayerPanel(channel, prefix);
        startPlayerPanelAutoRefresh(channel.getGuild());
    }

    public void refreshPlayerPanel(Message message, String prefix) {
        if (message.getChannel() instanceof TextChannel channel) {
            long guildId = channel.getGuild().getIdLong();
            playerPanelChannelIds.put(guildId, channel.getIdLong());
            playerPanelMessageIds.put(guildId, message.getIdLong());
            refreshOrCreatePlayerPanel(channel, prefix);
        }
    }

    public void refreshPresenceForGuild(Guild guild) {
        updatePresence(guild);
    }

    public void debugAudio(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();
        AudioManager audioManager = channel.getGuild().getAudioManager();
        GuildVoiceState selfVoiceState = channel.getGuild().getSelfMember().getVoiceState();

        String nowPlaying = current == null ? "none" : current.getInfo().title;
        long provided = musicManager.sendHandler.getProvidedFrames();
        long calls = musicManager.sendHandler.getProvideCalls();
        long lastFrameMs = musicManager.sendHandler.getMillisSinceLastFrame();
        var connectedChannelRef = audioManager.getConnectedChannel();
        String connectedChannel = connectedChannelRef == null
            ? "none"
            : connectedChannelRef.getName() + " (" + connectedChannelRef.getType() + ")";

        boolean guildMuted = selfVoiceState != null && selfVoiceState.isGuildMuted();
        boolean muted = selfVoiceState != null && selfVoiceState.isMuted();
        boolean suppressed = selfVoiceState != null && selfVoiceState.isSuppressed();
        boolean deafened = selfVoiceState != null && selfVoiceState.isDeafened();

        String message = String.join("\n",
                "Audio debug:",
                "connected=" + audioManager.isConnected(),
            "connectedChannel=" + connectedChannel,
                "selfMuted=" + audioManager.isSelfMuted(),
                "selfDeafened=" + audioManager.isSelfDeafened(),
            "guildMuted=" + guildMuted,
            "muted=" + muted,
            "deafened=" + deafened,
            "suppressed=" + suppressed,
                "nowPlaying=" + nowPlaying,
                "provideCalls=" + calls,
                "providedFrames=" + provided,
                "msSinceLastFrame=" + (lastFrameMs < 0 ? "never" : lastFrameMs),
                "lastCodec=" + musicManager.sendHandler.getLastCodecName(),
                "lastFrameBytes=" + musicManager.sendHandler.getLastDataLength(),
                "paused=" + musicManager.player.isPaused(),
                "volume=" + musicManager.player.getVolume(),
                "bassLevel=" + musicManager.getBassLevel(),
                "panelCreates=" + panelCreateCount.get(),
                "panelEdits=" + panelEditCount.get(),
                "panelSkips=" + panelSkipCount.get());

        channel.sendMessage(message).queue();
    }

    private void applyBassBoost(GuildMusicManager musicManager, int level) {
        if (level <= 0) {
            musicManager.player.setFilterFactory(null);
            musicManager.setBassLevel(0);
            return;
        }

        float gain = Math.min(0.15f * level, 0.75f);
        EqualizerFactory equalizer = new EqualizerFactory();
        equalizer.setGain(0, gain);
        equalizer.setGain(1, gain * 0.9f);
        equalizer.setGain(2, gain * 0.8f);
        equalizer.setGain(3, gain * 0.5f);
        equalizer.setGain(4, gain * 0.25f);
        musicManager.player.setFilterFactory(equalizer);
        musicManager.setBassLevel(level);
    }

    private GuildMusicManager getGuildMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
            java.util.concurrent.atomic.AtomicReference<GuildMusicManager> managerRef = new java.util.concurrent.atomic.AtomicReference<>();
            GuildMusicManager musicManager = new GuildMusicManager(
                    playerManager,
                    track -> {
                        GuildMusicManager manager = managerRef.get();
                        if (manager != null) {
                            manager.markTrackStarted();
                        }
                        startPlayerPanelAutoRefresh(guild);
                        updatePresence(guild);
                        lastTrackQueries.put(guild.getIdLong(), track.getInfo().title);
                        refreshPersistentPlayerPanel(guild);
                    },
                    () -> {
                        updatePresence(guild);
                        maybeAutoplay(guild, musicManagerRef(guild));
                        refreshPersistentPlayerPanel(guild);
                    }
            );
            managerRef.set(musicManager);

            int defaultVolume = settingsStore.get(guild.getIdLong()).defaultVolume();
            musicManager.player.setVolume(Math.max(0, Math.min(MAX_VOLUME, defaultVolume)));
            guild.getAudioManager().setSendingHandler(musicManager.sendHandler);
            return musicManager;
        });
    }

    private void updatePresence(Guild guild) {
        GuildMusicManager manager = musicManagerRef(guild);
        AudioTrack track = manager == null ? null : manager.player.getPlayingTrack();
        String title = track == null ? null : track.getInfo().title;

        I18n presenceI18n = guildI18n(guild);
        if (title == null || title.isBlank()) {
            guild.getJDA().getPresence().setActivity(Activity.playing(presenceI18n.t("status.waiting")));
        } else {
            guild.getJDA().getPresence().setActivity(Activity.playing(presenceI18n.t("status.playing", title)));
        }
    }

    private I18n guildI18n(Guild guild) {
        return new I18n(settingsStore.get(guild.getIdLong()).language());
    }

    private MessageEmbed buildPlayerEmbed(Guild guild, String prefix) {
        I18n localI18n = guildI18n(guild);
        GuildMusicManager musicManager = getGuildMusicManager(guild);
        AudioTrack current = musicManager.player.getPlayingTrack();

        String state = current == null
                ? localI18n.t("player.state.idle")
                : musicManager.player.isPaused()
                    ? localI18n.t("player.state.paused")
                    : localI18n.t("player.state.playing");

        String trackValue = current == null
            ? localI18n.t("player.none")
            : buildTrackValue(musicManager, current);

        var connectedChannel = guild.getAudioManager().getConnectedChannel();
        String voiceValue = connectedChannel == null ? localI18n.t("player.notConnected") : connectedChannel.getName();
        String queueValue = buildQueuePreview(musicManager, localI18n);

        return new EmbedBuilder()
                .setTitle(localI18n.t("player.title"))
                .setDescription(localI18n.t("player.hint", prefix))
                .setColor(current == null ? new Color(120, 120, 120) : new Color(46, 204, 113))
                .addField(localI18n.t("player.status"), state, true)
                .addField(localI18n.t("player.voice"), voiceValue, true)
                .addField(localI18n.t("player.volume"), musicManager.player.getVolume() + "%", true)
                .addField(localI18n.t("player.track"), trackValue, false)
                .addField(localI18n.t("player.queuePreview"), queueValue, false)
                .addField(localI18n.t("player.bass"), String.valueOf(musicManager.getBassLevel()), true)
                .setFooter(localI18n.t("player.footer", prefix))
                .build();
    }

    private String buildTrackValue(GuildMusicManager musicManager, AudioTrack current) {
        long position = musicManager.getCalculatedPositionMs();
        long duration = current.getDuration();

        if (duration >= 0) {
            position = Math.max(0L, Math.min(position, duration));
        } else {
            position = Math.max(0L, position);
        }

        return new StringBuilder(current.getInfo().title)
                .append("\n")
                .append(formatDuration(position))
                .append(" / ")
                .append(formatDuration(duration))
                .toString();
    }

    private void refreshOrCreatePlayerPanel(TextChannel channel, String prefix) {
        playerPanelChannelIds.put(channel.getGuild().getIdLong(), channel.getIdLong());
        enqueuePlayerPanelRefresh(channel.getGuild(), prefix);
    }

    private void enqueuePlayerPanelRefresh(Guild guild, String prefix) {
        long guildId = guild.getIdLong();
        AtomicBoolean inFlight = playerPanelRefreshInFlight.computeIfAbsent(guildId, ignored -> new AtomicBoolean(false));
        AtomicBoolean pending = playerPanelRefreshPending.computeIfAbsent(guildId, ignored -> new AtomicBoolean(false));

        if (!inFlight.compareAndSet(false, true)) {
            pending.set(true);
            return;
        }

        TextChannel channel = resolvePlayerPanelChannel(guild);
        if (channel == null) {
            inFlight.set(false);
            return;
        }

        performPlayerPanelRefresh(guild, channel, prefix, inFlight, pending);
    }

    private void performPlayerPanelRefresh(
            Guild guild,
            TextChannel channel,
            String prefix,
            AtomicBoolean inFlight,
            AtomicBoolean pending
    ) {
        long guildId = channel.getGuild().getIdLong();
        String nextSignature = buildPlayerPanelSignature(channel.getGuild(), prefix);

        Long messageId = playerPanelMessageIds.get(guildId);
        String lastSignature = playerPanelLastRenderedSignature.get(guildId);
        if (messageId != null && nextSignature.equals(lastSignature)) {
            panelSkipCount.incrementAndGet();
            finishPlayerPanelRefresh(guild, inFlight, pending);
            return;
        }

        if (messageId != null) {
            channel.retrieveMessageById(messageId).queue(existing ->
                    existing.editMessageEmbeds(buildPlayerEmbed(channel.getGuild(), prefix))
                            .setComponents(playerComponents(guildI18n(channel.getGuild())))
                            .queue(
                        ignored -> {
                        panelEditCount.incrementAndGet();
                        playerPanelLastRenderedSignature.put(guildId, nextSignature);
                        finishPlayerPanelRefresh(guild, inFlight, pending);
                        },
                                    ignored -> postNewPlayerPanel(guild, channel, prefix, inFlight, pending)
                            ),
                    ignored -> postNewPlayerPanel(guild, channel, prefix, inFlight, pending)
            );
            return;
        }

        postNewPlayerPanel(guild, channel, prefix, inFlight, pending);
    }

    private void postNewPlayerPanel(
            Guild guild,
            TextChannel channel,
            String prefix,
            AtomicBoolean inFlight,
            AtomicBoolean pending
    ) {
        long guildId = channel.getGuild().getIdLong();
        channel.sendMessageEmbeds(buildPlayerEmbed(channel.getGuild(), prefix))
            .setComponents(playerComponents(guildI18n(channel.getGuild())))
                .queue(message -> {
                    panelCreateCount.incrementAndGet();
                    playerPanelChannelIds.put(guildId, channel.getIdLong());
                    playerPanelMessageIds.put(guildId, message.getIdLong());
                    playerPanelLastRenderedSignature.put(guildId, buildPlayerPanelSignature(channel.getGuild(), prefix));
                    finishPlayerPanelRefresh(guild, inFlight, pending);
                }, ignored -> finishPlayerPanelRefresh(guild, inFlight, pending));
    }

    private void finishPlayerPanelRefresh(Guild guild, AtomicBoolean inFlight, AtomicBoolean pending) {
        inFlight.set(false);
        if (!pending.getAndSet(false)) {
            return;
        }

        if (!inFlight.compareAndSet(false, true)) {
            return;
        }

        TextChannel nextChannel = resolvePlayerPanelChannel(guild);
        if (nextChannel == null) {
            inFlight.set(false);
            return;
        }

        String prefix = settingsStore.get(guild.getIdLong()).prefix();
        performPlayerPanelRefresh(guild, nextChannel, prefix, inFlight, pending);
    }

    private void refreshPersistentPlayerPanel(Guild guild) {
        TextChannel channel = resolvePlayerPanelChannel(guild);
        if (channel == null) {
            return;
        }

        String prefix = settingsStore.get(guild.getIdLong()).prefix();
        refreshOrCreatePlayerPanel(channel, prefix);
    }

    private TextChannel resolvePlayerPanelChannel(Guild guild) {
        long guildId = guild.getIdLong();
        Long channelId = playerPanelChannelIds.get(guildId);
        if (channelId != null) {
            TextChannel tracked = guild.getTextChannelById(channelId);
            if (tracked != null) {
                return tracked;
            }
        }
        return null;
    }

    private void deleteTrackedPlayerPanel(Guild guild, long guildId) {
        Long messageId = playerPanelMessageIds.remove(guildId);
        Long channelId = playerPanelChannelIds.remove(guildId);
        if (messageId == null || channelId == null) {
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        channel.deleteMessageById(messageId).queue(
                ignored -> {
                },
                ignored -> {
                }
        );
    }

    private void clearPersistentPlayerPanel(Guild guild) {
        long guildId = guild.getIdLong();
        deleteTrackedPlayerPanel(guild, guildId);
        playerPanelRefreshInFlight.remove(guildId);
        playerPanelRefreshPending.remove(guildId);
        playerPanelLastAutoRefreshMillis.remove(guildId);
        playerPanelLastRenderedSignature.remove(guildId);
        stopPlayerPanelAutoRefresh(guildId);
    }

    private void startPlayerPanelAutoRefresh(Guild guild) {
        long guildId = guild.getIdLong();
        playerPanelAutoRefreshTasks.compute(guildId, (ignored, existing) -> {
            if (existing != null && !existing.isCancelled() && !existing.isDone()) {
                return existing;
            }

            return playerPanelRefresher.scheduleAtFixedRate(() -> {
                GuildMusicManager manager = musicManagerRef(guild);
                if (manager == null) {
                    stopPlayerPanelAutoRefresh(guildId);
                    return;
                }

                AudioTrack current = manager.player.getPlayingTrack();
                if (current == null) {
                    stopPlayerPanelAutoRefresh(guildId);
                    return;
                }

                long refreshIntervalMs = manager.player.isPaused()
                        ? PLAYER_PANEL_PAUSED_REFRESH_MS
                        : PLAYER_PANEL_PLAYING_REFRESH_MS;
                long now = System.currentTimeMillis();
                long lastRefresh = playerPanelLastAutoRefreshMillis.getOrDefault(guildId, 0L);
                if (now - lastRefresh < refreshIntervalMs) {
                    return;
                }

                playerPanelLastAutoRefreshMillis.put(guildId, now);
                refreshPersistentPlayerPanel(guild);
            }, PLAYER_PANEL_REFRESH_TICK_SECONDS, PLAYER_PANEL_REFRESH_TICK_SECONDS, TimeUnit.SECONDS);
        });
    }

    private void stopPlayerPanelAutoRefresh(long guildId) {
        ScheduledFuture<?> task = playerPanelAutoRefreshTasks.remove(guildId);
        playerPanelLastAutoRefreshMillis.remove(guildId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void cleanupRecentChat(TextChannel channel) {
        channel.getHistory().retrievePast(BULK_DELETE_LIMIT).queue(messages -> {
                    if (messages.isEmpty()) {
                        return;
                    }

                    fastDeleteBatch(channel, messages);

                    if (messages.size() == BULK_DELETE_LIMIT) {
                        String beforeId = messages.get(messages.size() - 1).getId();
                        cleanupRecentChatBefore(channel, beforeId);
                    }
                },
                ignored -> {
                }
        );
    }

    private void cleanupRecentChatBefore(TextChannel channel, String beforeId) {
        channel.getHistoryBefore(beforeId, BULK_DELETE_LIMIT).queue(history -> {
                    List<Message> messages = history.getRetrievedHistory();
                    if (messages.isEmpty()) {
                        return;
                    }

                    fastDeleteBatch(channel, messages);

                    if (messages.size() == BULK_DELETE_LIMIT) {
                        String nextBeforeId = messages.get(messages.size() - 1).getId();
                        cleanupRecentChatBefore(channel, nextBeforeId);
                    }
                },
                ignored -> {
                }
        );
    }

    private void fastDeleteBatch(TextChannel channel, List<Message> messages) {
        OffsetDateTime bulkCutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(14);
        List<Message> bulkEligible = new ArrayList<>();
        List<Message> fallback = new ArrayList<>();

        for (Message message : messages) {
            if (message.getTimeCreated().isAfter(bulkCutoff)) {
                bulkEligible.add(message);
            } else {
                fallback.add(message);
            }
        }

        if (bulkEligible.size() >= 2) {
            channel.deleteMessages(bulkEligible).queue(
                    ignored -> {
                    },
                    ignored -> {
                    }
            );
        } else if (bulkEligible.size() == 1) {
            bulkEligible.get(0).delete().queue(
                    ignored -> {
                    },
                    ignored -> {
                    }
            );
        }

        fallback.forEach(message -> message.delete().queue(
                ignored -> {
                },
                ignored -> {
                }
        ));
    }

    private List<MessageTopLevelComponent> playerComponents(I18n localI18n) {
        return List.of(
                ActionRow.of(
                Button.primary("player:pause", localI18n.t("player.pause")),
                        Button.success("player:resume", localI18n.t("player.resume")),
                        Button.primary("player:skip", localI18n.t("player.skip")),
                        Button.danger("player:stop", localI18n.t("player.stop"))
                ),
                ActionRow.of(
                        Button.secondary("player:voldown", localI18n.t("player.voldown")),
                        Button.secondary("player:volup", localI18n.t("player.volup")),
                    Button.secondary("player:bassdown", localI18n.t("player.bassdown")),
                    Button.secondary("player:bassup", localI18n.t("player.bassup"))
                ),
                ActionRow.of(
                    Button.secondary("player:bassreset", localI18n.t("player.bassreset")),
                        Button.secondary("player:refresh", localI18n.t("player.refresh"))
                )
        );
    }

    private String buildPlayerPanelSignature(Guild guild, String prefix) {
        GuildMusicManager musicManager = getGuildMusicManager(guild);
        AudioTrack current = musicManager.player.getPlayingTrack();

        String state = current == null
                ? "idle"
                : (musicManager.player.isPaused() ? "paused" : "playing");
        String trackTitle = current == null ? "none" : current.getInfo().title;
        String trackUri = current == null ? "none" : String.valueOf(current.getInfo().uri);
        long positionBucket = current == null ? 0L : (musicManager.getCalculatedPositionMs() / 1000L);
        long duration = current == null ? 0L : current.getDuration();
        String queuePreview = buildQueuePreview(musicManager, guildI18n(guild));

        return String.join("|",
                prefix,
                state,
                trackTitle,
                trackUri,
                Long.toString(positionBucket),
                Long.toString(duration),
                Integer.toString(musicManager.player.getVolume()),
                Integer.toString(musicManager.getBassLevel()),
                queuePreview
        );
    }

    private String buildQueuePreview(GuildMusicManager musicManager, I18n localI18n) {
        if (musicManager.scheduler.getQueue().isEmpty()) {
            return localI18n.t("player.queueEmpty");
        }

        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (AudioTrack track : musicManager.scheduler.getQueue()) {
            builder.append(index++).append(". ").append(track.getInfo().title).append('\n');
            if (index > 5) {
                builder.append("...");
                break;
            }
        }

        return builder.toString().trim();
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis < 0) {
            return "live";
        }

        long totalSeconds = durationMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%d:%02d", minutes, seconds);
    }

    private void maybeAutoplay(Guild guild, GuildMusicManager musicManager) {
        if (musicManager == null || musicManager.player.getPlayingTrack() != null || !musicManager.scheduler.getQueue().isEmpty()) {
            return;
        }

        if (isStopCleanupActive(guild.getIdLong())) {
            return;
        }

        GuildSettings settings = settingsStore.get(guild.getIdLong());
        if (!settings.autoplay()) {
            return;
        }

        String baseQuery = lastTrackQueries.get(guild.getIdLong());
        TextChannel announceChannel = lastTextChannels.get(guild.getIdLong());
        if (baseQuery == null || baseQuery.isBlank() || announceChannel == null) {
            return;
        }

        String query = "ytsearch:" + baseQuery;
        playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (isStopCleanupActive(guild.getIdLong())) {
                    return;
                }
                musicManager.scheduler.queue(track);
                lastTrackQueries.put(guild.getIdLong(), track.getInfo().title);
                announceChannel.sendMessage(guildI18n(guild).t("autoplay.queued", track.getInfo().title)).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack selected = playlist.getSelectedTrack();
                if (selected == null && !playlist.getTracks().isEmpty()) {
                    selected = playlist.getTracks().get(0);
                }
                if (selected != null) {
                    if (isStopCleanupActive(guild.getIdLong())) {
                        return;
                    }
                    musicManager.scheduler.queue(selected);
                    lastTrackQueries.put(guild.getIdLong(), selected.getInfo().title);
                    announceChannel.sendMessage(guildI18n(guild).t("autoplay.queued", selected.getInfo().title)).queue();
                }
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        });
    }

    private boolean isStopCleanupActive(long guildId) {
        Long until = stopCleanupUntilMillis.get(guildId);
        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() < until) {
            return true;
        }

        stopCleanupUntilMillis.remove(guildId);
        return false;
    }

    private GuildMusicManager musicManagerRef(Guild guild) {
        return musicManagers.get(guild.getIdLong());
    }

    private void connect(Guild guild, VoiceChannel voiceChannel, GuildMusicManager musicManager) {
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        var connectedChannel = audioManager.getConnectedChannel();
        if (!audioManager.isConnected() || connectedChannel == null || !connectedChannel.getId().equals(voiceChannel.getId())) {
            audioManager.openAudioConnection(voiceChannel);
        }
    }

    private VoiceChannel getUserVoiceChannel(Member member) {
        if (member == null) {
            return null;
        }

        var voiceState = member.getVoiceState();
        if (voiceState == null) {
            return null;
        }

        var channel = voiceState.getChannel();
        if (channel instanceof VoiceChannel voiceChannel) {
            return voiceChannel;
        }

        return null;
    }

    private VoiceChannel resolveDesktopVoiceChannel(Guild guild) {
        for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
            boolean hasHumanMember = voiceChannel.getMembers().stream().anyMatch(member -> !member.getUser().isBot());
            if (hasHumanMember) {
                return voiceChannel;
            }
        }

        List<VoiceChannel> channels = guild.getVoiceChannels();
        if (channels.isEmpty()) {
            return null;
        }
        return channels.get(0);
    }

    private String buildLoadFailureMessage(String identifier, FriendlyException exception) {
        String message = exception.getMessage() == null ? "unknown error" : exception.getMessage();
        String lower = (identifier + " " + message).toLowerCase();

        String reason;
        String advice;
        if (lower.contains("429") || lower.contains("too many requests") || lower.contains("rate")
                || lower.contains("not a bot") || lower.contains("sign in") || lower.contains("captcha")) {
            reason = "Source temporarily limited requests.";
            advice = "Try again in a few minutes, or use a search query instead of a direct URL.";
        } else if (lower.contains("age") || lower.contains("restricted") || lower.contains("unavailable")) {
            reason = "The track may be age-restricted or unavailable in your region.";
            advice = "Try a different source or search query for an alternative upload.";
        } else if (lower.contains("private") || lower.contains("forbidden") || lower.contains("403")) {
            reason = "The track appears private or blocked for this client.";
            advice = "Try a public mirror/upload or search for the same song title.";
        } else {
            reason = "The source failed to load this item.";
            advice = "Retry, or use `play <search words>` to fall back to search mode.";
        }

        return String.join("\n",
                "Load failed for: " + identifier,
                reason,
                "Details: " + message,
                "Suggestion: " + advice);
    }

    private void presentSearchChoices(TextChannel channel, Member member, String query, AudioPlaylist playlist, GuildMusicManager musicManager) {
        List<AudioTrack> topTracks = playlist.getTracks().stream()
                .limit(3)
                .map(AudioTrack::makeClone)
                .toList();

        if (topTracks.isEmpty()) {
            channel.sendMessage(guildI18n(channel.getGuild()).t("nothing.found", query)).queue();
            disconnectIfIdle(channel, musicManager);
            return;
        }

        String selectionId = Long.toString(searchSelectionCounter.incrementAndGet(), 36);
        long requesterId = member == null ? 0L : member.getIdLong();
        pendingSearches.put(selectionId, new PendingSearch(requesterId, topTracks));

        StringBuilder builder = new StringBuilder("Search results for: ").append(query).append('\n');
        for (int index = 0; index < topTracks.size(); index++) {
            builder.append(index + 1).append(". ").append(topTracks.get(index).getInfo().title).append('\n');
        }
        builder.append("Choose a result below.");

        List<Button> buttons = List.of(
                Button.primary("searchpick:" + selectionId + ":0", "1"),
                Button.primary("searchpick:" + selectionId + ":1", "2").withDisabled(topTracks.size() < 2),
                Button.primary("searchpick:" + selectionId + ":2", "3").withDisabled(topTracks.size() < 3),
                Button.danger("searchcancel:" + selectionId, "Cancel")
        );

        channel.sendMessage(builder.toString())
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private boolean canUsePendingSearch(Member member, PendingSearch pendingSearch) {
        return pendingSearch.requesterId() == 0L || (member != null && member.getIdLong() == pendingSearch.requesterId());
    }

    private String normalizeIdentifier(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("www.")
                || lower.startsWith("ytsearch:")
                || lower.startsWith("ytmsearch:")
                || lower.startsWith("scsearch:")) {
            return trimmed;
        }

        return "ytsearch:" + trimmed;
    }

    private boolean isSearchIdentifier(String identifier) {
        String lower = identifier == null ? "" : identifier.toLowerCase();
        return lower.startsWith("ytsearch:") || lower.startsWith("ytmsearch:") || lower.startsWith("scsearch:");
    }

    private void disconnectIfIdle(TextChannel channel, GuildMusicManager musicManager) {
        if (musicManager.player.getPlayingTrack() == null && musicManager.scheduler.getQueue().isEmpty()) {
            channel.getGuild().getAudioManager().closeAudioConnection();
            updatePresence(channel.getGuild());
            channel.sendMessage(guildI18n(channel.getGuild()).t("disconnect.idle")).queue();
        }
    }

    public record MetricsSnapshot(
            int trackedGuilds,
            int activePlayers,
            int queuedTracks,
            long loadSuccess,
            long loadFailures,
            long noMatches,
            String nowPlayingTitle,
            long nowPlayingPositionMs,
            long nowPlayingDurationMs,
            String nowPlayingState
    ) {
    }

    private record PendingSearch(long requesterId, List<AudioTrack> tracks) {
    }

    public record SearchSelectionOutcome(boolean success, String message) {
        public static SearchSelectionOutcome success(String message) {
            return new SearchSelectionOutcome(true, message);
        }

        public static SearchSelectionOutcome error(String message) {
            return new SearchSelectionOutcome(false, message);
        }
    }

    public record SearchTrackOption(String title, String uri) {
    }
}
