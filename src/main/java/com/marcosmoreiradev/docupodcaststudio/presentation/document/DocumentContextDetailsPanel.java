package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RailActionRow;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.TheatreChoralVoiceWorkflow;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contextual inspector module for the selected document sentence or block. */
public final class DocumentContextDetailsPanel extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final VBox choralVoiceSection = new VBox(8);
    private final VBox choralVoiceOptions = new VBox(6);
    private final CheckBox allCharacters = new CheckBox("Todos los personajes (sin narrador)");
    private final Label choralVoiceStatus = new Label("Selecciona una intervencion teatral.");
    private final ProgressBar choralVoiceProgress = new ProgressBar(0.0);
    private final Button renderChoralVoice = ActionButtonFactory.primary("Renderizar voz multipersona", this::renderChoralVoice);
    private final Button restoreSimpleVoice = ActionButtonFactory.danger("Restaurar voz simple", this::restoreSimpleVoice);
    private final Map<CheckBox, TheatreChoralVoiceWorkflow.Option> choralChecks = new LinkedHashMap<>();
    private boolean refreshingChoralVoice;

    public DocumentContextDetailsPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("document-context-module");
        setPadding(new Insets(8));
        setSpacing(8);
        setMaxHeight(Double.MAX_VALUE);
        setFillWidth(true);

        SectionHeader header = new SectionHeader(
                "Fragmento",
                "Revisa la selección y empieza a escuchar desde aquí.");

        Label selected = new Label();
        selected.getStyleClass().add("document-context-selected");
        selected.setWrapText(true);
        selected.textProperty().bind(viewModel.selectedDocumentRangeLabelProperty());

        Label location = new Label();
        location.getStyleClass().add("document-context-copy");
        location.setWrapText(true);
        location.textProperty().bind(Bindings.createStringBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            DocumentTextRange textRange = viewModel.selectedDocumentTextRangeProperty().get();
            if (blockId == null || blockId.isBlank()) {
                return "Haz clic en una oración o bloque del documento.";
            }
            if (textRange == null) {
                return "Bloque " + blockId;
            }
            return "Bloque " + blockId + " · " + textRange.displayLabel();
        }, viewModel.selectedDocumentBlockIdProperty(), viewModel.selectedDocumentTextRangeProperty()));

        Label sourceLocation = new Label();
        sourceLocation.getStyleClass().add("document-context-source-location");
        sourceLocation.setWrapText(true);
        sourceLocation.textProperty().bind(viewModel.selectedDocumentSourceLocationProperty());

        Button play = action("Reproducir desde aquí", viewModel::playFromSelectedSegment);
        Button playOnly = action("Reproducir fragmento (solo este)", viewModel::playSelectedFragmentOnly);
        Button regenerateAudio = action(
                "Renderizar audio de fragmento de nuevo según configuración",
                viewModel::regenerateSelectedTheatreInterventionAudio);
        regenerateAudio.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !viewModel.canRegenerateSelectedTheatreInterventionAudio(),
                viewModel.selectedDocumentBlockIdProperty(),
                viewModel.currentProjectModeProperty(),
                viewModel.audioJobRunningProperty(),
                viewModel.choralVoiceRenderingProperty()));
        configureChoralVoiceSection();
        CheckBox readTables = new CheckBox("Leer cuadros de texto y tablas");
        readTables.getStyleClass().add("document-context-checkbox");
        readTables.setSelected(viewModel.readTablesAndTextBoxesForNarration());
        readTables.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean selectedValue = Boolean.TRUE.equals(newValue);
            if (selectedValue != viewModel.readTablesAndTextBoxesForNarration()) {
                viewModel.setReadTablesAndTextBoxesForNarration(selectedValue);
            }
        });
        viewModel.activeReadingProfileProperty().addListener((obs, oldValue, newValue) -> {
            boolean selectedValue = viewModel.readTablesAndTextBoxesForNarration();
            if (readTables.isSelected() != selectedValue) {
                readTables.setSelected(selectedValue);
            }
        });

        Label playbackNotice = new Label("Si el audio aún no está listo, DocuPodcast lo preparará con la voz base o la configuración actual. Puedes cambiar voz, tono, audio o imagen en los módulos laterales.");
        playbackNotice.getStyleClass().add("document-context-note");
        playbackNotice.setWrapText(true);

        Label source = new Label("La fuente original no se edita: todo ajuste se guarda como capa dentro del proyecto.");
        source.getStyleClass().add("document-context-note");
        source.setWrapText(true);

        VBox body = new VBox(9,
                header,
                selected,
                location,
                sourceLocation,
                new RailActionRow(play),
                new RailActionRow(playOnly),
                new RailActionRow(regenerateAudio),
                choralVoiceSection,
                readTables,
                playbackNotice,
                source);
        body.getStyleClass().add("document-context-body");
        body.getStyleClass().add("document-fragment-inspector");
        body.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("document-context-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.activeVoiceLibraryProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.choralVoiceRenderingProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        viewModel.choralVoiceRenderProgressProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        viewModel.choralVoiceRenderStatusProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        refreshChoralVoiceSection();
    }

    private void configureChoralVoiceSection() {
        choralVoiceSection.getStyleClass().add("document-choral-voice-section");
        choralVoiceSection.setFillWidth(true);
        choralVoiceStatus.setWrapText(true);
        choralVoiceStatus.getStyleClass().add("document-context-note");
        allCharacters.getStyleClass().add("document-context-checkbox");
        allCharacters.setOnAction(event -> selectAllCharacters());
        choralVoiceProgress.setMaxWidth(Double.MAX_VALUE);
        choralVoiceProgress.setVisible(false);
        choralVoiceProgress.setManaged(false);
        renderChoralVoice.setMaxWidth(Double.MAX_VALUE);
        restoreSimpleVoice.setMaxWidth(Double.MAX_VALUE);
        choralVoiceSection.getChildren().setAll(
                new SectionHeader("Voces simultaneas", "Todas las voces seleccionadas diran el texto completo al mismo tiempo."),
                choralVoiceStatus,
                allCharacters,
                choralVoiceOptions,
                choralVoiceProgress,
                new RailActionRow(renderChoralVoice),
                new RailActionRow(restoreSimpleVoice));
    }

    private void refreshChoralVoiceSection() {
        boolean theatre = viewModel.currentProjectModeProperty().get() == ProjectMode.THEATRE_PRODUCTION;
        choralVoiceSection.setVisible(theatre);
        choralVoiceSection.setManaged(theatre);
        if (!theatre || viewModel.choralVoiceRenderingProperty().get()) {
            updateChoralVoiceControls();
            return;
        }
        refreshingChoralVoice = true;
        try {
            choralChecks.clear();
            choralVoiceOptions.getChildren().clear();
            List<TheatreChoralVoiceWorkflow.Option> options = viewModel.selectedTheatreChoralVoiceOptions();
            TheatreChoralVoiceWorkflow.State state = viewModel.selectedTheatreChoralVoiceState();
            for (TheatreChoralVoiceWorkflow.Option option : options) {
                CheckBox check = new CheckBox(option.displayName());
                check.getStyleClass().add("document-context-checkbox");
                check.setSelected(state.participantCharacterIds().contains(option.characterId()));
                check.setDisable(!option.voiceReady());
                check.setTooltip(new Tooltip(option.status()));
                check.setOnAction(event -> updateAllCharactersState());
                choralChecks.put(check, option);
                choralVoiceOptions.getChildren().add(check);
            }
            allCharacters.setDisable(options.stream().noneMatch(option -> option.voiceReady() && !option.narrator()));
            if (state.interventionId().isBlank()) {
                choralVoiceStatus.setText("Selecciona un fragmento teatral narrable.");
            } else if (state.stale()) {
                choralVoiceStatus.setText("La mezcla existente esta desactualizada. Renderizala de nuevo.");
            } else if (state.audioReady()) {
                choralVoiceStatus.setText("Mezcla activa: " + state.audioLabel() + ".");
            } else if (!state.participantCharacterIds().isEmpty()) {
                choralVoiceStatus.setText("Participantes configurados; falta renderizar la mezcla.");
            } else {
                choralVoiceStatus.setText("Selecciona al menos dos personajes con voz disponible.");
            }
            updateAllCharactersState();
        } finally {
            refreshingChoralVoice = false;
        }
        updateChoralVoiceControls();
    }

    private void selectAllCharacters() {
        if (refreshingChoralVoice) return;
        boolean selected = allCharacters.isSelected();
        refreshingChoralVoice = true;
        try {
            choralChecks.forEach((check, option) -> {
                if (option.voiceReady() && !option.narrator()) check.setSelected(selected);
            });
            allCharacters.setIndeterminate(false);
        } finally {
            refreshingChoralVoice = false;
        }
        updateChoralVoiceControls();
    }

    private void updateAllCharactersState() {
        if (refreshingChoralVoice) return;
        List<CheckBox> eligible = choralChecks.entrySet().stream()
                .filter(entry -> entry.getValue().voiceReady() && !entry.getValue().narrator())
                .map(Map.Entry::getKey)
                .toList();
        long selected = eligible.stream().filter(CheckBox::isSelected).count();
        refreshingChoralVoice = true;
        try {
            allCharacters.setSelected(!eligible.isEmpty() && selected == eligible.size());
            allCharacters.setIndeterminate(selected > 0 && selected < eligible.size());
        } finally {
            refreshingChoralVoice = false;
        }
        updateChoralVoiceControls();
    }

    private List<String> selectedChoralCharacterIds() {
        return choralChecks.entrySet().stream()
                .filter(entry -> entry.getKey().isSelected() && entry.getValue().voiceReady())
                .map(entry -> entry.getValue().characterId())
                .toList();
    }

    private void updateChoralVoiceControls() {
        boolean running = viewModel.choralVoiceRenderingProperty().get();
        choralVoiceProgress.setVisible(running);
        choralVoiceProgress.setManaged(running);
        choralVoiceProgress.setProgress(viewModel.choralVoiceRenderProgressProperty().get());
        if (running) choralVoiceStatus.setText(viewModel.choralVoiceRenderStatusProperty().get());
        choralChecks.keySet().forEach(check -> check.setDisable(running || !choralChecks.get(check).voiceReady()));
        allCharacters.setDisable(running || choralChecks.values().stream().noneMatch(option -> option.voiceReady() && !option.narrator()));
        renderChoralVoice.setDisable(running || !viewModel.canRenderSelectedTheatreChoralVoices(selectedChoralCharacterIds()));
        TheatreChoralVoiceWorkflow.State state = viewModel.selectedTheatreChoralVoiceState();
        restoreSimpleVoice.setDisable(running || state.interventionId().isBlank()
                || (state.participantCharacterIds().isEmpty() && state.mixedAudioAssetId().isBlank()));
    }

    private void renderChoralVoice() {
        viewModel.renderSelectedTheatreChoralVoices(selectedChoralCharacterIds());
        updateChoralVoiceControls();
    }

    private void restoreSimpleVoice() {
        viewModel.clearSelectedTheatreChoralVoices();
        refreshChoralVoiceSection();
    }

    private Button action(String text, Runnable handler) {
        Button button = ActionButtonFactory.rail(text, handler);
        button.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            return blockId == null || blockId.isBlank();
        }, viewModel.selectedDocumentBlockIdProperty()));
        return button;
    }
}
