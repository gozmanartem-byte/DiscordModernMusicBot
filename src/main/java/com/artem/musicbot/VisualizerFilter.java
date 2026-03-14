package com.artem.musicbot;

import java.nio.ShortBuffer;
import java.util.Arrays;

import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;

public final class VisualizerFilter implements UniversalPcmAudioFilter {
    private final UniversalPcmAudioFilter downstream;
    private final AudioVisualizer visualizer;
    private final int sampleRate;
    private final int channelCount;
    private final float[][] floatBuffer;

    public VisualizerFilter(UniversalPcmAudioFilter downstream, AudioVisualizer visualizer, int sampleRate, int channelCount) {
        this.downstream = downstream;
        this.visualizer = visualizer;
        this.sampleRate = sampleRate;
        this.channelCount = Math.max(1, channelCount);
        this.floatBuffer = new float[this.channelCount][AudioVisualizer.FFT_SIZE];
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        if (visualizer != null) {
            visualizer.process(input, offset, length, sampleRate);
        }
        downstream.process(input, offset, length);
    }

    @Override
    public void process(short[] input, int offset, int length) throws InterruptedException {
        if (visualizer != null && input != null) {
            int frames = Math.min(length, floatBuffer[0].length);
            int base = offset * channelCount;
            for (int i = 0; i < frames; i++) {
                int sampleBase = base + i * channelCount;
                for (int ch = 0; ch < channelCount; ch++) {
                    int index = sampleBase + ch;
                    if (index >= 0 && index < input.length) {
                        floatBuffer[ch][i] = input[index] / 32768f;
                    } else {
                        floatBuffer[ch][i] = 0f;
                    }
                }
            }
            visualizer.process(floatBuffer, 0, frames, sampleRate);
        }
        downstream.process(input, offset, length);
    }

    @Override
    public void process(short[][] input, int offset, int length) throws InterruptedException {
        if (visualizer != null && input != null) {
            int frames = Math.min(length, floatBuffer[0].length);
            int channels = Math.min(channelCount, input.length);
            for (int ch = 0; ch < channels; ch++) {
                short[] channel = input[ch];
                for (int i = 0; i < frames; i++) {
                    int index = offset + i;
                    if (index >= 0 && index < channel.length) {
                        floatBuffer[ch][i] = channel[index] / 32768f;
                    } else {
                        floatBuffer[ch][i] = 0f;
                    }
                }
            }
            for (int ch = channels; ch < channelCount; ch++) {
                Arrays.fill(floatBuffer[ch], 0, frames, 0f);
            }
            visualizer.process(floatBuffer, 0, frames, sampleRate);
        }
        downstream.process(input, offset, length);
    }

    @Override
    public void process(ShortBuffer buffer) throws InterruptedException {
        if (visualizer != null && buffer != null) {
            ShortBuffer snapshot = buffer.duplicate();
            int frames = Math.min(snapshot.remaining() / channelCount, floatBuffer[0].length);
            for (int i = 0; i < frames; i++) {
                for (int ch = 0; ch < channelCount; ch++) {
                    if (snapshot.hasRemaining()) {
                        floatBuffer[ch][i] = snapshot.get() / 32768f;
                    } else {
                        floatBuffer[ch][i] = 0f;
                    }
                }
            }
            visualizer.process(floatBuffer, 0, frames, sampleRate);
        }
        downstream.process(buffer);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {
        downstream.seekPerformed(requestedTime, providedTime);
    }

    @Override
    public void flush() throws InterruptedException {
        downstream.flush();
    }

    @Override
    public void close() {
        downstream.close();
    }
}
