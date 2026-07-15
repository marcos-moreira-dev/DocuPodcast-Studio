package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPrompt;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.detachNode;

/** Summary panel for selecting a voice and generating a contextual test. */
final class VoiceManageOverviewPanel extends VBox {
    VoiceManageOverviewPanel(
            VoiceProfile voice,
            String statusText,
            List<VoiceToneRecordingPrompt> registeredPrompts,
            ComboBox<VoiceToneRecordingPrompt> toneSelector,
            TextArea generatedTestText,
            Label generatedTestStatus,
            Button generate,
            Button play,
            Button newVoice,
            Button manageVoice
    ) {
        super(12);
        if (voice == null) {
            manageVoice.setDisable(true);
            getChildren().addAll(note("Selecciona una voz registrada para generar una prueba corta o abrir su gestión completa."),
                    VoiceActionStrip.of(newVoice, manageVoice));
            return;
        }
        boolean simpleVoice = VoiceProfilePresentationPolicy.simpleVoice(voice);
        boolean predefinedVoice = VoiceProfilePresentationPolicy.predefinedVoice(voice);
        if (simpleVoice) {
            manageVoice.setDisable(true);
            manageVoice.setTooltip(new Tooltip("La voz simple no usa muestras ni emociones."));
        } else if (predefinedVoice) {
            manageVoice.setDisable(true);
            manageVoice.setTooltip(new Tooltip("Las voces prediseñadas no se editan. Crea una voz nueva para registrar tus muestras."));
        }
        addToneSelectorIfNeeded(voice, registeredPrompts, toneSelector);
        if (generatedTestText.getText() == null || generatedTestText.getText().isBlank()) {
            generatedTestText.setText(simpleVoice
                    ? "Esta es una prueba breve de lectura local simple."
                    : "Esta es una prueba breve con la voz avanzada seleccionada.");
        }
        detachNode(generatedTestText);
        detachNode(generatedTestStatus);
        getChildren().add(new VoiceGeneratedTestPanel("Prueba de voz",
                simpleVoice
                        ? "Voz simple seleccionada"
                        : "Voz avanzada y emoción seleccionada",
                simpleVoice
                        ? "Genera un WAV corto con el motor de voz simple."
                        : "Genera un WAV corto con el motor avanzado. Si seleccionas una emoción registrada, la prueba usará esa muestra.",
                generatedTestText, generatedTestStatus, generate, play));
        getChildren().add(note("Uso responsable: registra únicamente voces propias o autorizadas. Las muestras orientan texto nuevo; no son clips fijos para repetir."));
        if (simpleVoice) {
            getChildren().add(note("La voz simple no usa muestras ni emociones."));
        }
        getChildren().add(VoiceActionStrip.of(newVoice, manageVoice));
    }

    private void addToneSelectorIfNeeded(VoiceProfile voice, List<VoiceToneRecordingPrompt> registeredPrompts, ComboBox<VoiceToneRecordingPrompt> toneSelector) {
        if (VoiceProfilePresentationPolicy.simpleVoice(voice)) {
            return;
        }
        if (registeredPrompts == null || registeredPrompts.isEmpty()) {
            getChildren().add(note("Sin emociones registradas. Abre Gestionar voz seleccionada y registra Neutral para habilitar pruebas por muestra."));
            return;
        }
        Label emotionLabel = new Label("Emoción registrada para la prueba");
        emotionLabel.getStyleClass().add("voice-field-label");
        detachNode(toneSelector);
        toneSelector.setMaxWidth(360);
        getChildren().addAll(emotionLabel, toneSelector);
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }
}
