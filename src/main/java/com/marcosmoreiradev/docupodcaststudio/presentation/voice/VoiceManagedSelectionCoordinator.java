package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.control.TextArea;

/** Keeps voice-selection effects explicit without coupling a character to a synthesis provider. */
final class VoiceManagedSelectionCoordinator {
    private VoiceManagedSelectionCoordinator() {
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
