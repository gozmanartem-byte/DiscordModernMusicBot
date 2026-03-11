package com.artem.musicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Consumer<AudioTrack> onTrackStart;
    private final Runnable onQueueEmpty;
    private final Queue<AudioTrack> queue = new ConcurrentLinkedQueue<>();

    public TrackScheduler(AudioPlayer player, Consumer<AudioTrack> onTrackStart, Runnable onQueueEmpty) {
        this.player = player;
        this.onTrackStart = Objects.requireNonNull(onTrackStart);
        this.onQueueEmpty = Objects.requireNonNull(onQueueEmpty);
    }

    public void queue(AudioTrack track) {
        if (player.startTrack(track, true)) {
            onTrackStart.accept(track);
        } else {
            queue.offer(track);
        }
    }

    public AudioTrack skip() {
        AudioTrack next = queue.poll();
        player.startTrack(next, false);

        if (next != null) {
            onTrackStart.accept(next);
        } else {
            onQueueEmpty.run();
        }

        return next;
    }

    public void stop() {
        queue.clear();
        player.stopTrack();
        onQueueEmpty.run();
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            skip();
        } else if (queue.isEmpty()) {
            onQueueEmpty.run();
        }
    }
}
