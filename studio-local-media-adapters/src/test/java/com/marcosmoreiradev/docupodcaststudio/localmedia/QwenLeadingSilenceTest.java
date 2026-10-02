package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class QwenLeadingSilenceTest {
    private static final int RATE = 24_000;

    @Test void trimsOnlyExcessivePrefixAndPreservesEveryFollowingSample() throws Exception {
        short[] input = new short[RATE * 4];
        tone(input, RATE / 2, RATE);
        tone(input, RATE * 2, RATE * 5 / 2); // Internal pause and long final pause.
        short[] result = QwenLeadingSilence.normalize(input, RATE);
        int cut = RATE / 2 - RATE * 120 / 1000;
        assertArrayEquals(Arrays.copyOfRange(input, cut, input.length), result);
    }

    @Test void keepsOrdinaryShortInitialPause() throws Exception {
        short[] input = new short[RATE];
        tone(input, RATE / 10, RATE / 2);
        assertArrayEquals(input, QwenLeadingSilence.normalize(input, RATE));
    }

    @Test void protectsWeakOnsetBeforeMainVoice() throws Exception {
        short[] input = new short[RATE * 2];
        Arrays.fill(input, RATE / 2, RATE * 3 / 4, (short) 12);
        tone(input, RATE * 3 / 4, RATE);
        int cut = RATE / 2 - RATE * 120 / 1000;
        assertArrayEquals(Arrays.copyOfRange(input, cut, input.length), QwenLeadingSilence.normalize(input, RATE));
    }

    @Test void rejectsSilenceAndIsolatedClick() {
        assertThrows(IOException.class, () -> QwenLeadingSilence.normalize(new short[RATE], RATE));
        short[] click = new short[RATE];
        click[RATE / 2] = 30_000;
        assertThrows(IOException.class, () -> QwenLeadingSilence.normalize(click, RATE));
    }

    @Test void isolatedNoiseBeforeVoiceDoesNotPreventPrefixCleanup() throws Exception {
        short[] input = new short[RATE * 2];
        input[RATE / 10] = 2000;
        tone(input, RATE / 2, RATE);
        int cut = RATE / 2 - RATE * 120 / 1000;
        assertArrayEquals(Arrays.copyOfRange(input, cut, input.length), QwenLeadingSilence.normalize(input, RATE));
    }

    private static void tone(short[] samples, int start, int end) {
        for (int i = start; i < end; i++) samples[i] = (short) (4000 * Math.sin(2 * Math.PI * 220 * i / RATE));
    }
}
