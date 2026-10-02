package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.util.Arrays;

/** Conservative PCM boundary treatment; never removes internal or trailing samples. */
final class QwenLeadingSilence {
    private QwenLeadingSilence() {}

    static short[] normalize(short[] samples, int sampleRate) throws IOException {
        int window = Math.max(1, sampleRate / 100); // 10 ms
        int firstQuietSignal = -1;
        int consecutiveQuiet = 0;
        int consecutiveActive = 0;
        boolean audible = false;
        for (int offset = 0; offset < samples.length; offset += window) {
            int end = Math.min(samples.length, offset + window);
            double energy = 0;
            for (int i = offset; i < end; i++) energy += (double) samples[i] * samples[i];
            double rms = Math.sqrt(energy / (end - offset));
            // About -72 dBFS: retain weak onsets before the main syllable.
            consecutiveQuiet = rms >= 8 ? consecutiveQuiet + 1 : 0;
            if (firstQuietSignal < 0 && consecutiveQuiet >= 3) firstQuietSignal = offset - 2 * window;
            // About -60 dBFS sustained for 30 ms; a lone click is insufficient.
            consecutiveActive = rms >= 32 ? consecutiveActive + 1 : 0;
            if (consecutiveActive >= 3) audible = true;
        }
        if (!audible) {
            throw new AudioQualityException("Qwen produjo un audio sin señal audible suficiente; vuelve a generar este fragmento.");
        }
        // Leave ordinary short attack pauses intact. Only shorten excessive pre-roll.
        if (firstQuietSignal <= sampleRate / 4) return samples;
        int cut = Math.max(0, firstQuietSignal - sampleRate * 120 / 1000);
        return Arrays.copyOfRange(samples, cut, samples.length);
    }
}
