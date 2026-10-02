package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AudioInputDeviceSelector;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Textual theatre map: intervention IDs and scene starts over the shared document. */
public final class TheatreTextualMapPanel extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final IntervencionBoundaryStore boundaryStore;
    private final TheatreSceneFoldList folds;
    private final StringProperty selectedIntervencion = new SimpleStringProperty("");
    private String awaitingSceneId = "";
    private BoundaryCapture awaitingBoundary = BoundaryCapture.NONE;

    public TheatreTextualMapPanel(DocuPodcastShellViewModel viewModel, IntervencionBoundaryStore boundaryStore) {
        this.viewModel = viewModel;
        this.boundaryStore = boundaryStore == null ? new IntervencionBoundaryStore() : boundaryStore;
        getStyleClass().add("theatre-textual-map-panel");
        setPadding(new Insets(10));
        setFocusTraversable(true);

        folds = new TheatreSceneFoldList(viewModel, this.boundaryStore, this::sceneContent);

        VBox body = new VBox(10,
                new SectionHeader("Mapa textual", "Ajusta inicio y final de escenas; clic derecho en una intervencion para procesarla, grabar narracion/efecto sonido, exportar paquete IA o ver contexto."),
                folds);
        body.getStyleClass().add("theatre-map-body");
        VBox.setVgrow(folds, Priority.ALWAYS);
        ScrollPane scroll = StudioViewportControls.scrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("document-context-scroll", "theatre-textual-map-scroll");
        setCenter(scroll);

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !awaitingSceneId.isBlank()) {
                cancelSceneBoundaryCapture();
                event.consume();
            }
        });
        viewModel.selectedDocumentTextRangeProperty().addListener((obs, oldValue, newValue) ->
                captureSceneBoundaryIfWaiting(newValue == null ? "" : newValue.blockId()));
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) ->
                captureSceneBoundaryIfWaiting(newValue));
        this.boundaryStore.revisionProperty().addListener((obs, oldValue, newValue) -> folds.refresh());
        TheatreInterventionSelectionBridge.bind(viewModel, selectedIntervencion, this.boundaryStore);
    }

    private Node sceneContent(TheatreProjectLayer.Scene scene) {
        IntervencionBoundaryStore.SceneBoundary boundary = boundaryStore.limite(scene.id());
        Button start = boundaryButton(scene, BoundaryCapture.START);
        Button end = boundaryButton(scene, BoundaryCapture.END);
        String startAlias = idParaEscena(boundary.startId());
        String endAlias = idParaEscena(boundary.endId());

        Label startCopy = new Label(startAlias.isBlank()
                ? "Texto inicial sin definir."
                : startAlias + " seleccionado para texto inicial de escena " + scene.displayName() + ".");
        startCopy.getStyleClass().add("theatre-map-microcopy");
        startCopy.setWrapText(true);

        Label endCopy = new Label(endAlias.isBlank()
                ? "Texto final sin definir."
                : endAlias + " seleccionado para texto final de escena " + scene.displayName() + ".");
        endCopy.getStyleClass().add("theatre-map-microcopy");
        endCopy.setWrapText(true);

        HBox actions = new HBox(6, start, end);
        actions.getStyleClass().add("theatre-scene-boundary-actions");
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        List<IntervencionCatalogo.IntervencionInfo> aliases = intervencionesParaEscena(scene);
        TheatreTextSequenceCanvas canvas = new TheatreTextSequenceCanvas(
                scene.displayName(),
                TheatreWorkspaceEmptyState.interventionSequenceMessage(document, script),
                aliases,
                selectedIntervencion,
                TheatreInterventionSelectionBridge.blockSelector(viewModel, scene),
                alias -> showEmotionEditor(scene, alias),
                null,
                alias -> showManualRecordingDialog(scene, alias),
                null);
        ScrollPane canvasScroll = StudioViewportControls.scrollPane(canvas);
        canvasScroll.getStyleClass().add("theatre-action-canvas-scroll");
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);
        canvasScroll.setPrefViewportHeight(260);
        canvasScroll.setMinViewportHeight(220);
        canvasScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox box = new VBox(6, actions, startCopy, endCopy);
        TheatreWorkspaceEmptyState.preparationNotice(document, script)
                .map(TheatreTextualMapPanel::stateNote)
                .ifPresent(box.getChildren()::add);
        box.getChildren().add(canvasScroll);
        box.getStyleClass().add("theatre-textual-scene-start");
        return box;
    }

    private void showEmotionEditor(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (scene == null || alias == null) {
            return;
        }
        List<VoiceReferenceTone> tones = List.of(VoiceReferenceTone.values());
        ComboBox<VoiceReferenceTone> toneCombo = StudioFormControls.comboBox();
        toneCombo.getItems().setAll(tones);
        toneCombo.setValue(VoiceReferenceTone.NEUTRAL);
        toneCombo.setMaxWidth(Double.MAX_VALUE);
        toneCombo.getStyleClass().add("voice-library-combo");
        toneCombo.setConverter(new StringConverter<>() {
            @Override public String toString(VoiceReferenceTone tone) {
                return tone == null ? "" : tone.displayName() + " (" + tone.layerTargetId() + ")";
            }
            @Override public VoiceReferenceTone fromString(String value) { return null; }
        });

        Label title = new Label("Editar emocion de " + alias.alias());
        title.getStyleClass().add("theatre-character-dialog-title");
        Label text = new Label(alias.preview());
        text.setWrapText(true);
        text.getStyleClass().add("theatre-map-microcopy");

        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            VoiceReferenceTone selected = toneCombo.getValue();
            if (selected != null) {
                selectedIntervencion.set(alias.alias());
                viewModel.assignEmotionStyleToDocumentBlock(alias.blockId(), selected.layerTargetId());
                viewModel.updateStatusMessage(alias.alias() + " de " + scene.displayName()
                        + " usara emocion " + selected.displayName() + ".");
            }
            ((Stage) toneCombo.getScene().getWindow()).close();
        });
        Button cancel = ActionButtonFactory.secondary("Cancelar", () -> ((Stage) toneCombo.getScene().getWindow()).close());

        VBox root = new VBox(10,
                title,
                text,
                new Label("Emocion o estilo interpretativo"),
                toneCombo,
                new HBox(8, accept, cancel));
        root.setPadding(new Insets(14));
        root.getStyleClass().add("theatre-character-dialog-root");

        Stage stage = new Stage();
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Emocion de " + alias.alias());
        Scene dialog = new Scene(root, 460, 260);
        if (getScene() != null) {
            dialog.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(dialog);
        stage.show();
    }

    private void showManualRecordingDialog(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (scene == null || alias == null) {
            return;
        }
        AudioInputDeviceSelector microphone = new AudioInputDeviceSelector(viewModel.audioInputDevices());
        ObjectProperty<Path> draftRecording = new SimpleObjectProperty<>();

        Label title = new Label("Grabar audio narraci\u00f3n/efecto sonido de " + alias.displayName());
        title.getStyleClass().add("theatre-character-dialog-title");
        TextArea text = StudioFormControls.textArea(alias.fullText().isBlank()
                ? "Texto completo no disponible para esta intervencion."
                : alias.fullText());
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefRowCount(6);
        text.setMinHeight(170);
        text.getStyleClass().add("theatre-manual-script-text");
        Label state = new Label("Segmento: " + alias.blockId() + " / escena " + scene.displayName() + ".");
        state.setWrapText(true);
        state.getStyleClass().add("theatre-map-microcopy");

        Button record = ActionButtonFactory.primary("Grabar", "Grabar o detener audio humano", () ->
                runRecordingAction(state, () -> {
                    if (viewModel.manualAudioRecordingRunningProperty().get()) {
                        Path draft = viewModel.stopManualInterventionRecordingDraft(alias.blockId(), alias.displayName());
                        draftRecording.set(draft);
                        state.setText("Borrador grabado para " + alias.displayName() + ". Usa Asignar audio a intervencion para aplicarlo.");
                    } else {
                        AudioInputDevice selected = microphone.selectedDevice();
                        viewModel.startManualInterventionRecording(alias.blockId(), alias.displayName(), alias.preview(), selected.id());
                        state.setText("Grabando con " + selected.displayName() + ".");
                    }
                }));
        record.textProperty().bind(Bindings.when(viewModel.manualAudioRecordingRunningProperty())
                .then("Dejar de grabar")
                .otherwise("Grabar"));

        Button assign = ActionButtonFactory.primary("Asignar audio a intervenci\u00f3n", "Aplicar el borrador grabado a esta intervencion", () ->
                runRecordingAction(state, () -> {
                    Path draft = draftRecording.get();
                    if (draft == null) {
                        state.setText("Graba un borrador antes de asignarlo.");
                        return;
                    }
                    viewModel.assignManualInterventionRecording(alias.blockId(), alias.displayName(), draft);
                    draftRecording.set(null);
                    state.setText("Audio asignado a " + alias.displayName() + ".");
                }));
        Button listen = ActionButtonFactory.secondary("Escuchar", "Escuchar borrador o audio aplicado", () ->
                runRecordingAction(state, () -> {
                    Path draft = draftRecording.get();
                    if (draft != null && Files.isRegularFile(draft)) {
                        viewModel.playStandaloneAudio(draft);
                    } else {
                        viewModel.playManualInterventionAudio(alias.blockId());
                    }
                }));
        Button delete = ActionButtonFactory.danger("Eliminar", "Eliminar audio manual", () ->
                runRecordingAction(state, () -> {
                    Path draft = draftRecording.get();
                    if (draft != null) {
                        Files.deleteIfExists(draft);
                        draftRecording.set(null);
                        state.setText("Borrador eliminado. El audio aplicado no cambio.");
                    } else {
                        viewModel.deleteManualInterventionAudio(alias.blockId(), alias.displayName());
                        state.setText("Audio manual eliminado de " + alias.displayName() + ".");
                    }
                }));
        Button cancelRecording = ActionButtonFactory.secondary("Cancelar grabacion", "Cancelar captura activa", () ->
                runRecordingAction(state, () -> {
                    viewModel.cancelManualInterventionRecording();
                    state.setText("Grabacion cancelada.");
                }));

        microphone.disableProperty().bind(viewModel.manualAudioRecordingRunningProperty());
        listen.disableProperty().bind(viewModel.manualAudioRecordingRunningProperty());
        delete.disableProperty().bind(viewModel.manualAudioRecordingRunningProperty());
        assign.disableProperty().bind(Bindings.or(viewModel.manualAudioRecordingRunningProperty(), draftRecording.isNull()));
        cancelRecording.disableProperty().bind(Bindings.not(viewModel.manualAudioRecordingRunningProperty()));

        VBox root = new VBox(10,
                title,
                text,
                new Label("Microfono"),
                microphone,
                state,
                new HBox(8, record, cancelRecording),
                new HBox(8, listen, delete, assign));
        root.setPadding(new Insets(14));
        root.getStyleClass().add("theatre-character-dialog-root");

        Stage stage = new Stage();
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Grabar audio narraci\u00f3n/efecto sonido - " + alias.displayName());
        Scene dialog = new Scene(root, 720, 520);
        if (getScene() != null) {
            dialog.getStylesheets().addAll(getScene().getStylesheets());
        }
        dialog.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });
        stage.setScene(dialog);
        stage.show();
    }

    private void runRecordingAction(Label state, RecordingDialogAction action) {
        try {
            action.run();
        } catch (Exception ex) {
            state.setText("No se pudo completar la accion: " + ex.getMessage());
            viewModel.updateStatusMessage("No se pudo completar la accion de grabacion: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface RecordingDialogAction {
        void run() throws Exception;
    }

    private Button boundaryButton(TheatreProjectLayer.Scene scene, BoundaryCapture boundary) {
        boolean awaiting = scene.id().equals(awaitingSceneId) && awaitingBoundary == boundary;
        AppIcon icon = boundary == BoundaryCapture.START ? AppIcon.PREVIOUS_FRAGMENT : AppIcon.NEXT_FRAGMENT;
        String label = boundary == BoundaryCapture.START ? "Establecer texto inicial" : "Establecer texto final";
        String tooltip = boundary == BoundaryCapture.START
                ? "Seleccionar texto inicial de la escena"
                : "Seleccionar texto final de la escena";
        Button button = ActionButtonFactory.iconOnly(
                icon,
                awaiting ? "Selecciona un fragmento; Esc para cancelar" : tooltip,
                () -> toggleSceneBoundaryCapture(scene, boundary),
                "ui-action-button",
                "ui-action-button-secondary",
                "theatre-scene-boundary-button");
        button.setText(label);
        button.setGraphicTextGap(6);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        if (awaiting) {
            button.getStyleClass().add("theatre-scene-boundary-button-active");
        }
        return button;
    }

    private void toggleSceneBoundaryCapture(TheatreProjectLayer.Scene scene, BoundaryCapture boundary) {
        if (scene == null) {
            return;
        }
        if (scene.id().equals(awaitingSceneId) && awaitingBoundary == boundary) {
            cancelSceneBoundaryCapture();
            return;
        }
        awaitingSceneId = scene.id();
        awaitingBoundary = boundary;
        String target = boundary == BoundaryCapture.START ? "inicio" : "final";
        viewModel.updateStatusMessage("Selecciona un fragmento para marcar el " + target + " de " + scene.displayName() + ".");
        requestFocus();
        folds.refresh();
    }

    private void cancelSceneBoundaryCapture() {
        awaitingSceneId = "";
        awaitingBoundary = BoundaryCapture.NONE;
        viewModel.updateStatusMessage("Selección de límite de escena cancelada.");
        folds.refresh();
    }

    private void captureSceneBoundaryIfWaiting(String blockId) {
        if (awaitingSceneId.isBlank() || awaitingBoundary == BoundaryCapture.NONE || blockId == null || blockId.isBlank()) {
            return;
        }
        Optional<TheatreProjectLayer.Scene> scene = viewModel.theatreScenes().stream()
                .filter(candidate -> candidate.id().equals(awaitingSceneId))
                .findFirst();
        if (scene.isEmpty()) {
            cancelSceneBoundaryCapture();
            return;
        }
        Optional<String> alias = aliasForBlock(blockId);
        if (alias.isEmpty()) {
            viewModel.updateStatusMessage("Ese fragmento no tiene identificador de personaje o acotacion.");
            return;
        }
        if (awaitingBoundary == BoundaryCapture.START) {
            boundaryStore.setInicio(scene.get().id(), alias.get());
        } else {
            boundaryStore.setFin(scene.get().id(), alias.get());
        }
        viewModel.focusTheatreScene(scene.get().id());
        String target = awaitingBoundary == BoundaryCapture.START ? "inicio" : "final";
        String visibleAlias = idParaEscena(alias.get());
        awaitingSceneId = "";
        awaitingBoundary = BoundaryCapture.NONE;
        viewModel.updateStatusMessage(visibleAlias + " seleccionado para " + target + " de escena " + scene.get().displayName() + ".");
        folds.refresh();
    }

    private Optional<String> aliasForBlock(String blockId) {
        return intervencionesActuales().stream()
                .filter(alias -> alias.blockId().equals(blockId))
                .map(IntervencionCatalogo.IntervencionInfo::alias)
                .findFirst();
    }

    private List<IntervencionCatalogo.IntervencionInfo> intervencionesActuales() {
        return IntervencionCatalogo.intervenciones(
                viewModel.currentDocumentProperty().get(),
                viewModel.currentScriptProperty().get());
    }

    private List<IntervencionCatalogo.IntervencionInfo> intervencionesParaEscena(TheatreProjectLayer.Scene scene) {
        return IntervencionNumberingScene.intervencionesParaEscena(
                intervencionesActuales(),
                viewModel.theatreScenes(),
                boundaryStore,
                scene);
    }

    private String idParaEscena(String globalAlias) {
        return IntervencionNumberingScene.idParaEscena(
                globalAlias,
                intervencionesActuales(),
                viewModel.theatreScenes(),
                boundaryStore);
    }

    private static Label stateNote(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-media-empty-note");
        return label;
    }

    private enum BoundaryCapture {
        NONE,
        START,
        END
    }
}
