package com.artem.musicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Web;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicController {
    private static final int DEFAULT_VOLUME = 100;
    private static final int MAX_VOLUME = 200;
    private static final int MAX_BASS = 5;

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();

    public MusicController(BotConfig config) {
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

    public void setLoudPreset(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        musicManager.player.setVolume(MAX_VOLUME);
        applyBassBoost(musicManager, MAX_BASS);
        channel.sendMessage("Loud preset enabled (volume 200%, bass 5). Use !normal to reset.").queue();
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
            builder.append("Upcoming:\n");
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
            guild.getJDA().getPresence().setActivity(Activity.playing("Ждёт музыку"));
        } else {
            guild.getJDA().getPresence().setActivity(Activity.playing("Поёт " + title));
        }
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
