package com.artem.musicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.function.Consumer;

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
}
