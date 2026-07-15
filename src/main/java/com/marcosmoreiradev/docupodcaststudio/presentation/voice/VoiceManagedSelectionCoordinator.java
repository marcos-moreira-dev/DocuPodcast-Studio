package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.control.TextArea;

/** Keeps Manage Voices selection effects explicit without growing the workspace view. */
final class VoiceManagedSelectionCoordinator {
    private VoiceManagedSelectionCoordinator() {
    }

    static void synchronizeEngine(DocuPodcastShellViewModel viewModel, VoiceProfile voice) {
        if (voice == null) {
            return;
        }
        if (VoiceProfilePresentationPolicy.simpleVoice(voice)) {
            viewModel.selectDocumentAudioSource("Voz local simple");
        } else if (VoiceProfilePresentationPolicy.advancedVoice(voice)) {
            viewModel.selectDocumentAudioSource("Voz IA avanzada");
        }
    }

    static void resetGeneratedTest(DocuPodcastShellViewModel viewModel, TextArea generatedTestText, VoiceProfile voice) {
        if (voice == null) {
            viewModel.resetGeneratedVoiceTestForSelection("Sin voz de prueba generada.");
            generatedTestText.setText("");
            return;
        }
        generatedTestText.setText(defaultGeneratedTestPhrase(voice));
        viewModel.resetGeneratedVoiceTestForSelection("Sin prueba generada para " + VoiceProfilePresentationPolicy.displayName(voice) + ".");
    }

    static String defaultGeneratedTestPhrase(VoiceProfile voice) {
        return VoiceProfilePresentationPolicy.simpleVoice(voice)
                ? "Esta es una prueba breve de lectura local simple."
                : "Esta es una prueba breve con la voz avanzada seleccionada.";
    }
}
