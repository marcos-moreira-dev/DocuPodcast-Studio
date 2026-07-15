package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

/**
 * Legacy compact action rail kept for advanced surfaces. The main T81D assignment
 * workflow lives in {@link DocumentAudioNarrationPanel} and {@link DocumentImageContextPanel}.
 */
public final class DocumentLayerRailView extends VBox {
    private final DocuPodcastShellViewModel viewModel;

    public DocumentLayerRailView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("document-layer-rail");
        setPadding(new Insets(10));
        setSpacing(8);

        Label selected = new Label();
        selected.getStyleClass().add("document-layer-selected");
        selected.textProperty().bind(viewModel.selectedDocumentRangeLabelProperty());
        selected.setWrapText(true);

        Label rule = new Label("Las capas se guardan en el proyecto DocuPodcast: no se escriben dentro del Word original.");
        rule.getStyleClass().add("document-layer-rule");
        rule.setWrapText(true);

        Label protection = new Label("Protección: una voz o audio principal no puede pisar otro rango sin reemplazar, dividir o desasignar.");
        protection.getStyleClass().add("document-layer-rule");
        protection.setWrapText(true);

        Button removePrimary = ActionButtonFactory.secondary("Quitar capa principal", viewModel::removePrimaryAssignmentForSelectedDocumentRange);
        removePrimary.getStyleClass().add("document-layer-secondary-action");

        getChildren().addAll(
                selected,
                actionButton("Asignar voz IA", () -> viewModel.prepareDocumentLayerAssignment(NarrativeLayerKind.VOICE)),
                actionButton("Elegir audio", this::chooseAudioFromComputer),
                actionButton("Extraer video", this::extractAudioFromVideo),
                actionButton("Asignar tono / intención", () -> viewModel.prepareDocumentLayerAssignment(NarrativeLayerKind.EMOTION)),
                actionButton("Elegir imagen", () -> viewModel.prepareDocumentLayerAssignment(NarrativeLayerKind.IMAGE)),
                removePrimary,
                rule,
                protection
        );
    }

    private Button actionButton(String text, Runnable handler) {
        Button button = ActionButtonFactory.rail(text, handler);
        button.getStyleClass().add("document-layer-action");
        button.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            return blockId == null || blockId.isBlank();
        }, viewModel.selectedDocumentBlockIdProperty()));
        return button;
    }

    private void chooseAudioFromComputer() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir audio del computador");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio compatible (*.wav, *.mp3, *.m4a, *.flac, *.ogg)", "*.wav", "*.mp3", "*.m4a", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("WAV directo (*.wav)", "*.wav")
        );
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }
        try {
            viewModel.importUserAudioForSelectedDocumentRange(file.toPath());
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo importar el audio: " + ex.getMessage());
        }
    }

    private void extractAudioFromVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Extraer audio de video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video compatible (*.mp4, *.mov, *.mkv, *.webm)", "*.mp4", "*.mov", "*.mkv", "*.webm"),
                new FileChooser.ExtensionFilter("MP4 recomendado (*.mp4)", "*.mp4")
        );
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }
        try {
            viewModel.extractVideoAudioForSelectedDocumentRange(file.toPath());
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo extraer audio del video: " + ex.getMessage());
        }
    }

    private Window ownerWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }
}

