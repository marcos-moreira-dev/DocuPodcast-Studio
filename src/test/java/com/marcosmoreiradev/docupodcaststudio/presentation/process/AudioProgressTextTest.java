package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioProgressTextTest {
    private AudioJobStatusDto status(AudioGenerationStage stage) {
        return new AudioJobStatusDto("job", "document", AudioJobState.GENERATING_AUDIO,
                stage, 20, 100, 0, .2, "unit", "", 600, 90, "", "", "", "");
    }

    @Test void generationKeepsEstimateAndUncertainty() {
        assertEquals("Generación de voz: Faltan aproximadamente 10 min 0 s ± 1 min 30 s.",
                AudioProgressText.timing(status(AudioGenerationStage.GENERATING_SEGMENTS)));
    }

    @Test void waitingAndMergingDoNotReuseGenerationEstimate() {
        for (var stage : new AudioGenerationStage[] {AudioGenerationStage.WAITING_FOR_RESOURCES,
                AudioGenerationStage.MERGING_SEGMENTS}) {
            assertFalse(AudioProgressText.timing(status(stage)).contains("10 min"));
            assertFalse(AudioProgressText.timing(status(stage)).contains("±"));
        }
        assertEquals("Uniendo fragmentos de audio", AudioProgressText.title(status(AudioGenerationStage.MERGING_SEGMENTS)));
    }

    @Test void allStagesHaveSpecificText() {
        for (var stage : AudioGenerationStage.values()) {
            assertFalse(AudioProgressText.title(status(stage)).isBlank());
            assertFalse(AudioProgressText.timing(status(stage)).isBlank());
        }
    }
}
