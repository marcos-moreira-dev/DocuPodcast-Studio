package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.TransportControls;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/** Workspace for the editable narration script generated from the imported document. */
public final class ScriptWorkspaceView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final ReadOnlyObjectProperty<NarrationScriptDocument> scriptProperty;
    private final VBox content = new VBox(12);
    private final ListView<String> segmentList = StudioCollectionControls.listView();
    private final VBox validationIssueList = new VBox(6);
    private boolean rendering;
    private final Label selectedSegmentLabel = new Label("Selección: ninguna");
    private final Label selectedSegmentStatusLabel = new Label("Estado: sin segmento seleccionado");
    private final Label playbackLabel = new Label("Playback: detenido");

    public ScriptWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        this.scriptProperty = viewModel.currentScriptProperty();
        getStyleClass().add("script-workspace");
        setPadding(new Insets(10));

        ScrollPane center = StudioViewportControls.scrollPane(content);
        center.setFitToWidth(true);
        center.getStyleClass().add("script-scroll");

        SplitPane splitPane = StudioViewportControls.splitPane(buildSidePanel(), center);
        splitPane.setDividerPositions(0.31);
        setCenter(splitPane);

        scriptProperty.addListener((obs, oldValue, newValue) -> render(newValue));
        viewModel.selectedScriptSegmentIdProperty().addListener((obs, oldValue, newValue) -> {
            refreshSelectionLabels();
            render(scriptProperty.get());
        });
        viewModel.playbackCursorProperty().addListener((obs, oldValue, newValue) -> {
            refreshSelectionLabels();
            render(scriptProperty.get());
        });
        viewModel.currentPlaybackManifestProperty().addListener((obs, oldValue, newValue) -> {
            refreshSelectionLabels();
            render(scriptProperty.get());
        });
        viewModel.currentStoryboardProperty().addListener((obs, oldValue, newValue) -> render(scriptProperty.get()));
        viewModel.activeVoiceLibraryProperty().addListener((obs, oldValue, newValue) -> {
            refreshSelectionLabels();
            render(scriptProperty.get());
        });
        segmentList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (!rendering) {
                selectSegment(extractSegmentId(newValue));
            }
        });
        render(scriptProperty.get());
        refreshSelectionLabels();
    }

    private VBox buildSidePanel() {
        VBox side = new VBox(10);
        side.getStyleClass().add("script-side-panel");
        side.setPadding(new Insets(8));

        Label title = new Label("Narración avanzada");
        title.getStyleClass().add("side-dock-module-title");

        var create = ActionButtonFactory.primary("Crear desde documento", viewModel::buildNarrationScriptFromDocument);

        Label segmentsTitle = new Label("Segmentos");
        segmentsTitle.getStyleClass().add("document-side-section-title");
        segmentList.getStyleClass().add("script-segment-list");
        segmentList.setPrefHeight(210);

        Label selectionTitle = new Label("Segmento activo");
        selectionTitle.getStyleClass().add("document-side-section-title");
        selectedSegmentLabel.setWrapText(true);
        selectedSegmentLabel.getStyleClass().add("document-side-text");
        selectedSegmentStatusLabel.setWrapText(true);
        selectedSegmentStatusLabel.getStyleClass().add("document-side-text");
        var aiVoice = ActionButtonFactory.secondary("Usar voz IA", viewModel::prepareAiVoiceForSelectedText);
        var humanVoice = ActionButtonFactory.secondary("Preparar voz humana", viewModel::prepareHumanVoiceForSelectedText);

        Label validationTitle = new Label("Validación del narración interna");
        validationTitle.getStyleClass().add("document-side-section-title");
        validationIssueList.getStyleClass().add("script-validation-list");

        Label playbackTitle = new Label("Playback sincronizado");
        playbackTitle.getStyleClass().add("document-side-section-title");
        playbackLabel.setWrapText(true);
        playbackLabel.getStyleClass().add("document-side-text");
        Label syncLabel = new Label();
        syncLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> viewModel.playbackSyncState().activeLabel() + " · " + viewModel.playbackSyncState().readinessLabel(),
                viewModel.currentPlaybackManifestProperty(),
                viewModel.playbackCursorProperty(),
                viewModel.currentStoryboardProperty()));
        syncLabel.setWrapText(true);
        syncLabel.getStyleClass().add("document-side-text");
        var play = ActionButtonFactory.primary("Reproducir desde selección", viewModel::playFromSelectedSegment);
        var playbackButtons = new TransportControls(viewModel::pausePlayback, viewModel::resumePlayback, viewModel::stopPlayback);

        Label manifestTitle = new Label("Manifest de playback");
        manifestTitle.getStyleClass().add("document-side-section-title");
        Label manifestLabel = new Label();
        manifestLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                viewModel::playbackManifestSummary,
                viewModel.currentPlaybackManifestProperty(),
                viewModel.playbackCursorProperty()));
        manifestLabel.setWrapText(true);
        manifestLabel.getStyleClass().add("document-side-text");

        Label helpTitle = new Label("Uso operativo");
        helpTitle.getStyleClass().add("document-side-section-title");
        Label help = new Label("Cada tarjeta del narración interna muestra si el segmento ya tiene voz usable, audio generado, imagen visual y validación limpia. Selecciona un segmento para preparar voz, generar playback o asociar visuales en los módulos correspondientes.");
        help.setWrapText(true);
        help.getStyleClass().add("document-side-text");

        side.getChildren().addAll(title, create, segmentsTitle, segmentList,
                selectionTitle, selectedSegmentLabel, selectedSegmentStatusLabel, aiVoice, humanVoice,
                validationTitle, validationIssueList,
                playbackTitle, playbackLabel, syncLabel, play, playbackButtons, manifestTitle, manifestLabel,
                helpTitle, help);
        return side;
    }

    private void render(NarrationScriptDocument script) {
        rendering = true;
        try {
            content.getChildren().clear();
            segmentList.getItems().clear();
            validationIssueList.getChildren().clear();
            if (script == null) {
                validationIssueList.getChildren().add(sideText("Sin narración interna activo."));
                content.getChildren().add(emptyState());
                return;
            }
            List<ScriptValidationIssue> issues = validationIssues(script);
            content.getChildren().add(editorHeader(script, issues));
            populateValidationIssues(issues);
            for (NarrationSegment segment : script.segments()) {
                segmentList.getItems().add(segment.id() + " — " + segment.title());
                content.getChildren().add(segmentCard(segment, issues));
            }
        } finally {
            rendering = false;
        }
    }

    private VBox emptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("script-empty-state");
        Label title = new Label("Narración avanzada");
        title.getStyleClass().add("script-title");
        Label text = new Label("Todavía no hay narración interna. Sigue el flujo recomendado para convertir tu documento en una narración editable.");
        text.setWrapText(true);
        text.getStyleClass().add("script-summary");
        VBox steps = new VBox(6,
                stepLabel("1. Abre o importa un Word/DOCX."),
                stepLabel("2. Revisa estructura, bloques narrables y perfil de lectura en Documento."),
                stepLabel("3. Pulsa Preparar lectura desde documento."),
                stepLabel("4. Luego asigna voces, genera audio y prepara visuales."));
        steps.getStyleClass().add("script-empty-steps");
        var create = ActionButtonFactory.primary("Preparar lectura desde documento", viewModel::buildNarrationScriptFromDocument);
        box.getChildren().addAll(title, text, steps, create);
        return box;
    }

    private VBox editorHeader(NarrationScriptDocument script, List<ScriptValidationIssue> issues) {
        VBox header = new VBox(10);
        header.getStyleClass().add("script-editor-header");
        Label title = new Label(script.title());
        title.getStyleClass().add("script-title");
        Label summary = new Label("Segmentos: %d · Narrables: %d · Palabras: %d · Caracteres estimados: %d · Fuente: %s".formatted(
                script.segmentCount(),
                script.narratableSegmentCount(),
                script.wordCount(),
                script.estimatedCharacters(),
                script.sourceDocumentTitle()
        ));
        summary.getStyleClass().add("script-summary");

        List<ScriptSegmentPresentation> cards = script.segments().stream()
                .map(segment -> presentationFor(segment, issues))
                .toList();
        long voiceReady = cards.stream().filter(ScriptSegmentPresentation::voiceReady).count();
        long audioReady = cards.stream().filter(ScriptSegmentPresentation::audioReady).count();
        long storyboardReady = cards.stream().filter(ScriptSegmentPresentation::storyboardReady).count();
        long validationOk = cards.stream().filter(card -> card.validationLevel() == ScriptValidationIssueLevel.INFO).count();
        HBox readiness = new HBox(8,
                readinessCard("Voz", voiceReady + "/" + script.segmentCount(), "segmentos con voz usable", voiceReady == script.segmentCount() ? "script-readiness-ok" : "script-readiness-warning"),
                readinessCard("Audio", audioReady + "/" + script.segmentCount(), "segmentos con cue de audio", audioReady == script.segmentCount() ? "script-readiness-ok" : "script-readiness-pending"),
                readinessCard("Visuales", storyboardReady + "/" + script.segmentCount(), "segmentos con imagen", storyboardReady == script.segmentCount() ? "script-readiness-ok" : "script-readiness-pending"),
                readinessCard("Validación", validationOk + "/" + script.segmentCount(), "segmentos sin hallazgos", validationOk == script.segmentCount() ? "script-readiness-ok" : "script-readiness-warning")
        );
        readiness.getStyleClass().add("script-readiness-row");
        header.getChildren().addAll(title, summary, validationSummary(script, issues), readiness);
        return header;
    }

    private VBox readinessCard(String title, String value, String caption, String styleClass) {
        VBox card = new VBox(3);
        card.getStyleClass().addAll("script-readiness-card", styleClass);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("script-readiness-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("script-readiness-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("script-readiness-caption");
        captionLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, valueLabel, captionLabel);
        return card;
    }

    private VBox segmentCard(NarrationSegment segment, List<ScriptValidationIssue> issues) {
        ScriptSegmentPresentation presentation = presentationFor(segment, issues);
        VBox card = new VBox(8);
        card.getStyleClass().addAll("script-segment-card", "script-segment-" + segment.type().name().toLowerCase(Locale.ROOT));
        if (segment.id().equals(viewModel.selectedScriptSegmentIdProperty().get())) {
            card.getStyleClass().add("script-segment-selected");
        }
        if (presentation.playbackActive()) {
            card.getStyleClass().add("script-segment-playing");
        }
        if (presentation.playbackPaused()) {
            card.getStyleClass().add("script-segment-paused");
        }
        card.setOnMouseClicked(event -> selectSegment(segment.id()));

        HBox headerRow = new HBox(8);
        headerRow.getStyleClass().add("script-segment-header-row");
        Label header = new Label(segment.id() + " · " + segment.type().displayName() + " · " + segment.title());
        header.getStyleClass().add("script-segment-header");
        Label counts = new Label(segment.wordCount() + " palabras · " + segment.characterCount() + " caracteres");
        counts.getStyleClass().add("script-segment-counts");
        headerRow.getChildren().addAll(header, counts);

        FlowPane statusRow = new FlowPane(6, 6,
                statusChip("Voz", presentation.voiceStatus(), presentation.voiceCssClass()),
                statusChip("Audio", presentation.audioStatus(), presentation.audioCssClass()),
                statusChip("Visuales", presentation.storyboardStatus(), presentation.storyboardCssClass()),
                statusChip("Playback", presentation.playbackStatus(), presentation.playbackCssClass()),
                statusChip("Validación", presentation.validationStatus(), presentation.validationCssClass())
        );
        statusRow.getStyleClass().add("script-segment-status-row");

        Label voiceLine = new Label("Personaje: " + presentation.characterLabel()
                + " · Voz: " + presentation.voiceLabel()
                + " · Estilo: " + presentation.styleLabel()
                + " · Fuente: " + String.join(", ", segment.sourceBlockIds()));
        voiceLine.getStyleClass().add("script-segment-chips");
        voiceLine.setWrapText(true);
        Label text = new Label(segment.narrationText());
        text.setWrapText(true);
        text.getStyleClass().add("script-segment-text");
        card.getChildren().addAll(headerRow, statusRow, voiceLine, text);
        return card;
    }

    private Label statusChip(String label, String value, String styleClass) {
        Label chip = new Label(label + ": " + value);
        chip.getStyleClass().addAll("script-status-chip", styleClass);
        chip.setWrapText(false);
        return chip;
    }

    private Label validationSummary(NarrationScriptDocument script, List<ScriptValidationIssue> issues) {
        long warnings = issues.stream().filter(issue -> issue.level() == ScriptValidationIssueLevel.WARNING).count();
        long errors = issues.stream().filter(issue -> issue.level() == ScriptValidationIssueLevel.ERROR).count();
        Label label = new Label("Validación: " + warnings + " advertencias · " + errors + " errores. El narración interna es la proyección avanzada para voces, audio, visuales y playback.");
        label.setWrapText(true);
        label.getStyleClass().add(errors > 0 ? "document-issue-error" : warnings > 0 ? "document-issue-warning" : "document-issue-info");
        return label;
    }

    private void populateValidationIssues(List<ScriptValidationIssue> issues) {
        validationIssueList.getChildren().clear();
        if (issues.isEmpty()) {
            Label ok = sideText("Sin hallazgos. El narración interna está listo para asignar voces y generar audio.");
            ok.getStyleClass().add("script-side-ok");
            validationIssueList.getChildren().add(ok);
            return;
        }
        for (ScriptValidationIssue issue : issues.stream().limit(6).toList()) {
            Label label = sideText((issue.segmentId().isBlank() ? "Narración" : issue.segmentId()) + " · " + issue.message());
            label.getStyleClass().add(issue.level() == ScriptValidationIssueLevel.ERROR ? "script-side-error" : "script-side-warning");
            validationIssueList.getChildren().add(label);
        }
        if (issues.size() > 6) {
            validationIssueList.getChildren().add(sideText("+ " + (issues.size() - 6) + " hallazgos adicionales."));
        }
    }

    private ScriptSegmentPresentation presentationFor(NarrationSegment segment, List<ScriptValidationIssue> issues) {
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        StoryboardDocument storyboard = viewModel.currentStoryboardProperty().get();
        return ScriptSegmentPresentation.from(
                segment,
                library,
                viewModel.currentPlaybackManifestProperty().get(),
                storyboard,
                issues,
                viewModel.playbackCursorProperty().get()
        );
    }

    private List<ScriptValidationIssue> validationIssues(NarrationScriptDocument script) {
        return viewModel.projectWorkspace().script().validateNarrationScript().validate(script);
    }

    private Label stepLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("script-empty-step");
        return label;
    }

    private Label sideText(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }

    private void selectSegment(String segmentId) {
        viewModel.selectScriptSegment(segmentId);
    }

    private void refreshSelectionLabels() {
        String selected = viewModel.selectedScriptSegmentIdProperty().get();
        if (selected == null || selected.isBlank()) {
            selectedSegmentLabel.setText("Selección: ninguna. Haz clic sobre una tarjeta del narración interna.");
            selectedSegmentStatusLabel.setText("Estado: sin segmento seleccionado.");
        } else {
            selectedSegmentLabel.setText("Selección: " + selected + ". Puede recibir voz IA/TTS, voz humana, estilo, imagen y playback desde esta línea/segmento.");
            selectedSegmentStatusLabel.setText(selectedSegmentStatus(selected));
        }
        PlaybackCursor cursor = viewModel.playbackCursorProperty().get();
        if (cursor == null || cursor.segmentId().isBlank()) {
            playbackLabel.setText("Playback: detenido. Haz clic en un segmento para preparar el cursor.");
        } else {
            playbackLabel.setText(viewModel.playbackSyncState().summaryLabel());
        }
    }

    private String selectedSegmentStatus(String segmentId) {
        NarrationScriptDocument script = scriptProperty.get();
        if (script == null) {
            return "Estado: no hay narración interna activo.";
        }
        return script.segmentById(segmentId)
                .map(segment -> {
                    ScriptSegmentPresentation presentation = presentationFor(segment, validationIssues(script));
                    return "Estado: " + presentation.voiceStatus()
                            + " · " + presentation.audioStatus()
                            + " · " + presentation.storyboardStatus()
                            + " · " + presentation.validationStatus();
                })
                .orElse("Estado: segmento no encontrado.");
    }

    private static String extractSegmentId(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        int separator = label.indexOf(' ');
        return separator < 0 ? label.strip() : label.substring(0, separator).strip();
    }
}
