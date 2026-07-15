package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AudioInputDeviceSelector;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/** Coordinates sample actions for the Voices workspace without owning its layout state. */
final class VoiceSampleActions {
    private final DocuPodcastShellViewModel viewModel;
    private final Context context;
    private final BooleanProperty samplePlaybackRunning = new SimpleBooleanProperty(false);
    private PauseTransition samplePlaybackReset;
    private AudioInputDeviceSelector inputDeviceSelector;

    VoiceSampleActions(DocuPodcastShellViewModel viewModel, Context context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    VBox actionGroups(boolean includeExport) {
        inputDeviceSelector = new AudioInputDeviceSelector(viewModel.audioInputDevices());
        Label microphone = new Label("Microfono");
        microphone.getStyleClass().add("voice-library-body");
        Button importOwnSample = ActionButtonFactory.secondary("Importar audio",
                "Importar audio de muestra para el tono seleccionado.", this::chooseAndImportOwnVoiceSample);
        importOwnSample.disableProperty().bind(viewModel.voiceRecordingRunningProperty());
        Button startRecording = ActionButtonFactory.primary("● Grabar", "Grabar muestra", this::startOwnVoiceRecording);
        startRecording.disableProperty().bind(viewModel.voiceRecordingRunningProperty());
        Button stopRecording = ActionButtonFactory.secondary("■ Detener", "Detener y guardar", this::stopOwnVoiceRecording);
        stopRecording.disableProperty().bind(Bindings.not(viewModel.voiceRecordingRunningProperty()));
        Button cancelRecording = ActionButtonFactory.secondary("✕ Cancelar", "Cancelar grabación", this::cancelOwnVoiceRecording);
        cancelRecording.disableProperty().bind(Bindings.not(viewModel.voiceRecordingRunningProperty()));
        Button playSample = ActionButtonFactory.secondary("▶ Escuchar", "Reproducir muestra", this::playSelectedToneSample);
        playSample.textProperty().bind(Bindings.when(samplePlaybackRunning)
                .then("▶ Reproduciendo muestra...")
                .otherwise("▶ Escuchar"));
        playSample.disableProperty().bind(Bindings.or(viewModel.voiceRecordingRunningProperty(), samplePlaybackRunning));
        Button repeatRecording = ActionButtonFactory.secondary("↺ Repetir", "Grabar muestra otra vez", this::startOwnVoiceRecording);
        repeatRecording.disableProperty().bind(viewModel.voiceRecordingRunningProperty());
        Button deleteSample = ActionButtonFactory.secondary("Eliminar muestra", this::deleteSelectedToneSample);
        deleteSample.disableProperty().bind(viewModel.voiceRecordingRunningProperty());

        VBox groups = new VBox(6);
        groups.getStyleClass().add("voice-sample-action-groups");
        VoiceActionStrip createSample = VoiceActionStrip.of(importOwnSample, startRecording);
        createSample.getStyleClass().add("voice-sample-action-row");
        VoiceActionStrip recording = VoiceActionStrip.of(stopRecording, cancelRecording);
        recording.getStyleClass().add("voice-sample-action-row");
        if (includeExport) {
            Button exportSample = ActionButtonFactory.secondary("Exportar", "Exportar muestra", this::exportSelectedToneSample);
            exportSample.disableProperty().bind(viewModel.voiceRecordingRunningProperty());
            VoiceActionStrip currentSample = VoiceActionStrip.of(playSample, repeatRecording, exportSample, deleteSample);
            currentSample.getStyleClass().add("voice-sample-action-row");
            groups.getChildren().addAll(microphone, inputDeviceSelector, createSample, recording, currentSample);
        } else {
            VoiceActionStrip currentSample = VoiceActionStrip.of(playSample, repeatRecording, deleteSample);
            currentSample.getStyleClass().add("voice-sample-action-row");
            groups.getChildren().addAll(microphone, inputDeviceSelector, createSample, recording, currentSample);
        }
        return groups;
    }

    private void chooseAndImportOwnVoiceSample() {
        FileChooser chooser = new FileChooser();
        VoiceToneRecordingPlan plan = context.currentToneRecordingPlan();
        chooser.setTitle("Importar muestra de voz · " + plan.toneDisplayName());
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio compatible (*.wav, *.mp3, *.flac, *.ogg, *.m4a)", "*.wav", "*.mp3", "*.flac", "*.ogg", "*.m4a"),
                new FileChooser.ExtensionFilter("WAV recomendado (*.wav)", "*.wav")
        );
        File file = chooser.showOpenDialog(context.ownerWindow());
        if (file == null) {
            return;
        }
        try {
            VoiceProfile voice = context.selectedVoiceForSample();
            viewModel.importVoiceSample(voice, file.toPath(), plan.tone());
            context.refreshVoices();
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo importar la muestra del tono " + plan.toneDisplayName() + ": " + ex.getMessage());
        }
    }

    private void startOwnVoiceRecording() {
        VoiceToneRecordingPlan plan = context.currentToneRecordingPlan();
        try {
            VoiceProfile voice = context.selectedVoiceForSample();
            viewModel.startVoiceRecording(voice, plan.tone(), selectedInputDeviceId());
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo iniciar la grabación del tono " + plan.toneDisplayName() + ": " + ex.getMessage());
        }
    }

    private void stopOwnVoiceRecording() {
        try {
            viewModel.stopOwnVoiceRecording();
            context.refreshVoices();
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo detener o guardar la grabación: " + ex.getMessage());
        }
    }

    private String selectedInputDeviceId() {
        return inputDeviceSelector == null ? "" : inputDeviceSelector.selectedDeviceId();
    }

    private void cancelOwnVoiceRecording() {
        try {
            viewModel.cancelOwnVoiceRecording();
            context.showSummary("Grabación cancelada. La muestra anterior se conserva.");
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo cancelar la grabación: " + ex.getMessage());
        }
    }

    private void playSelectedToneSample() {
        VoiceReferenceTone tone = context.selectedReferenceTone();
        try {
            viewModel.playVoiceReferenceSample(context.selectedVoice(), tone);
            context.markSamplePlaybackStarted(tone);
            startSamplePlaybackUiTimer(tone);
            context.showSummary("Reproduciendo muestra " + tone.displayName() + " de " + context.selectedVoiceName() + ".");
        } catch (IOException | RuntimeException ex) {
            samplePlaybackRunning.set(false);
            context.showSummary("No se pudo reproducir la muestra del tono seleccionado: " + ex.getMessage());
        }
    }

    private void startSamplePlaybackUiTimer(VoiceReferenceTone tone) {
        samplePlaybackRunning.set(true);
        if (samplePlaybackReset != null) {
            samplePlaybackReset.stop();
        }
        long durationMillis = Math.max(1200L, context.selectedSampleDurationMillis(tone) + 250L);
        samplePlaybackReset = new PauseTransition(Duration.millis(durationMillis));
        samplePlaybackReset.setOnFinished(event -> {
            samplePlaybackRunning.set(false);
            context.markSamplePlaybackFinished(tone);
        });
        samplePlaybackReset.playFromStart();
    }

    private void exportSelectedToneSample() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Exportar muestra de voz");
        File selected = chooser.showDialog(context.ownerWindow());
        if (selected == null) {
            context.showSummary("Exportación cancelada. No se copió ninguna muestra.");
            return;
        }
        VoiceReferenceTone tone = context.selectedReferenceTone();
        try {
            viewModel.downloadVoiceReferenceSample(context.selectedVoice(), tone, selected.toPath());
            context.showSummary("Muestra exportada desde el tono " + tone.displayName() + " de " + context.selectedVoiceName() + ".");
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo exportar la muestra del tono seleccionado: " + ex.getMessage());
        }
    }

    private void deleteSelectedToneSample() {
        VoiceProfile voice = context.selectedVoice();
        if (voice == null) {
            context.showSummary("Selecciona una voz antes de eliminar una muestra.");
            return;
        }
        VoiceReferenceTone tone = context.selectedReferenceTone();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar muestra de voz");
        confirm.setHeaderText("Eliminar la muestra del tono " + tone.displayName());
        confirm.setContentText("Se retirará la referencia del proyecto y, si el archivo pertenece a la carpeta gestionada por DocuPodcast, también se eliminará ese archivo. Esta acción no borra archivos externos originales.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            context.showSummary("Eliminación cancelada. La muestra anterior se conserva.");
            return;
        }
        try {
            viewModel.deleteVoiceReferenceSample(voice, tone);
            context.showSummary("Muestra eliminada del tono " + tone.displayName() + " de " + context.selectedVoiceName() + ".");
            context.renderCurrentLibrary();
        } catch (IOException | RuntimeException ex) {
            context.showSummary("No se pudo eliminar la muestra del tono seleccionado: " + ex.getMessage());
        }
    }

    interface Context {
        Window ownerWindow();

        VoiceToneRecordingPlan currentToneRecordingPlan();

        VoiceProfile selectedVoiceForSample() throws IOException;

        VoiceProfile selectedVoice();

        VoiceReferenceTone selectedReferenceTone();

        String selectedVoiceName();

        void refreshVoices();

        void renderCurrentLibrary();

        void showSummary(String... lines);

        void markSamplePlaybackStarted(VoiceReferenceTone tone);

        void markSamplePlaybackFinished(VoiceReferenceTone tone);

        long selectedSampleDurationMillis(VoiceReferenceTone tone);
    }
}
