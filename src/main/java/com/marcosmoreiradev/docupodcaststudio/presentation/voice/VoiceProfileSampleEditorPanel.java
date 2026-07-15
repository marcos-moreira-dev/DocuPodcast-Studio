package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPrompt;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.masterDetail;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.moduleRoot;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.section;

/** Focused editor for creating or managing one advanced voice profile and its reference samples. */
final class VoiceProfileSampleEditorPanel extends VBox {
    VoiceProfileSampleEditorPanel(
            boolean editingExisting,
            TextField voiceNameField,
            ComboBox<VoiceToneRecordingPrompt> tonePromptSelector,
            Label tonePromptText,
            Label toneRecordingContract,
            Label selectedSampleStatus,
            VBox sampleActions,
            VBox readyTones,
            Button back,
            Button save,
            Button delete,
            Button export
    ) {
        super(12);
        getStyleClass().add("voice-profile-sample-editor");
        String title = editingExisting ? "Gestionar voz" : "Nueva voz";
        String summary = editingExisting
                ? "Edita el nombre y las muestras de referencia por emoción."
                : "Registra una voz avanzada con Neutral como muestra base.";
        getChildren().addAll(
                moduleRoot(title, "Muestras de referencia", summary),
                backButton(back),
                masterDetail(identitySection(voiceNameField, save, delete, export),
                        sampleSection(tonePromptSelector, tonePromptText, toneRecordingContract, selectedSampleStatus, sampleActions, readyTones))
        );
    }

    private static Button backButton(Button back) {
        back.getStyleClass().add("voice-editor-back");
        return back;
    }

    private static VBox identitySection(TextField voiceNameField, Button save, Button delete, Button export) {
        VBox identity = section("Voz");
        Label nameLabel = fieldLabel("Nombre de la voz");
        Label neutralRequirement = note("Guardar exige nombre y muestra Neutral. Las demás emociones son opcionales.");
        VoiceActionStrip finalActions = actionStrip(save, delete, export);
        identity.getChildren().addAll(nameLabel, voiceNameField, neutralRequirement, finalActions,
                note("Exportar muestras copia los audios de referencia registrados para esta voz."));
        return identity;
    }

    private static VBox sampleSection(
            ComboBox<VoiceToneRecordingPrompt> tonePromptSelector,
            Label tonePromptText,
            Label toneRecordingContract,
            Label selectedSampleStatus,
            VBox sampleActions,
            VBox readyTones
    ) {
        VBox sample = section("Referencia sonora por emoción");
        tonePromptText.setWrapText(true);
        tonePromptText.getStyleClass().add("voice-reference-phrase");
        tonePromptText.getStyleClass().add("voice-interpretation-phrase");
        toneRecordingContract.setWrapText(true);
        toneRecordingContract.getStyleClass().add("document-side-text");
        Label emotionLabel = fieldLabel("Emoción de referencia");
        Label phraseLabel = fieldLabel("Frase para interpretar");
        Label sampleStateLabel = fieldLabel("Estado de muestra");
        Label readyTonesLabel = fieldLabel("Emociones listas");
        sample.getChildren().addAll(emotionLabel, tonePromptSelector,
                phraseLabel, tonePromptText, toneRecordingContract,
                sampleStateLabel, selectedSampleStatus,
                sampleActions,
                readyTonesLabel, readyTones,
                note("Importar o grabar reemplaza la muestra de la emoción seleccionada."),
                note("Escuchar reproduce la muestra grabada o importada de la emoción seleccionada."));
        return sample;
    }

    private static VoiceActionStrip actionStrip(Node... nodes) {
        List<Node> actions = new ArrayList<>();
        for (Node node : nodes) {
            if (node != null) {
                actions.add(node);
            }
        }
        return VoiceActionStrip.of(actions.toArray(Node[]::new));
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("voice-field-label");
        return label;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }
}
