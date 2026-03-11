package com.artem.musicbot;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

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
    private final I18n i18n;

    public MusicController(BotConfig config, I18n i18n) {
        this.i18n = i18n;
        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
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
        playerManager.loadItemOrdered(musicManager, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                channel.sendMessage("Queued: " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && !playlist.getTracks().isEmpty()) {
                    track = playlist.getTracks().get(0);
                }

                if (track == null) {
                    channel.sendMessage("Playlist is empty.").queue();
                    return;
                }

                musicManager.scheduler.queue(track);
                channel.sendMessage("Queued from playlist: " + track.getInfo().title).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found for: " + identifier).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Load failed: " + exception.getMessage()).queue();
            }
        });
    }

    public void skip(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        AudioTrack next = musicManager.scheduler.skip();
        if (next == null) {
            channel.sendMessage("Queue is empty.").queue();
        } else {
            channel.sendMessage("Skipped. Now playing: " + next.getInfo().title).queue();
        }
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
    }

    public void stop(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.scheduler.stop();
        channel.getGuild().getAudioManager().closeAudioConnection();
        updatePresence(channel.getGuild());
        channel.sendMessage("Stopped playback and left the voice channel.").queue();
    }

    public void setVolume(TextChannel channel, int volume) {
        if (volume < 0 || volume > MAX_VOLUME) {
            channel.sendMessage("Volume must be between 0 and " + MAX_VOLUME + ".").queue();
            return;
        }

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(volume);
        channel.sendMessage("Volume set to " + volume + "%.").queue();
    }

    public void adjustVolume(TextChannel channel, int delta) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        int current = musicManager.player.getVolume();
        int next = Math.max(0, Math.min(MAX_VOLUME, current + delta));
        musicManager.player.setVolume(next);
        channel.sendMessage("Volume set to " + next + "%.").queue();
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
    }

    public void setLoudPreset(TextChannel channel, String prefix) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(MAX_VOLUME);
        applyBassBoost(musicManager, MAX_BASS);
        channel.sendMessage("Loud preset enabled (volume 200%, bass 5). Use " + prefix + "normal to reset.").queue();
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

    public void sendPlayerPanel(TextChannel channel, String prefix) {
        channel.sendMessageEmbeds(buildPlayerEmbed(channel.getGuild(), prefix))
                .setComponents(playerComponents())
                .queue();
    }

    public void refreshPlayerPanel(Message message, String prefix) {
        message.editMessageEmbeds(buildPlayerEmbed(message.getGuild(), prefix))
                .setComponents(playerComponents())
                .queue();
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
        String connectedChannel = audioManager.getConnectedChannel() == null
            ? "none"
            : audioManager.getConnectedChannel().getName() + " (" + audioManager.getConnectedChannel().getType() + ")";

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
                    track -> updatePresence(guild),
                    () -> updatePresence(guild)
            );
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
                : current.getInfo().title + "\n`" + formatDuration(current.getPosition()) + " / " + formatDuration(current.getDuration()) + "`";

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
}
