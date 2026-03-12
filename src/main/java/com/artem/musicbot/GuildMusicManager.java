package com.artem.musicbot;

import java.util.function.Consumer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;
    private int bassLevel;

    public GuildMusicManager(AudioPlayerManager playerManager, Consumer<AudioTrack> onTrackStart, Runnable onQueueEmpty) {
        this.player = playerManager.createPlayer();
        this.player.setVolume(100);
        this.scheduler = new TrackScheduler(player, onTrackStart, onQueueEmpty);
        this.sendHandler = new AudioPlayerSendHandler(player);
        this.player.addListener(scheduler);
    }

    public int getBassLevel() {
        return bassLevel;
    }

    public void setBassLevel(int bassLevel) {
        this.bassLevel = bassLevel;
    }

    public void markTrackStarted() {
        // Kept for compatibility with existing call sites.
    }

    public void markTrackPaused() {
        // Kept for compatibility with existing call sites.
    }

    public void markTrackResumed() {
        // Kept for compatibility with existing call sites.
    }

    public long getCalculatedPositionMs() {
        AudioTrack current = player.getPlayingTrack();
        if (current == null) {
            return 0L;
        }
        return current.getPosition();
    }
}
