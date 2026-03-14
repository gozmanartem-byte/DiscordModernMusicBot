package com.artem.musicbot;

import java.util.Arrays;

public final class AudioVisualizer {
    public static final int BANDS = 28;
    static final int FFT_SIZE = 1024;
    private static final float MIN_FREQ = 60f;
    private static final float MAX_FREQ = 12000f;

    private final float[] window = new float[FFT_SIZE];
    private final float[] real = new float[FFT_SIZE];
    private final float[] imag = new float[FFT_SIZE];
    private final float[] bandBuffer = new float[BANDS];
    private final float[] smooth = new float[BANDS];
    private final float[] levels = new float[BANDS];
    private int[] bandEdges = buildBandEdges(48000);
    private int lastSampleRate = 48000;

    public AudioVisualizer() {
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (FFT_SIZE - 1)));
        }
        Arrays.fill(levels, 0f);
    }

    public void process(float[][] input, int offset, int length, int sampleRate) {
        if (input == null || input.length == 0 || length <= 0) {
            return;
        }
        if (sampleRate > 0 && sampleRate != lastSampleRate) {
            bandEdges = buildBandEdges(sampleRate);
            lastSampleRate = sampleRate;
        }

        int channels = input.length;
        int count = Math.min(length, FFT_SIZE);
        for (int i = 0; i < count; i++) {
            float sample = 0f;
            int idx = offset + i;
            for (int ch = 0; ch < channels; ch++) {
                sample += input[ch][idx];
            }
            sample /= Math.max(1, channels);
            real[i] = sample * window[i];
            imag[i] = 0f;
        }
        for (int i = count; i < FFT_SIZE; i++) {
            real[i] = 0f;
            imag[i] = 0f;
        }

        fft(real, imag);

        int binCount = FFT_SIZE / 2;
        float scale = 6.0f;
        float norm = (float) Math.log10(1f + scale);
        for (int band = 0; band < BANDS; band++) {
            int start = Math.max(1, bandEdges[band]);
            int end = Math.min(binCount - 1, bandEdges[band + 1]);
            if (end <= start) {
                end = Math.min(binCount - 1, start + 1);
            }
            float sum = 0f;
            int countBins = 0;
            for (int i = start; i < end; i++) {
                float re = real[i];
                float im = imag[i];
                float mag = (float) Math.sqrt(re * re + im * im);
                sum += mag;
                countBins++;
            }
            float avg = countBins == 0 ? 0f : (sum / countBins);
            float level = (float) Math.log10(1f + avg * scale) / norm;
            bandBuffer[band] = Math.max(0f, Math.min(1f, level));
        }

        synchronized (this) {
            for (int i = 0; i < BANDS; i++) {
                float target = bandBuffer[i];
                float current = smooth[i];
                float speed = target > current ? 0.45f : 0.2f;
                float next = current + (target - current) * speed;
                smooth[i] = next;
                levels[i] = next;
            }
        }
    }

    public synchronized float[] snapshot() {
        return Arrays.copyOf(levels, levels.length);
    }

    private static int[] buildBandEdges(int sampleRate) {
        int[] edges = new int[BANDS + 1];
        float max = Math.min(MAX_FREQ, sampleRate / 2f);
        float min = Math.min(MIN_FREQ, max * 0.5f);
        if (min <= 0f) {
            min = 60f;
        }
        double ratio = max / min;
        for (int i = 0; i <= BANDS; i++) {
            double exponent = i / (double) BANDS;
            double freq = min * Math.pow(ratio, exponent);
            int bin = (int) Math.round(freq * FFT_SIZE / sampleRate);
            edges[i] = Math.max(1, Math.min(FFT_SIZE / 2 - 1, bin));
        }
        for (int i = 1; i <= BANDS; i++) {
            edges[i] = Math.max(edges[i], edges[i - 1] + 1);
            if (edges[i] >= FFT_SIZE / 2) {
                edges[i] = FFT_SIZE / 2 - 1;
            }
        }
        edges[BANDS] = Math.min(edges[BANDS], FFT_SIZE / 2 - 1);
        return edges;
    }

    private static void fft(float[] real, float[] imag) {
        int n = real.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; j >= bit; bit >>= 1) {
                j -= bit;
            }
            j += bit;
            if (i < j) {
                float tmp = real[i];
                real[i] = real[j];
                real[j] = tmp;
                tmp = imag[i];
                imag[i] = imag[j];
                imag[j] = tmp;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            float wLenR = (float) Math.cos(angle);
            float wLenI = (float) Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                float wr = 1f;
                float wi = 0f;
                int half = len >> 1;
                for (int j = 0; j < half; j++) {
                    int u = i + j;
                    int v = i + j + half;
                    float ur = real[u];
                    float ui = imag[u];
                    float vr = real[v] * wr - imag[v] * wi;
                    float vi = real[v] * wi + imag[v] * wr;
                    real[u] = ur + vr;
                    imag[u] = ui + vi;
                    real[v] = ur - vr;
                    imag[v] = ui - vi;
                    float nextWr = wr * wLenR - wi * wLenI;
                    wi = wr * wLenI + wi * wLenR;
                    wr = nextWr;
                }
            }
        }
    }
}
