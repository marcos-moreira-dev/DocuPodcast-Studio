package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVoiceToneRecordingPlanUseCaseTest {
    @Test
    void buildsToneSpecificRecordingPlanWithPromptAndSafeFilename() {
        VoiceToneRecordingPlan plan = new BuildVoiceToneRecordingPlanUseCase()
                .build("VOC-PEPITA", "Pepita Alegre", VoiceReferenceTone.ANGRY);

        assertEquals(VoiceReferenceTone.ANGRY, plan.tone());
        assertEquals("Enojada", plan.toneDisplayName());
        assertTrue(plan.promptText().contains("No puedo aceptar"));
        assertTrue(plan.suggestedFileName().contains("pepita_alegre"));
        assertTrue(plan.suggestedFileName().contains("enojada"));
        assertTrue(plan.suggestedFileName().endsWith(".wav"));
        assertEquals("Cancelar", plan.cancelLabel());
        assertEquals("Detener grabacion", plan.stopLabel());
        assertEquals("Guardar muestra", plan.saveLabel());
        assertTrue(plan.cancelKeepsPreviousSample());
    }

    @Test
    void defaultsToNeutralToneWhenToneIsMissing() {
        VoiceToneRecordingPlan plan = new BuildVoiceToneRecordingPlanUseCase()
                .build("VOC-PEPITA", "Pepita", null);

        assertEquals(VoiceReferenceTone.NEUTRAL, plan.tone());
        assertEquals("Neutral", plan.toneDisplayName());
        assertTrue(plan.promptText().contains("claridad"));
    }
}
