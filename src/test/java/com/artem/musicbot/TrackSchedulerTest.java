package com.artem.musicbot;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackSchedulerTest {

    @Test
    void queueStartsTrackImmediatelyWhenPlayerIdle() {
        AudioPlayer player = mock(AudioPlayer.class);
        AudioTrack first = mock(AudioTrack.class);
        AtomicInteger started = new AtomicInteger();

        when(player.startTrack(first, true)).thenReturn(true);

        TrackScheduler scheduler = new TrackScheduler(player, track -> started.incrementAndGet(), () -> {});
        scheduler.queue(first);

        verify(player).startTrack(eq(first), eq(true));
        assertEquals(1, started.get());
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    void queueAddsTrackWhenPlayerBusy() {
        AudioPlayer player = mock(AudioPlayer.class);
        AudioTrack first = mock(AudioTrack.class);

        when(player.startTrack(first, true)).thenReturn(false);

        TrackScheduler scheduler = new TrackScheduler(player, track -> {}, () -> {});
        scheduler.queue(first);

        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    void skipRequeuesCurrentWhenLoopQueueEnabled() {
        AudioPlayer player = mock(AudioPlayer.class);
        AudioTrack current = mock(AudioTrack.class);
        AudioTrack currentClone = mock(AudioTrack.class);
        AudioTrack queued = mock(AudioTrack.class);

        when(player.startTrack(queued, true)).thenReturn(false);
        when(player.getPlayingTrack()).thenReturn(current);
        when(current.makeClone()).thenReturn(currentClone);

        TrackScheduler scheduler = new TrackScheduler(player, track -> {}, () -> {});
        scheduler.queue(queued);
        scheduler.setLoopQueue(true);

        AudioTrack next = scheduler.skip();

        assertEquals(queued, next);
        verify(player).startTrack(eq(queued), eq(false));
        assertEquals(1, scheduler.getQueue().size());
        assertEquals(currentClone, scheduler.getQueue().peek());
    }

    @Test
    void onTrackEndRestartsCloneWhenLoopTrackEnabled() {
        AudioPlayer player = mock(AudioPlayer.class);
        AudioTrack current = mock(AudioTrack.class);
        AudioTrack clone = mock(AudioTrack.class);
        AtomicInteger started = new AtomicInteger();

        when(current.makeClone()).thenReturn(clone);

        TrackScheduler scheduler = new TrackScheduler(player, track -> started.incrementAndGet(), () -> {});
        scheduler.setLoopTrack(true);
        scheduler.onTrackEnd(player, current, AudioTrackEndReason.FINISHED);

        verify(player).startTrack(eq(clone), eq(false));
        assertEquals(1, started.get());
    }

    @Test
    void removeAtRejectsInvalidIndex() {
        AudioPlayer player = mock(AudioPlayer.class);
        TrackScheduler scheduler = new TrackScheduler(player, track -> {}, () -> {});

        assertFalse(scheduler.removeAt(0));
        assertFalse(scheduler.removeAt(1));
    }
}
