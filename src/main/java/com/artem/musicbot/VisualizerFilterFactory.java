package com.artem.musicbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public final class VisualizerFilterFactory implements PcmFilterFactory {
    private final AudioVisualizer visualizer;
    private final EqualizerFactory equalizer;

    public VisualizerFilterFactory(AudioVisualizer visualizer, EqualizerFactory equalizer) {
        this.visualizer = visualizer;
        this.equalizer = equalizer;
    }

    @Override
    public List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output) {
        VisualizerFilter visualizerFilter = new VisualizerFilter(output, visualizer, format.sampleRate, format.channelCount);
        if (equalizer == null) {
            return Collections.singletonList(visualizerFilter);
        }

        List<AudioFilter> eqFilters = equalizer.buildChain(track, format, visualizerFilter);
        if (eqFilters.isEmpty()) {
            return Collections.singletonList(visualizerFilter);
        }

        List<AudioFilter> filters = new ArrayList<>(eqFilters.size() + 1);
        filters.addAll(eqFilters);
        filters.add(visualizerFilter);
        return filters;
    }
}
