package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceSpanTest {
    @Test
    void supportsAiVoiceAssignmentsOverTextRangesWithOptionalImage() {
        PerformanceSpan span = new PerformanceSpan("PER-001", new ScriptTextRange("SEG-001", 0, 12),
                VoiceSourceKind.AI_TTS, "VOC-001", "", "STY-NEUTRAL", "IMG-001", "");

        assertTrue(span.usesAiVoice());
    }

    @Test
    void humanRecordingRequiresAudioAsset() {
        assertThrows(IllegalArgumentException.class, () -> new PerformanceSpan("PER-001",
                new ScriptTextRange("SEG-001", 0, 12), VoiceSourceKind.HUMAN_RECORDING,
                "", "", "", "", ""));
    }
}
