package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVoiceRegistrationWizardPlanUseCaseTest {
    @Test
    void buildsAdvancedVoiceWizardWithoutTechnicalEngineName() {
        VoiceRegistrationWizardPlan plan = new BuildVoiceRegistrationWizardPlanUseCase().build("Pepita", true);

        assertEquals("Pepita", plan.voiceDisplayName());
        assertEquals("Voz IA avanzada", plan.visibleEngineLabel());
        assertTrue(plan.usesVisibleAdvancedVoiceLabelOnly());
        assertEquals(VoiceReferenceTone.NEUTRAL, plan.neutralPrompt().tone());
        assertTrue(plan.neutralPrompt().required());
        assertFalse(plan.neutralPrompt().suggestedRecordingPrompt().isBlank());
        assertTrue(plan.recommendedPrompts().stream().anyMatch(prompt -> prompt.tone() == VoiceReferenceTone.BORED));
        assertTrue(plan.theatricalPrompts().stream().anyMatch(prompt -> prompt.tone() == VoiceReferenceTone.HEROIC));
        assertTrue(plan.theatricalPrompts().stream().anyMatch(prompt -> prompt.tone() == VoiceReferenceTone.SEDUCTIVE_NON_EXPLICIT));
        assertEquals("Generar prueba con esta voz", plan.generatedTestActionLabel());
    }

    @Test
    void canHideExtendedTheatricalCatalogForSimpleCreation() {
        VoiceRegistrationWizardPlan plan = new BuildVoiceRegistrationWizardPlanUseCase().build("María", false);

        assertTrue(plan.theatricalPrompts().isEmpty());
        assertTrue(plan.recommendedPrompts().size() >= 8);
        assertTrue(plan.expressiveReferenceNotice().contains("muestras de referencia"));
    }
}
