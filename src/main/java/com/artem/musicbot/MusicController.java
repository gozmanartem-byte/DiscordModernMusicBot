package com.artem.musicbot;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
    private final Map<Long, String> lastTrackQueries = new ConcurrentHashMap<>();
    private final Map<Long, TextChannel> lastTextChannels = new ConcurrentHashMap<>();
    private final I18n i18n;
    private final GuildSettingsStore settingsStore;
    private final AtomicLong loadSuccessCount = new AtomicLong();
    private final AtomicLong loadFailureCount = new AtomicLong();
    private final AtomicLong noMatchesCount = new AtomicLong();
    private final AtomicLong searchSelectionCounter = new AtomicLong();
    private final Map<String, PendingSearch> pendingSearches = new ConcurrentHashMap<>();
    private final Map<Long, Long> playerPanelMessageIds = new ConcurrentHashMap<>();
    private final Map<Long, Long> playerPanelChannelIds = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> playerPanelRefreshInFlight = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> playerPanelRefreshPending = new ConcurrentHashMap<>();

    public MusicController(BotConfig config, I18n i18n, GuildSettingsStore settingsStore) {
        this.i18n = i18n;
        this.settingsStore = settingsStore;
        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
        this.playerManager.getConfiguration().setFrameBufferFactory(
            (bufferDuration, format, stopping) -> new NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping));
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
        String resolvedIdentifier = normalizeIdentifier(identifier);
        lastTextChannels.put(channel.getGuild().getIdLong(), channel);
        VoiceChannel voiceChannel = getUserVoiceChannel(member);
        if (voiceChannel == null) {
            channel.sendMessage("Join a voice channel first.").queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        connect(channel.getGuild(), voiceChannel, musicManager);

        GuildVoiceState selfVoiceState = channel.getGuild().getSelfMember().getVoiceState();
        if (selfVoiceState != null && selfVoiceState.isGuildMuted()) {
            channel.sendMessage("Warning: I am server-muted in this voice channel. Unmute me in Discord to hear audio.").queue();
        }

        channel.sendMessage("Loading: " + identifier).queue();
        playerManager.loadItemOrdered(musicManager, resolvedIdentifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage("Queued: " + track.getInfo().title).queue(
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
                    channel.sendMessage("Playlist is empty.").queue();
                    disconnectIfIdle(channel, musicManager);
                    return;
                }

                musicManager.scheduler.queue(track);
                lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
                loadSuccessCount.incrementAndGet();
                channel.sendMessage("Queued from playlist: " + track.getInfo().title).queue(
                    ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
                    ignored -> {
                    }
                );
            }

            @Override
            public void noMatches() {
                noMatchesCount.incrementAndGet();
                channel.sendMessage("Nothing found for: " + identifier).queue();
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

    public SearchSelectionOutcome chooseSearchResult(TextChannel channel, Member member, String selectionId, int index) {
        PendingSearch pendingSearch = pendingSearches.get(selectionId);
        if (pendingSearch == null) {
            return SearchSelectionOutcome.error("This search selection expired.");
        }

        if (!canUsePendingSearch(member, pendingSearch)) {
            return SearchSelectionOutcome.error("Only the user who started this search can choose a result.");
        }

        if (index < 0 || index >= pendingSearch.tracks().size()) {
            return SearchSelectionOutcome.error("Invalid search result selection.");
        }

        AudioTrack track = pendingSearch.tracks().get(index).makeClone();
        pendingSearches.remove(selectionId);

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.scheduler.queue(track);
        lastTrackQueries.put(channel.getGuild().getIdLong(), track.getInfo().title);
        loadSuccessCount.incrementAndGet();
        channel.sendMessage("Queued: " + track.getInfo().title).queue(
            ignored -> refreshPersistentPlayerPanel(channel.getGuild()),
            ignored -> {
            }
        );
        return SearchSelectionOutcome.success("Selected: " + track.getInfo().title);
    }

    public SearchSelectionOutcome cancelSearchSelection(TextChannel channel, Member member, String selectionId) {
        PendingSearch pendingSearch = pendingSearches.get(selectionId);
        if (pendingSearch == null) {
            return SearchSelectionOutcome.error("This search selection already expired.");
        }

        if (!canUsePendingSearch(member, pendingSearch)) {
            return SearchSelectionOutcome.error("Only the user who started this search can cancel it.");
        }

        pendingSearches.remove(selectionId);
        disconnectIfIdle(channel, getGuildMusicManager(channel.getGuild()));
        return SearchSelectionOutcome.success("Search cancelled.");
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
            nowPlayingPositionMs = current.getPosition();
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

    public void skip(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack next = musicManager.scheduler.skip();
        if (next == null) {
            channel.sendMessage("Queue is empty.").queue();
        } else {
            channel.sendMessage("Skipped. Now playing: " + next.getInfo().title).queue();
        }
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void pause(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();

        if (current == null) {
            channel.sendMessage("Nothing is playing right now.").queue();
            return;
        }

        if (musicManager.player.isPaused()) {
            channel.sendMessage("Playback is already paused.").queue();
            return;
        }

        musicManager.player.setPaused(true);
        channel.sendMessage("Paused playback.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void resume(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();

        if (current == null) {
            channel.sendMessage("Nothing is playing right now.").queue();
            return;
        }

        if (!musicManager.player.isPaused()) {
            channel.sendMessage("Playback is already running.").queue();
            return;
        }

        musicManager.player.setPaused(false);
        channel.sendMessage("Resumed playback.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void stop(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.scheduler.stop();
        channel.getGuild().getAudioManager().closeAudioConnection();
        updatePresence(channel.getGuild());
        channel.sendMessage("Stopped playback and left the voice channel.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setVolume(TextChannel channel, int volume) {
        if (volume < 0 || volume > MAX_VOLUME) {
            channel.sendMessage("Volume must be between 0 and " + MAX_VOLUME + ".").queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(volume);
        channel.sendMessage("Volume set to " + volume + "%.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void adjustVolume(TextChannel channel, int delta) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int current = musicManager.player.getVolume();
        int next = Math.max(0, Math.min(MAX_VOLUME, current + delta));
        musicManager.player.setVolume(next);
        channel.sendMessage("Volume set to " + next + "%.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void showVolume(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        channel.sendMessage("Current volume: " + musicManager.player.getVolume() + "%.").queue();
    }

    public void setBass(TextChannel channel, int level) {
        if (level < 0 || level > MAX_BASS) {
            channel.sendMessage("Bass level must be between 0 and " + MAX_BASS + ".").queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        applyBassBoost(musicManager, level);
        channel.sendMessage("Bass boost set to " + level + ".").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void showBass(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        channel.sendMessage("Current bass boost: " + musicManager.getBassLevel() + ".").queue();
    }

    public void resetNormal(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(DEFAULT_VOLUME);
        applyBassBoost(musicManager, 0);
        channel.sendMessage("Audio reset to normal (volume 100%, bass 0)." ).queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setLoudPreset(TextChannel channel, String prefix) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(MAX_VOLUME);
        applyBassBoost(musicManager, MAX_BASS);
        channel.sendMessage("Loud preset enabled (volume 200%, bass 5). Use " + prefix + "normal to reset.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void queue(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack current = musicManager.player.getPlayingTrack();
        StringBuilder builder = new StringBuilder();
        if (current != null) {
            builder.append("Now playing: ").append(current.getInfo().title).append('\n');
        }
        if (musicManager.scheduler.getQueue().isEmpty()) {
            builder.append("Queue is empty.");
        } else {
            builder.append("Upcoming:").append('\n');
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
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        boolean removed = musicManager.scheduler.removeAt(index);
        channel.sendMessage(removed ? "Removed track #" + index + " from queue." : "Invalid queue index.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void clearQueue(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int removed = musicManager.scheduler.clearQueue();
        channel.sendMessage("Cleared " + removed + " queued track(s).").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void shuffleQueue(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int shuffled = musicManager.scheduler.shuffleQueue();
        channel.sendMessage(shuffled == 0 ? "Queue is empty." : "Shuffled " + shuffled + " queued track(s).").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setLoop(TextChannel channel, String mode) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        String normalized = mode == null ? "off" : mode.trim().toLowerCase();

        switch (normalized) {
            case "track" -> {
                musicManager.scheduler.setLoopTrack(true);
                channel.sendMessage("Loop mode set to: track.").queue();
            }
            case "queue" -> {
                musicManager.scheduler.setLoopQueue(true);
                channel.sendMessage("Loop mode set to: queue.").queue();
            }
            default -> {
                musicManager.scheduler.setLoopTrack(false);
                musicManager.scheduler.setLoopQueue(false);
                channel.sendMessage("Loop mode set to: off.").queue();
            }
        }
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void seek(TextChannel channel, long seconds) {
        if (seconds < 0) {
            channel.sendMessage("Seek must be >= 0.").queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        boolean ok = musicManager.scheduler.seekTo(seconds * 1000L);
        channel.sendMessage(ok ? "Seeked to " + seconds + "s." : "Nothing is playing right now.").queue();
        refreshPersistentPlayerPanel(channel.getGuild());
    }

    public void setAutoplay(TextChannel channel, boolean enabled) {
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
        channel.sendMessage("Autoplay " + (enabled ? "enabled." : "disabled.")).queue();
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
                "managedQueues=" + guildCount);
    }

    public void sendPlayerPanel(TextChannel channel, String prefix) {
        repostPlayerPanel(channel, prefix);
    }

    public void refreshPlayerPanel(Message message, String prefix) {
        if (message.getChannel() instanceof TextChannel channel) {
            repostPlayerPanel(channel, prefix);
        }
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
                "bassLevel=" + musicManager.getBassLevel());

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
            GuildMusicManager musicManager = new GuildMusicManager(
                    playerManager,
                    track -> {
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

            int defaultVolume = settingsStore.get(guild.getIdLong()).defaultVolume();
            musicManager.player.setVolume(Math.max(0, Math.min(MAX_VOLUME, defaultVolume)));
            guild.getAudioManager().setSendingHandler(musicManager.sendHandler);
            return musicManager;
        });
    }

    private void updatePresence(Guild guild) {
        String title = null;
        for (GuildMusicManager manager : musicManagers.values()) {
            AudioTrack track = manager.player.getPlayingTrack();
            if (track != null) {
                title = track.getInfo().title;
                break;
            }
        }

        if (title == null || title.isBlank()) {
            guild.getJDA().getPresence().setActivity(Activity.playing(i18n.t("status.waiting")));
        } else {
            guild.getJDA().getPresence().setActivity(Activity.playing(i18n.t("status.playing", title)));
        }
    }

    private MessageEmbed buildPlayerEmbed(Guild guild, String prefix) {
        GuildMusicManager musicManager = getGuildMusicManager(guild);
        AudioTrack current = musicManager.player.getPlayingTrack();

        String state = current == null
                ? i18n.t("player.state.idle")
                : musicManager.player.isPaused()
                    ? i18n.t("player.state.paused")
                    : i18n.t("player.state.playing");

        String trackValue = current == null
            ? i18n.t("player.none")
            : buildTrackValue(current);

        var connectedChannel = guild.getAudioManager().getConnectedChannel();
        String voiceValue = connectedChannel == null ? i18n.t("player.notConnected") : connectedChannel.getName();
        String queueValue = buildQueuePreview(musicManager);

        return new EmbedBuilder()
                .setTitle(i18n.t("player.title"))
                .setDescription(i18n.t("player.hint", prefix))
                .setColor(current == null ? new Color(120, 120, 120) : new Color(46, 204, 113))
                .addField(i18n.t("player.status"), state, true)
                .addField(i18n.t("player.voice"), voiceValue, true)
                .addField(i18n.t("player.volume"), musicManager.player.getVolume() + "%", true)
                .addField(i18n.t("player.track"), trackValue, false)
                .addField(i18n.t("player.queuePreview"), queueValue, false)
                .addField(i18n.t("player.bass"), String.valueOf(musicManager.getBassLevel()), true)
                .setFooter(i18n.t("player.footer", prefix))
                .build();
    }

    private String buildTrackValue(AudioTrack current) {
        long position = current.getPosition();
        long duration = current.getDuration();
        long now = System.currentTimeMillis();
        long startedEpoch = Math.max(0L, (now - position) / 1000L);

        StringBuilder value = new StringBuilder(current.getInfo().title)
                .append("\n<t:")
                .append(startedEpoch)
                .append(":R>")
                .append(" / ")
                .append(formatDuration(duration));

        return value.toString();
    }

    private void repostPlayerPanel(TextChannel channel, String prefix) {
        playerPanelChannelIds.put(channel.getGuild().getIdLong(), channel.getIdLong());
        enqueuePlayerPanelRepost(channel.getGuild(), prefix);
    }

    private void enqueuePlayerPanelRepost(Guild guild, String prefix) {
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

        performPlayerPanelRepost(guild, channel, prefix, inFlight, pending);
    }

    private void performPlayerPanelRepost(
            Guild guild,
            TextChannel channel,
            String prefix,
            AtomicBoolean inFlight,
            AtomicBoolean pending
    ) {
        long guildId = channel.getGuild().getIdLong();
        deleteTrackedPlayerPanel(channel.getGuild(), guildId);

        channel.sendMessageEmbeds(buildPlayerEmbed(channel.getGuild(), prefix))
                .setComponents(playerComponents())
                .queue(message -> {
                    playerPanelChannelIds.put(guildId, channel.getIdLong());
                    playerPanelMessageIds.put(guildId, message.getIdLong());
                    finishPlayerPanelRepost(guild, inFlight, pending);
                }, ignored -> finishPlayerPanelRepost(guild, inFlight, pending));
    }

    private void finishPlayerPanelRepost(Guild guild, AtomicBoolean inFlight, AtomicBoolean pending) {
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
        performPlayerPanelRepost(guild, nextChannel, prefix, inFlight, pending);
    }

    private void refreshPersistentPlayerPanel(Guild guild) {
        TextChannel channel = resolvePlayerPanelChannel(guild);
        if (channel == null) {
            return;
        }

        String prefix = settingsStore.get(guild.getIdLong()).prefix();
        repostPlayerPanel(channel, prefix);
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

        return lastTextChannels.get(guildId);
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

    private List<MessageTopLevelComponent> playerComponents() {
        return List.of(
                ActionRow.of(
                        Button.secondary("player:pause", i18n.t("player.pause")),
                        Button.success("player:resume", i18n.t("player.resume")),
                        Button.primary("player:skip", i18n.t("player.skip")),
                        Button.danger("player:stop", i18n.t("player.stop"))
                ),
                ActionRow.of(
                        Button.primary("player:queue", i18n.t("player.queue")),
                        Button.secondary("player:voldown", i18n.t("player.voldown")),
                        Button.secondary("player:volup", i18n.t("player.volup")),
                        Button.secondary("player:refresh", i18n.t("player.refresh"))
                )
        );
    }

    private String buildQueuePreview(GuildMusicManager musicManager) {
        if (musicManager.scheduler.getQueue().isEmpty()) {
            return i18n.t("player.queueEmpty");
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
                musicManager.scheduler.queue(track);
                lastTrackQueries.put(guild.getIdLong(), track.getInfo().title);
                announceChannel.sendMessage("Autoplay queued: " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack selected = playlist.getSelectedTrack();
                if (selected == null && !playlist.getTracks().isEmpty()) {
                    selected = playlist.getTracks().get(0);
                }
                if (selected != null) {
                    musicManager.scheduler.queue(selected);
                    lastTrackQueries.put(guild.getIdLong(), selected.getInfo().title);
                    announceChannel.sendMessage("Autoplay queued: " + selected.getInfo().title).queue();
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
            channel.sendMessage("Nothing found for: " + query).queue();
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
            channel.sendMessage("No playable track was loaded, so I left the voice channel.").queue();
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
}
