package com.artem.musicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final AtomicLong providedFrames = new AtomicLong();
    private final AtomicLong provideCalls = new AtomicLong();
    private volatile long lastFrameNanos = 0L;
    private volatile String lastCodecName = "n/a";
    private volatile int lastDataLength = 0;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
    }

    @Override
    public boolean canProvide() {
        provideCalls.incrementAndGet();
        AudioFrame frame = audioPlayer.provide();
        if (frame == null) {
            return false;
        }

        ((Buffer) buffer).clear();
        buffer.put(frame.getData());
        ((Buffer) buffer).flip();
        providedFrames.incrementAndGet();
        lastFrameNanos = System.nanoTime();
        lastCodecName = frame.getFormat().codecName();
        lastDataLength = frame.getDataLength();
        return true;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public long getProvidedFrames() {
        return providedFrames.get();
    }

    public long getProvideCalls() {
        return provideCalls.get();
    }

    public long getMillisSinceLastFrame() {
        long timestamp = lastFrameNanos;
        if (timestamp == 0L) {
            return -1L;
        }
        return (System.nanoTime() - timestamp) / 1_000_000L;
    }

    public String getLastCodecName() {
        return lastCodecName;
    }

    public int getLastDataLength() {
        return lastDataLength;
    }
}
