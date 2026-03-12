package com.artem.musicbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Consumer<AudioTrack> onTrackStart;
    private final Runnable onQueueEmpty;
    private final Queue<AudioTrack> queue = new ConcurrentLinkedQueue<>();
    private boolean loopTrack;
    private boolean loopQueue;

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

    public void queueNext(AudioTrack track) {
        if (player.getPlayingTrack() == null) {
            if (player.startTrack(track, true)) {
                onTrackStart.accept(track);
            } else {
                queue.offer(track);
            }
            return;
        }

        List<AudioTrack> tracks = new ArrayList<>(queue);
        queue.clear();
        queue.offer(track);
        queue.addAll(tracks);
    }

    public AudioTrack skip() {
        AudioTrack current = player.getPlayingTrack();
        AudioTrack next = queue.poll();

        if (loopQueue && current != null) {
            queue.offer(current.makeClone());
        }

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

    public int clearQueue() {
        int size = queue.size();
        queue.clear();
        return size;
    }

    public boolean removeAt(int index) {
        if (index < 1 || index > queue.size()) {
            return false;
        }

        List<AudioTrack> tracks = new ArrayList<>(queue);
        tracks.remove(index - 1);
        queue.clear();
        queue.addAll(tracks);
        return true;
    }

    public int shuffleQueue() {
        List<AudioTrack> tracks = new ArrayList<>(queue);
        Collections.shuffle(tracks);
        queue.clear();
        queue.addAll(tracks);
        return tracks.size();
    }

    public boolean seekTo(long positionMs) {
        AudioTrack current = player.getPlayingTrack();
        if (current == null) {
            return false;
        }

        current.setPosition(positionMs);
        return true;
    }

    public boolean isLoopTrack() {
        return loopTrack;
    }

    public boolean isLoopQueue() {
        return loopQueue;
    }

    public void setLoopTrack(boolean loopTrack) {
        this.loopTrack = loopTrack;
        if (loopTrack) {
            this.loopQueue = false;
        }
    }

    public void setLoopQueue(boolean loopQueue) {
        this.loopQueue = loopQueue;
        if (loopQueue) {
            this.loopTrack = false;
        }
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (loopTrack && endReason.mayStartNext && track != null) {
            AudioTrack clone = track.makeClone();
            player.startTrack(clone, false);
            onTrackStart.accept(clone);
        } else if (endReason.mayStartNext) {
            skip();
        } else if (queue.isEmpty()) {
            onQueueEmpty.run();
        }
    }
}
