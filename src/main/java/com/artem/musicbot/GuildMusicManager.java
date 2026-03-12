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
    private String timingTrackId;
    private long timingAnchorTrackPosMs;
    private long timingAnchorWallMs;
    private boolean timingPaused;

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
        synchronized (this) {
            AudioTrack current = player.getPlayingTrack();
            if (current == null) {
                timingTrackId = null;
                timingAnchorTrackPosMs = 0L;
                timingAnchorWallMs = System.currentTimeMillis();
                timingPaused = false;
                return;
            }

            timingTrackId = current.getIdentifier();
            timingAnchorTrackPosMs = current.getPosition();
            timingAnchorWallMs = System.currentTimeMillis();
            timingPaused = false;
        }
    }

    public void markTrackPaused() {
        synchronized (this) {
            AudioTrack current = player.getPlayingTrack();
            if (current == null) {
                timingPaused = true;
                return;
            }

            timingTrackId = current.getIdentifier();
            timingAnchorTrackPosMs = Math.max(timingAnchorTrackPosMs, current.getPosition());
            timingAnchorWallMs = System.currentTimeMillis();
            timingPaused = true;
        }
    }

    public void markTrackResumed() {
        synchronized (this) {
            AudioTrack current = player.getPlayingTrack();
            if (current == null) {
                timingPaused = false;
                timingAnchorWallMs = System.currentTimeMillis();
                return;
            }

            timingTrackId = current.getIdentifier();
            timingAnchorTrackPosMs = Math.max(timingAnchorTrackPosMs, current.getPosition());
            timingAnchorWallMs = System.currentTimeMillis();
            timingPaused = false;
        }
    }

    public long getCalculatedPositionMs() {
        synchronized (this) {
            AudioTrack current = player.getPlayingTrack();
            if (current == null) {
                timingTrackId = null;
                timingAnchorTrackPosMs = 0L;
                timingAnchorWallMs = System.currentTimeMillis();
                timingPaused = false;
                return 0L;
            }

            String currentTrackId = current.getIdentifier();
            long observedPosition = current.getPosition();
            long now = System.currentTimeMillis();

            if (timingTrackId == null || !timingTrackId.equals(currentTrackId)) {
                timingTrackId = currentTrackId;
                timingAnchorTrackPosMs = observedPosition;
                timingAnchorWallMs = now;
                timingPaused = player.isPaused();
            }

            long predictedPosition = timingAnchorTrackPosMs;
            if (!player.isPaused() && !timingPaused) {
                long elapsed = Math.max(0L, now - timingAnchorWallMs);
                predictedPosition += elapsed;
            }

            long calculated = Math.max(observedPosition, predictedPosition);
            long duration = current.getDuration();
            if (duration >= 0) {
                calculated = Math.min(calculated, duration);
            }

            timingAnchorTrackPosMs = calculated;
            timingAnchorWallMs = now;
            timingPaused = player.isPaused();
            return calculated;
        }
    }
}
