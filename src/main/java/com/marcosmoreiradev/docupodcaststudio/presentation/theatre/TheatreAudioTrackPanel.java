package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimelineEntry;
import com.marcosmoreiradev.docupodcaststudio.application.media.PreparedAudioAsset;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.TheatreAudioTrackWorkflow;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Scrollable editor for one theatre background track. Its state stays live while the Audio mode is hidden. */
public final class TheatreAudioTrackPanel extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final SegmentAudioPlayer previewPlayer;
    private final Label selection = new Label("Selecciona un fragmento narrable.");
    private final Label coverage = new Label("Sin pista asignada.");
    private final Label fileName = new Label("Sin archivo de audio.");
    private final TextField sourceStart = secondsField("0.0");
    private final TextField sourceEnd = secondsField("0.0");
    private final ComboBox<TheatreProjectLayer.AudioTrackEndMode> endMode = new ComboBox<>();
    private final Slider volume = new Slider(0.0, 1.0, 0.30);
    private final Label volumeLabel = new Label("30%");
    private final Slider playhead = new Slider(0.0, 1.0, 0.0);
    private final Label playheadLabel = new Label("00:00.0 / 00:00.0");
    private final CheckBox gentleFade = new CheckBox("Fade suave al inicio y al final (0,5 s)");
    private final ObservableList<TheatreAudioTrackTimelineEntry> trackEntries = FXCollections.observableArrayList();
    private final ObjectProperty<TheatreAudioTrackTimelineEntry> selectedTrack = new SimpleObjectProperty<>();
    private final Button chooseAudio;
    private final Button remove;
    private final Timeline positionTimer;
    private javafx.concurrent.Task<PreparedAudioAsset> preparationTask;
    private PreparedAudioAsset pendingAudio;
    private final Label preparationState = new Label("Elige un archivo para preparar la pista.");
    private final ProgressIndicator preparationProgress = new ProgressIndicator();
    private final java.util.List<javafx.scene.control.Control> readyControls = new java.util.ArrayList<>();
    private boolean previewPaused;
    private boolean restartingPreview;
    private String currentSelectionSegmentId = "";
    private double sourceDurationSeconds;

    public TheatreAudioTrackPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        this.previewPlayer = viewModel.playbackWorkspace().playback().previewAudioPlayer();
        getStyleClass().add("theatre-audio-track-panel");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        selection.getStyleClass().add("theatre-audio-track-selection");
        selection.setWrapText(true);
        coverage.setWrapText(true);
        fileName.setWrapText(true);
        configureEndMode();
        configureSliders();

        chooseAudio = fullWidth(ActionButtonFactory.secondary("Elegir audio", this::chooseAudio));
        Button play = ActionButtonFactory.transportIcon(AppIcon.RESUME, "Reproducir preescucha", this::playPreview);
        Button pause = ActionButtonFactory.transportIcon(AppIcon.PAUSE, "Pausar o reanudar preescucha", this::pausePreview);
        Button stop = ActionButtonFactory.transportIcon(AppIcon.STOP, "Detener preescucha", this::stopPreview);
        Button markStart = ActionButtonFactory.secondary("Establecer inicio", this::setStartFromPlayhead);
        Button markEnd = ActionButtonFactory.secondary("Establecer fin", this::setEndFromPlayhead);
        HBox transport = new HBox(8, play, pause, stop);
        HBox markers = new HBox(8, markStart, markEnd);
        HBox.setHgrow(markStart, Priority.ALWAYS);
        HBox.setHgrow(markEnd, Priority.ALWAYS);
        markStart.setMaxWidth(Double.MAX_VALUE);
        markEnd.setMaxWidth(Double.MAX_VALUE);

        Button save = fullWidth(ActionButtonFactory.primary("Guardar pista", this::saveTrack));
        Button goTo = fullWidth(ActionButtonFactory.secondary("Ir al fragmento inicial", this::goToTrackStart));
        remove = fullWidth(ActionButtonFactory.danger("Quitar pista", this::removeTrack));
        remove.setDisable(true);

        preparationProgress.setMaxSize(20, 20);
        preparationProgress.setVisible(false);
        preparationProgress.setManaged(false);
        HBox preparation = new HBox(8, preparationProgress, preparationState);
        readyControls.addAll(java.util.List.of(playhead, play, pause, stop, markStart, markEnd, sourceStart,
                sourceEnd, endMode, volume, gentleFade, save));
        VBox body = new VBox(10, section("Pista de audio"), selection, coverage,
                labeled("Archivo", fileName), chooseAudio, preparation, labeled("Posicion", playhead), playheadLabel,
                transport, markers, labeled("Desde segundo", sourceStart), labeled("Final", endMode),
                labeled("Hasta segundo", sourceEnd), labeled("Volumen", new HBox(8, volume, volumeLabel)),
                gentleFade, save, goTo, remove);
        body.setPadding(new Insets(14));
        body.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPannable(false);
        scroll.getStyleClass().add("theatre-audio-track-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        viewModel.selectedScriptSegmentIdProperty().addListener((obs, oldValue, value) -> refresh());
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, value) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, value) -> refresh());
        viewModel.currentPlaybackManifestProperty().addListener((obs, oldValue, value) -> refresh());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, value) -> refresh());
        positionTimer = new Timeline(new KeyFrame(Duration.millis(100), event -> updatePlayheadFromPlayer()));
        positionTimer.setCycleCount(Timeline.INDEFINITE);
        positionTimer.play();
        sceneProperty().addListener((obs, oldScene, newScene) -> { if (newScene == null) { stopPreview(); discardPendingAudio(); } });
        setReady(false, "Elige un archivo para preparar la pista.");
        refresh();
    }

    ObservableList<TheatreAudioTrackTimelineEntry> trackEntries() { return trackEntries; }
    ObjectProperty<TheatreAudioTrackTimelineEntry> selectedTrackProperty() { return selectedTrack; }

    void selectTrack(TheatreAudioTrackTimelineEntry entry, boolean navigate) {
        if (entry == null) return;
        if (pendingAudio != null) discardPendingAudio();
        selectedTrack.set(entry);
        pendingAudio = null;
        fileName.setText(entry.assetDisplayName().isBlank() ? entry.track().assetId() : entry.assetDisplayName());
        sourceStart.setText(format(entry.track().sourceStartSeconds()));
        sourceEnd.setText(format(entry.track().effectiveEndSeconds()));
        endMode.setValue(entry.track().endMode());
        volume.setValue(entry.track().volume());
        gentleFade.setSelected(entry.track().gentleFade());
        sourceDurationSeconds = entry.track().sourceDurationSeconds();
        configurePlayhead(entry.track().sourceStartSeconds(), sourceDurationSeconds);
        setReady(true, "Audio listo para configurar.");
        chooseAudio.setText("Cambiar audio");
        remove.setDisable(false);
        if (navigate) viewModel.selectTheatreAudioSegment(entry.startSegmentId());
    }

    Optional<TheatreAudioTrackTimelineEntry> trackEntry(String trackId) {
        String resolved = normalized(trackId);
        return trackEntries.stream()
                .filter(entry -> entry != null && resolved.equals(entry.track().id()))
                .findFirst();
    }

    private void refresh() {
        var timeline = viewModel.theatreAudioTrackTimeline();
        synchronizeTrackEntries(timeline.entries());
        String segmentId = normalized(viewModel.selectedScriptSegmentIdProperty().get());
        selection.setText(segmentId.isBlank()
                ? "Selecciona un fragmento narrable en el documento o mapa teatral."
                : "Fragmento seleccionado: " + segmentId);
        if (segmentId.isBlank()) {
            coverage.setText("Sin fragmento seleccionado.");
            return;
        }
        Optional<TheatreAudioTrackTimelineEntry> direct = timeline.startingAtSegment(segmentId);
        Optional<TheatreAudioTrackTimelineEntry> affected = direct.isPresent()
                ? direct : timeline.affectingSegment(segmentId);
        if (affected.isPresent()) {
            TheatreAudioTrackTimelineEntry entry = affected.get();
            selectTrack(entry, false);
            coverage.setText(direct.isPresent()
                    ? "Pista asignada directamente. Cubre " + entry.affectedSegmentIds().size() + " fragmento(s)."
                    : "Afectado por la pista iniciada en " + entry.startSegmentId() + ".");
        } else {
            if (!segmentId.equals(currentSelectionSegmentId)) resetForNewTrack();
            coverage.setText("Este fragmento no esta afectado por una pista.");
        }
        currentSelectionSegmentId = segmentId;
    }

    private void synchronizeTrackEntries(List<TheatreAudioTrackTimelineEntry> incoming) {
        List<TheatreAudioTrackTimelineEntry> safe = incoming == null ? List.of() : List.copyOf(incoming);
        boolean sameIds = trackEntries.size() == safe.size();
        for (int index = 0; sameIds && index < safe.size(); index++) {
            sameIds = Objects.equals(trackEntries.get(index).track().id(), safe.get(index).track().id());
        }
        if (!sameIds) {
            trackEntries.setAll(safe);
        } else {
            for (int index = 0; index < safe.size(); index++) {
                if (!Objects.equals(trackEntries.get(index), safe.get(index))) {
                    trackEntries.set(index, safe.get(index));
                }
            }
        }
        TheatreAudioTrackTimelineEntry selected = selectedTrack.get();
        if (selected != null) {
            trackEntry(selected.track().id()).ifPresentOrElse(
                    current -> {
                        if (!Objects.equals(selected, current)) selectedTrack.set(current);
                    },
                    () -> selectedTrack.set(null));
        }
    }

    private void resetForNewTrack() {
        stopPreview();
        discardPendingAudio();
        selectedTrack.set(null);
        pendingAudio = null;
        fileName.setText("Sin archivo de audio.");
        sourceStart.setText("0.0");
        sourceEnd.setText("0.0");
        endMode.setValue(TheatreProjectLayer.AudioTrackEndMode.FILE_END);
        volume.setValue(0.30);
        gentleFade.setSelected(false);
        sourceDurationSeconds = 0.0;
        configurePlayhead(0.0, 0.0);
        chooseAudio.setText("Elegir audio");
        remove.setDisable(true);
        setReady(false, "Elige un archivo para preparar la pista.");
    }

    private void chooseAudio() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir pista de audio teatral");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Audio compatible", "*.wav", "*.mp3", "*.m4a", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("WAV", "*.wav"));
        Path chosen = Optional.ofNullable(chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow()))
                .map(java.io.File::toPath).orElse(null);
        if (chosen == null) return;
        prepareAudio(chosen);
    }

    private void prepareAudio(Path chosen) {
        stopPreview();
        setLoading("Preparando audio...");
        if (preparationTask != null) preparationTask.cancel();
        javafx.concurrent.Task<PreparedAudioAsset> task = new javafx.concurrent.Task<>() {
            @Override protected PreparedAudioAsset call() throws Exception {
                discardPendingAudioNow();
                Path projectFile = viewModel.currentProjectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de elegir audio."));
                PreparedAudioAsset prepared = viewModel.generationWorkspace().media().importUserMediaAsset().prepareAudio(projectFile, chosen);
                if (isCancelled()) {
                    viewModel.generationWorkspace().media().importUserMediaAsset().discardPreparedAudio(projectFile, prepared);
                    return null;
                }
                return prepared;
            }
        };
        preparationTask = task;
        task.setOnSucceeded(event -> {
            preparationTask = null;
            pendingAudio = task.getValue();
            if (pendingAudio == null) return;
            sourceDurationSeconds = pendingAudio.durationSeconds();
            fileName.setText(pendingAudio.sourceDisplayName());
            sourceStart.setText("0.0");
            sourceEnd.setText(format(sourceDurationSeconds));
            configurePlayhead(0.0, sourceDurationSeconds);
            chooseAudio.setText("Cambiar audio");
            setReady(true, "Audio listo para configurar.");
        });
        task.setOnFailed(event -> {
            preparationTask = null;
            pendingAudio = null;
            sourceDurationSeconds = 0.0;
            configurePlayhead(0.0, 0.0);
            setReady(false, "Error al preparar audio. Puedes reintentar o elegir otro archivo.");
            error(task.getException() == null ? "No se pudo preparar el audio." : task.getException().getMessage());
        });
        task.setOnCancelled(event -> preparationTask = null);
        Thread thread = new Thread(task, "theatre-audio-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void saveTrack() { saveTrack(false); }

    private void saveTrack(boolean replaceConflicts) {
        String selectedSegment = normalized(viewModel.selectedScriptSegmentIdProperty().get());
        String segmentId = selectedTrack.get() == null ? selectedSegment : selectedTrack.get().startSegmentId();
        String interventionId = selectedTrack.get() == null
                ? viewModel.selectedTheatreAudioInterventionId().orElse("")
                : selectedTrack.get().track().startIntervencionId();
        if (segmentId.isBlank()) { error("Selecciona primero un fragmento narrable del documento o mapa teatral."); return; }
        try {
            double start = number(sourceStart);
            double end = number(sourceEnd);
            validateRange(start, end, endMode.getValue(), sourceDurationSeconds);
            TheatreAudioTrackWorkflow.Result result = viewModel.saveTheatreAudioTrack(
                    selectedTrack.get() == null ? "" : selectedTrack.get().track().id(), segmentId, interventionId,
                    pendingAudio, start, endMode.getValue(), end, volume.getValue(), gentleFade.isSelected(), replaceConflicts);
            if (result.conflict()) confirmReplacement(result);
            else if (result.saved()) pendingAudio = null;
            refresh();
        } catch (IOException | RuntimeException ex) {
            error(ex.getMessage());
        }
    }

    private void confirmReplacement(TheatreAudioTrackWorkflow.Result result) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                result.message() + "\n\nLa pista existente sera eliminada de la linea temporal.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Pista superpuesta");
        confirm.setHeaderText("Solo puede sonar una pista teatral a la vez");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            saveTrack(true);
        }
    }

    private void removeTrack() {
        if (selectedTrack.get() == null) return;
        try { viewModel.removeTheatreAudioTrack(selectedTrack.get().track().id()); }
        catch (RuntimeException ex) { error(ex.getMessage()); return; }
        resetForNewTrack();
        refresh();
    }

    private void goToTrackStart() {
        if (selectedTrack.get() != null) viewModel.selectTheatreAudioSegment(selectedTrack.get().startSegmentId());
    }

    private void playPreview() {
        TheatreAudioTrackTimelineEntry entry = selectedTrack.get();
        Path audio = pendingAudio != null ? pendingAudio.pendingPath()
                : entry == null ? null : viewModel.currentProjectDirectory().map(root -> root.resolve(entry.assetRelativePath()).normalize()).orElse(null);
        if (audio == null || !Files.isRegularFile(audio)) { error("No se encontro el archivo de audio de la pista."); return; }
        double start = number(sourceStart);
        double end = configuredEnd();
        validateRange(start, number(sourceEnd), endMode.getValue(), sourceDurationSeconds);
        double position = playhead.getValue();
        if (position < start || position >= end) position = start;
        startPreviewAt(audio, position, start, end, false);
    }

    private void startPreviewAt(Path audio, double position, double start, double end, boolean remainPaused) {
        try {
            restartingPreview = true;
            previewPlayer.setVolume(volume.getValue());
            previewPlayer.setStopAtSeconds(end);
            previewPlayer.setFadeEnvelope(start, end, fadeDuration(end - start));
            previewPlayer.play(audio, position);
            previewPaused = false;
            if (remainPaused) { previewPlayer.pause(); previewPaused = true; }
        } catch (IOException ex) {
            error(ex.getMessage());
        } finally {
            restartingPreview = false;
        }
    }

    private void pausePreview() {
        if (!previewPlayer.playing()) return;
        if (previewPaused) previewPlayer.resume(); else previewPlayer.pause();
        previewPaused = !previewPaused;
    }

    private void stopPreview() {
        previewPlayer.stop();
        previewPaused = false;
        if (!restartingPreview) playhead.setValue(safeNumber(sourceStart, 0.0));
    }

    private void seekPreview() {
        TheatreAudioTrackTimelineEntry entry = selectedTrack.get();
        if (!previewPlayer.playing()) return;
        Path audio = pendingAudio != null ? pendingAudio.pendingPath()
                : entry == null ? null : viewModel.currentProjectDirectory().map(root -> root.resolve(entry.assetRelativePath()).normalize()).orElse(null);
        if (audio == null || !Files.isRegularFile(audio)) return;
        startPreviewAt(audio, playhead.getValue(), number(sourceStart), configuredEnd(), previewPaused);
    }

    private void setStartFromPlayhead() {
        double value = playhead.getValue();
        double end = configuredEnd();
        if (end > 0.0 && value >= end) { error("El inicio debe ser menor que el fin de la pista."); return; }
        sourceStart.setText(format(value));
    }

    private void setEndFromPlayhead() {
        double value = playhead.getValue();
        double start = safeNumber(sourceStart, 0.0);
        if (value <= start) { error("El fin debe ser mayor que el inicio de la pista."); return; }
        sourceEnd.setText(format(value));
        endMode.setValue(TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME);
    }

    private void configureEndMode() {
        endMode.getItems().setAll(TheatreProjectLayer.AudioTrackEndMode.FILE_END,
                TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME);
        endMode.setValue(TheatreProjectLayer.AudioTrackEndMode.FILE_END);
        endMode.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(TheatreProjectLayer.AudioTrackEndMode value) {
                return value == TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME ? "Hasta segundo" : "Hasta el final";
            }
            @Override public TheatreProjectLayer.AudioTrackEndMode fromString(String value) { return null; }
        });
        endMode.valueProperty().addListener((obs, oldValue, value) ->
                sourceEnd.setDisable(value != TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME));
        sourceEnd.setDisable(true);
    }

    private void configureSliders() {
        volume.valueProperty().addListener((obs, oldValue, value) -> {
            volumeLabel.setText(Math.round(value.doubleValue() * 100.0) + "%");
            previewPlayer.setVolume(value.doubleValue());
        });
        playhead.valueProperty().addListener((obs, oldValue, value) -> updatePlayheadLabel());
        playhead.valueChangingProperty().addListener((obs, oldValue, changing) -> { if (!changing) seekPreview(); });
        playhead.setOnMouseReleased(event -> seekPreview());
    }

    private void configurePlayhead(double position, double duration) {
        playhead.setMin(0.0);
        playhead.setMax(Math.max(0.001, duration));
        playhead.setValue(Math.max(0.0, Math.min(position, playhead.getMax())));
        updatePlayheadLabel();
    }

    private void setLoading(String message) {
        chooseAudio.setDisable(true);
        preparationProgress.setManaged(true);
        preparationProgress.setVisible(true);
        preparationState.setText(message);
        readyControls.forEach(control -> control.setDisable(true));
    }

    private void setReady(boolean ready, String message) {
        chooseAudio.setDisable(false);
        preparationProgress.setManaged(false);
        preparationProgress.setVisible(false);
        preparationState.setText(message);
        readyControls.forEach(control -> control.setDisable(!ready));
        sourceEnd.setDisable(!ready || endMode.getValue() != TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME);
    }

    private void discardPendingAudio() {
        if (preparationTask != null) preparationTask.cancel();
        PreparedAudioAsset discarded = pendingAudio;
        pendingAudio = null;
        if (discarded == null) return;
        Thread thread = new Thread(() -> {
            try { viewModel.currentProjectFile().ifPresent(file -> { try { viewModel.generationWorkspace().media().importUserMediaAsset().discardPreparedAudio(file, discarded); } catch (IOException ignored) { } }); }
            catch (RuntimeException ignored) { }
        }, "theatre-audio-discard");
        thread.setDaemon(true);
        thread.start();
    }

    private void discardPendingAudioNow() throws IOException {
        PreparedAudioAsset discarded = pendingAudio;
        pendingAudio = null;
        if (discarded != null && viewModel.currentProjectFile().isPresent()) {
            viewModel.generationWorkspace().media().importUserMediaAsset().discardPreparedAudio(viewModel.currentProjectFile().get(), discarded);
        }
    }

    private void updatePlayheadFromPlayer() {
        if (previewPlayer.playing() && !playhead.isValueChanging()) {
            playhead.setValue(Math.min(playhead.getMax(), previewPlayer.currentPositionSeconds()));
        }
    }

    private void updatePlayheadLabel() {
        playheadLabel.setText(clock(playhead.getValue()) + " / " + clock(sourceDurationSeconds));
    }

    private double configuredEnd() {
        return endMode.getValue() == TheatreProjectLayer.AudioTrackEndMode.FILE_END
                ? sourceDurationSeconds : safeNumber(sourceEnd, 0.0);
    }

    private double fadeDuration(double duration) {
        return gentleFade.isSelected() ? Math.min(0.5, Math.max(0.0, duration) * 0.25) : 0.0;
    }

    static void validateRange(double start, double end, TheatreProjectLayer.AudioTrackEndMode mode,
                              double knownDuration) {
        double effectiveEnd = mode == TheatreProjectLayer.AudioTrackEndMode.FILE_END ? knownDuration : end;
        if (start < 0.0 || (effectiveEnd > 0.0 && start >= effectiveEnd)) {
            throw new IllegalArgumentException("El inicio debe ser menor que el fin de la pista.");
        }
        if (mode == TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME && end <= start) {
            throw new IllegalArgumentException("El fin debe ser mayor que el inicio de la pista.");
        }
        if (knownDuration > 0.0 && effectiveEnd > knownDuration) {
            throw new IllegalArgumentException("El fin no puede superar la duracion del archivo.");
        }
    }

    private void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                message == null || message.isBlank() ? "No se pudo completar la accion." : message,
                ButtonType.OK);
        alert.setTitle("Pista de audio");
        alert.setHeaderText("Revisa la configuracion de la pista");
        alert.showAndWait();
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-media-action-label");
        return label;
    }

    private static VBox labeled(String text, javafx.scene.Node control) {
        VBox box = new VBox(4, new Label(text), control);
        if (control instanceof javafx.scene.layout.Region region) region.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Button fullWidth(Button button) { button.setMaxWidth(Double.MAX_VALUE); return button; }
    private static TextField secondsField(String value) { TextField field = new TextField(value); field.setPromptText("0.0"); return field; }
    private static double number(TextField field) { return Double.parseDouble(field.getText().trim().replace(',', '.')); }
    private static double safeNumber(TextField field, double fallback) { try { return Math.max(0.0, number(field)); } catch (RuntimeException ex) { return fallback; } }
    private static String format(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String normalized(String value) { return value == null ? "" : value.strip(); }
    private static String clock(double seconds) {
        double safe = Math.max(0.0, seconds);
        return String.format(Locale.ROOT, "%02d:%04.1f", (int) (safe / 60.0), safe % 60.0);
    }
}
