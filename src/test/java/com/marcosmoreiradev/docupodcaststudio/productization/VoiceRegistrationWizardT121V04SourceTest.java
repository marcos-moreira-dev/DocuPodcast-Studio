package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceRegistrationWizardT121V04SourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void wizardUsesAdvancedVoiceLabelAndNotTechnicalEngineName() throws IOException {
        String plan = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceRegistrationWizardPlan.java"));
        String useCase = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/BuildVoiceRegistrationWizardPlanUseCase.java"));

        assertTrue(useCase.contains("Voz IA avanzada"));
        assertFalse(plan.contains("Coqui"));
        assertFalse(plan.contains("XTTS"));
    }

    @Test
    void wizardProvidesPromptPerToneAndRecordingCancelContract() throws IOException {
        String tone = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceTone.java"));
        String recordingPlan = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceToneRecordingPlan.java"));
        String useCase = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/BuildVoiceToneRecordingPlanUseCase.java"));

        assertTrue(tone.contains("suggestedRecordingPrompt"));
        assertTrue(tone.contains("BORED"));
        assertTrue(tone.contains("HEROIC"));
        assertTrue(tone.contains("SEDUCTIVE_NON_EXPLICIT"));
        assertTrue(recordingPlan.contains("cancelKeepsPreviousSample"));
        assertTrue(useCase.contains("Detener grabacion"));
        assertTrue(useCase.contains("Guardar muestra"));
    }

    @Test
    void voiceServicesExposeWizardPlans() throws IOException {
        String services = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VoiceApplicationServices.java"));
        assertTrue(services.contains("BuildVoiceRegistrationWizardPlanUseCase"));
        assertTrue(services.contains("BuildVoiceToneRecordingPlanUseCase"));
    }
}
