package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionBar;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Workspace for audio generation jobs, progress, persisted recovery and mock audio output. */
public final class AudioWorkspaceView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final Label title = new Label("Generación de audio");
    private final Label engine = new Label();
    private final Label status = new Label();
    private final Label eta = new Label();
    private final Label output = new Label();
    private final Label playbackSync = new Label();
    private final Label activeJobTitle = new Label();
    private final Label activeJobSupport = new Label();
    private final Label activeJobMessage = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final ListView<String> queueList = new ListView<>();
    private final ListView<String> selectedJobDetails = new ListView<>();
    private final ListView<String> processDiagnostics = new ListView<>();
    private final ListView<String> playbackCues = new ListView<>();
    private final ListView<String> segmentList = new ListView<>();

    public AudioWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("audio-workspace");
        setPadding(new Insets(16));
        setTop(header());
        setCenter(center());
        setBottom(actions());
        viewModel.activeAudioJobStatusProperty().addListener((obs, oldValue, newValue) -> {
            renderStatus(newValue);
            if (newValue != null && newValue.state().terminal()) {
                refreshQueueState();
            }
        });
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refreshSegments());
        viewModel.currentStoryboardProperty().addListener((obs, oldValue, newValue) -> {
            refreshSegments();
            refreshQueueState();
        });
        viewModel.currentPlaybackManifestProperty().addListener((obs, oldValue, newValue) -> {
            refreshSegments();
            refreshQueueState();
        });
        viewModel.playbackCursorProperty().addListener((obs, oldValue, newValue) -> {
            refreshSegments();
            refreshQueueState();
        });
        renderStatus(viewModel.activeAudioJobStatusProperty().get());
        refreshSegments();
        refreshQueueState();
    }

    private VBox header() {
        title.getStyleClass().add("workspace-title");
        Label subtitle = new Label("Cola operativa persistente: genera audio por segmentos, revisa progreso/ETA, diagnósticos, manifest y jobs reanudables.");
        subtitle.getStyleClass().add("workspace-subtitle");
        VBox box = new VBox(4, title, subtitle);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private Node center() {
        engine.getStyleClass().add("audio-engine-status");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("generation-progress-bar");
        status.getStyleClass().add("audio-job-status");
        eta.getStyleClass().add("audio-job-eta");
        output.getStyleClass().add("audio-job-output");
        activeJobTitle.getStyleClass().add("audio-active-job-title");
        activeJobSupport.getStyleClass().add("audio-active-job-support");
        activeJobMessage.getStyleClass().add("audio-active-job-message");
        playbackSync.getStyleClass().add("audio-playback-sync-summary");
        playbackSync.setWrapText(true);

        queueList.getStyleClass().add("audio-queue-list");
        selectedJobDetails.getStyleClass().add("audio-job-detail-list");
        processDiagnostics.getStyleClass().add("audio-process-diagnostics-list");
        playbackCues.getStyleClass().add("audio-playback-cue-list");
        segmentList.getStyleClass().add("audio-segment-list");
        queueList.setPrefHeight(150);
        selectedJobDetails.setPrefHeight(160);
        processDiagnostics.setPrefHeight(130);
        playbackCues.setPrefHeight(120);
        segmentList.setPrefHeight(140);

        VBox activeCard = card("Job activo", activeJobTitle, activeJobSupport, status, progress, eta, output, activeJobMessage);
        VBox queueCard = card("Cola persistente", queueList);
        VBox detailCard = card("Detalle del job seleccionado", selectedJobDetails);
        VBox diagnosticsCard = card("Diagnóstico TTS", processDiagnostics);
        VBox playbackCard = card("Playback sincronizado transversal", playbackSync, playbackCues);
        VBox segmentsCard = card("Fragmentos de lectura", segmentList);

        HBox queueRow = new HBox(12, queueCard, detailCard);
        queueRow.getStyleClass().add("audio-queue-row");
        HBox.setHgrow(queueCard, Priority.ALWAYS);
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        HBox bottomRow = new HBox(12, diagnosticsCard, playbackCard);
        bottomRow.getStyleClass().add("audio-queue-row");
        HBox.setHgrow(diagnosticsCard, Priority.ALWAYS);
        HBox.setHgrow(playbackCard, Priority.ALWAYS);

        VBox content = new VBox(12, engine, activeCard, queueRow, bottomRow, segmentsCard);
        content.getStyleClass().add("audio-queue-dashboard");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("workspace-scroll");
        return scroll;
    }

    private VBox card(String label, Node... children) {
        Label title = new Label(label);
        title.getStyleClass().add("panel-title");
        VBox box = new VBox(8);
        box.getStyleClass().add("audio-job-card");
        box.setPadding(new Insets(14));
        box.getChildren().add(title);
        box.getChildren().addAll(children);
        return box;
    }

    private HBox actions() {
        var generate = ActionButtonFactory.primary("Generar audio", () -> {
            viewModel.submitAudioGeneration();
            renderStatus(viewModel.activeAudioJobStatusProperty().get());
        });
        var cancel = ActionButtonFactory.danger("Cancelar job activo", viewModel::cancelActiveAudioJob);
        cancel.disableProperty().bind(viewModel.audioJobRunningProperty().not());
        var resume = ActionButtonFactory.primary("Continuar último reanudable", () -> {
            viewModel.resumeMostRecentRecoverableAudioJob();
            refreshQueueState();
        });
        resume.disableProperty().bind(viewModel.audioJobRunningProperty());
        var refresh = ActionButtonFactory.secondary("Actualizar cola", this::refreshQueueState);
        ActionBar box = new ActionBar(generate, cancel, resume, refresh);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void renderStatus(AudioJobStatusDto dto) {
        AudioJobRow active = AudioJobRow.active(dto == null ? AudioJobStatusDto.idle() : dto);
        activeJobTitle.setText(active.headline());
        activeJobSupport.setText(active.supportLine());
        status.setText((dto == null ? AudioJobStatusDto.idle() : dto).statusLine());
        progress.setProgress(active.progress());
        eta.setText("ETA: " + active.etaLabel() + " · Avance: " + active.progressLabel());
        output.setText(active.outputDirectory().isBlank()
                ? "Carpeta del job: todavía no creada."
                : "Carpeta del job: " + active.outputDirectory());
        activeJobMessage.setText(active.message().isBlank() ? active.actionHint() : active.message());
    }

    private void refreshQueueState() {
        AudioQueueState state = viewModel.audioQueueState();
        engine.setText("Motor: " + state.engineLabel()
                + " · Cola: " + state.persistedJobs().size() + " jobs"
                + (state.hasRecoverableJob() ? " · hay reanudables" : ""));
        queueList.getItems().setAll(state.queueLabels());
        selectedJobDetails.getItems().setAll(state.selectedJobDetails());
        processDiagnostics.getItems().setAll(state.processDiagnostics());
        playbackSync.setText(viewModel.playbackSyncState().summaryLabel() + " · " + viewModel.playbackSyncState().readinessLabel());
        playbackCues.getItems().setAll(state.playbackCues());
    }

    private void refreshSegments() {
        segmentList.getItems().clear();
        segmentList.getItems().setAll(viewModel.playbackSynchronizedSegmentLabels());
    }
}
