package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryWizardWiringT121V04CSourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void voiceLibraryUsesWizardPlansAndNoStaticPrompt() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");

        assertTrue(view.contains("VoiceRegistrationWizardPlan"));
        assertTrue(view.contains("VoiceToneRecordingPlan"));
        assertTrue(view.contains("voiceRegistrationWizardPlan"));
        assertTrue(view.contains("voiceToneRecordingPlan"));
        assertTrue(view.contains("tonePromptSelector"));
        assertTrue(view.contains("wizard.neutralPrompt()"));
        assertTrue(view.contains("wizard.recommendedPrompts()"));
        assertFalse(view.contains("REFERENCE_PHRASE"));
        assertFalse(view.contains("Lee esta frase con voz clara, natural y constante"));
    }

    @Test
    void voiceLibraryImportsAndRecordsSelectedTone() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceSampleWorkflowCoordinator.java");

        assertTrue(view.contains("selectedReferenceTone"));
        assertTrue(actions.contains("viewModel.importVoiceSample(voice, file.toPath(), plan.tone())"));
        assertTrue(actions.contains("AudioInputDeviceSelector"));
        assertTrue(actions.contains("viewModel.startVoiceRecording(voice, plan.tone(), selectedInputDeviceId())"));
        assertTrue(viewModel.contains("activeVoiceRecordingTone"));
        assertTrue(viewModel.contains("startVoiceRecording(VoiceProfile voice, VoiceReferenceTone tone, String inputDeviceId)"));
        assertTrue(coordinator.contains("forOwnVoiceTone"));
        assertTrue(viewModel.contains("VoiceReferenceTone.NEUTRAL"));
    }

    @Test
    void voiceLibraryKeepsTransversalStyledComponentsAndFriendlyLanguage() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String profileCard = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java");
        String engineControls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java");

        assertTrue(view.contains("ActionButtonFactory.primary"));
        assertTrue(view.contains("ActionButtonFactory.secondary"));
        assertTrue(profileCard.contains("voice-profile-card"));
        assertTrue(editor.contains("voice-reference-phrase"));
        assertTrue(view.contains("Voz IA avanzada"));
        assertTrue(engineControls.contains("Voz local simple"));
        assertTrue(engineControls.contains("Modo de prueba"));
        assertFalse(view.contains("Coqui"));
        assertFalse(view.contains("XTTS"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }
}
