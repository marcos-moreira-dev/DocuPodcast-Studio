package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioSourceOption;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceAssignmentOption;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceAssignmentReadinessContext;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RailActionRow;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentAudioAction;
import com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceToneLabelPolicy;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/** Contextual audio/narration module for the selected sentence. */
public final class DocumentAudioNarrationPanel extends VBox {
    private static final String MOCK_VOICE = "Modo de prueba";
    private static final String COMPUTER_AUDIO = "Audio del computador";
    private static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";

    private final DocuPodcastShellViewModel viewModel;
    private final VBox options = new VBox(8);
    private final ComboBox<AudioSourceOption> sourceSelector = StudioFormControls.comboBox();
    private final Label sourceStatus = new Label();
    private final ComboBox<VoiceAssignmentOption> voiceSelector = StudioFormControls.comboBox();
    private final ComboBox<VoiceReferenceTone> toneSelector = StudioFormControls.comboBox();
    private final Label voiceStatus = new Label();
    private final Consumer<DocumentAudioAction> documentAudioActionRequest;
    private boolean updatingChoices;
    private boolean updatingSourceChoices;

    public DocumentAudioNarrationPanel(DocuPodcastShellViewModel viewModel) {
        this(viewModel, null);
    }

    public DocumentAudioNarrationPanel(
            DocuPodcastShellViewModel viewModel,
            Consumer<DocumentAudioAction> documentAudioActionRequest) {
        this.viewModel = viewModel;
        this.documentAudioActionRequest = documentAudioActionRequest;
        getStyleClass().add("document-context-module");
        setPadding(new Insets(10));
        setSpacing(10);
        setMaxHeight(Double.MAX_VALUE);
        setFillWidth(true);

        configureVoiceSelectors();

        SectionHeader header = new SectionHeader(
                "Audio",
                "Elige voz generada por IA/local o usa un audio del computador para la selección.");

        Label selected = new Label();
        selected.getStyleClass().add("document-context-selected");
        selected.setWrapText(true);
        selected.textProperty().bind(viewModel.selectedDocumentRangeLabelProperty());

        Label origin = new Label("Origen");
        origin.getStyleClass().add("document-context-field-label");
        syncSourceChoices();
        configureSourceSelectorCells();
        sourceSelector.getStyleClass().add("document-context-combo");
        sourceSelector.setMaxWidth(Double.MAX_VALUE);
        sourceSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingSourceChoices && newValue != null && newValue.selectable()
                    && !"computer-audio".equals(newValue.id())) {
                viewModel.selectDocumentAudioSource(newValue.id());
            }
            renderOptions();
        });
        sourceStatus.getStyleClass().add("document-context-note");
        sourceStatus.setWrapText(true);

        CheckBox readAfterColon = StudioFormControls.checkBox(
                "Leer desde después de ':' (se aplica al procesar la lectura)");
        readAfterColon.getStyleClass().add("document-context-checkbox");
        readAfterColon.setSelected(viewModel.readAfterColonForNarrationProperty().get());
        viewModel.readAfterColonForNarrationProperty().addListener((obs, oldValue, newValue) -> {
            if (readAfterColon.isSelected() != newValue.booleanValue()) { readAfterColon.setSelected(newValue); }
        });
        readAfterColon.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue == newValue) { return; }
            viewModel.setReadAfterColonForNarration(newValue);
        });
        Label note = new Label("La fuente no cambia. La regla se guarda en el proyecto y se aplicará la próxima vez que pulses Procesar; cambiarla no inicia una generación automática.");
        note.getStyleClass().add("document-context-note");
        note.setWrapText(true);

        VBox body = new VBox(8, header, selected, origin, sourceSelector, sourceStatus, options, readAfterColon, note);
        body.getStyleClass().add("document-context-body");
        body.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = StudioViewportControls.scrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("document-context-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        renderOptions();
    }

    private void configureVoiceSelectors() {
        voiceSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(VoiceAssignmentOption voice) {
                return voice == null ? "" : voice.toString();
            }

            @Override
            public VoiceAssignmentOption fromString(String string) {
                return null;
            }
        });
        toneSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(VoiceReferenceTone tone) {
                return tone == null ? "" : VoiceToneLabelPolicy.comboLabel(tone);
            }

            @Override
            public VoiceReferenceTone fromString(String string) {
                return null;
            }
        });
        voiceSelector.getStyleClass().add("document-context-combo");
        toneSelector.getStyleClass().add("document-context-combo");
        voiceSelector.setMaxWidth(Double.MAX_VALUE);
        toneSelector.setMaxWidth(Double.MAX_VALUE);
        voiceSelector.setPromptText("Voz");
        toneSelector.setPromptText("Tono");
        voiceSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            voiceStatus.setText(appliedVoiceStatus() + "\n" + voiceStatus(newValue)
                    + ((!updatingChoices && newValue != null)
                    ? " La configuración queda activa; el audio se actualizará al generar."
                    : ""));
            if (!updatingChoices) {
                updatingChoices = true;
                try {
                    refreshToneChoicesForSelectedVoice(profileForCurrentSource(), voiceProfile(newValue));
                } finally {
                    updatingChoices = false;
                }
                Platform.runLater(this::configureSelectedVoiceWithoutRendering);
            }
        });
        toneSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingChoices && newValue != null) {
                Platform.runLater(this::configureSelectedVoiceWithoutRendering);
            }
        });
        voiceStatus.getStyleClass().add("document-context-note");
        voiceStatus.setWrapText(true);
        viewModel.activeVoiceLibraryProperty().addListener((obs, oldValue, newValue) -> renderOptions());
        viewModel.statusMessageProperty().addListener((obs, oldValue, newValue) -> {
            syncSourceChoices();
            renderOptions();
        });
    }

    private void renderOptions() {
        syncSourceChoices();
        options.getChildren().clear();
        if (sourceIs("computer-audio")) {
            options.getChildren().addAll(computerAudioOptions());
        } else {
            options.getChildren().addAll(voiceEngineOptions());
        }
    }

    private void syncSourceChoices() {
        AudioSourceOption current = sourceSelector.getValue();
        List<AudioSourceOption> choices = viewModel.documentAudioSourceAvailability().stream()
                .map(AudioSourceOption::from)
                .distinct()
                .toList();
        if (choices.isEmpty()) {
            choices = java.util.List.of(
                    new AudioSourceOption("mock", MOCK_VOICE, "Diagnóstico", true, "", ""),
                    new AudioSourceOption("computer-audio", COMPUTER_AUDIO, "Archivo manual", true, "", ""));
        }
        List<AudioSourceOption> availableChoices = choices;
        updatingSourceChoices = true;
        try {
            if (!sourceSelector.getItems().equals(availableChoices)) {
                sourceSelector.getItems().setAll(availableChoices);
            }
            AudioSourceOption preserved = current == null ? null : availableChoices.stream()
                    .filter(choice -> choice.id().equals(current.id())).findFirst().orElse(null);
            if (preserved != null) {
                sourceSelector.setValue(preserved);
                updateSourceStatus(availableChoices);
                return;
            }
            String active = viewModel.audioEngineDescriptor().engineId();
            AudioSourceOption selected = availableChoices.stream()
                    .filter(choice -> choice.id().equalsIgnoreCase(active)).findFirst()
                    .orElseGet(() -> availableChoices.stream().filter(AudioSourceOption::selectable)
                            .findFirst().orElse(availableChoices.getFirst()));
            sourceSelector.setValue(selected);
            updateSourceStatus(availableChoices);
        } finally {
            updatingSourceChoices = false;
        }
    }

    private void updateSourceStatus(List<AudioSourceOption> choices) {
        AudioSourceOption selected = sourceSelector.getValue();
        String readinessDetail = String.join("\n", viewModel.audioEngineReadinessLines());
        sourceStatus.setTooltip(new Tooltip(readinessDetail));
        if (selected == null) {
            sourceStatus.setText("Selecciona un origen de audio disponible.");
            return;
        }
        if ("computer-audio".equals(selected.id())) {
            sourceStatus.setText("Audio del computador: elige un archivo local para esta selección; no usa motor de voz.");
            return;
        }
        if (!selected.selectable()) {
            sourceStatus.setText(selected.message() + (selected.recommendedAction().isBlank()
                    ? "" : " " + selected.recommendedAction()));
            return;
        }
        sourceStatus.setText(selected.accessibleLabel() + ": origen operativo y listo para la lectura.");
    }

    private VoiceEngineCapabilityProfile profileForCurrentSource() {
        return viewModel.administrationWorkspace().voice().voiceCapabilityPolicy()
                .activeEngineProfile(descriptorForCurrentSource());
    }

    private VBox voiceEngineOptions() {
        VoiceEngineCapabilityProfile profile = profileForCurrentSource();
        if (profile.simpleLocalMode()) {
            TextField nativeVoice = StudioFormControls.textField("Narrador predeterminado — voz del motor");
            nativeVoice.setDisable(true);
            return new VBox(8, label("Voz de la lectura"), nativeVoice,
                    notice("El motor seleccionado usa su voz predeterminada para todos los fragmentos. Las voces y tonos personalizados quedan inactivos, pero se conservan para volver a usarlos con otro motor."));
        }
        refreshVoiceChoices(profile);
        Label summary = new Label(generatedVoiceNotice(profile));
        summary.getStyleClass().add("document-context-copy");
        summary.setWrapText(true);

        Button manageVoices = ActionButtonFactory.secondary("Ir a biblioteca de voces", viewModel::showVoiceLibraryWorkspace);
        Button useDefaultVoice = ActionButtonFactory.primary(
                "Aplicar voz y regenerar toda la lectura",
                this::useSelectedVoiceForWholeDocument);
        useDefaultVoice.disableProperty().bind(Bindings.createBooleanBinding(
                () -> voiceSelector.getValue() == null || !voiceSelector.getValue().selectable(),
                voiceSelector.valueProperty()));
        Button assignSpecificVoice = ActionButtonFactory.secondary("Asignar voz al fragmento seleccionado", this::assignSelectedVoiceToSelectedFragment);
        bindToSelectionAndUsableVoice(assignSpecificVoice);
        Button useGeneralVoice = ActionButtonFactory.warning(
                "Asignar voz general",
                "El fragmento volverá a usar la voz predeterminada del documento.",
                viewModel::removePrimaryAssignmentForSelectedDocumentRange);
        Button clearSpecificVoices = ActionButtonFactory.danger("Eliminar voces específicas de la lectura", viewModel::removeAllSpecificVoicesFromDocument);
        bindToSelection(useGeneralVoice);

        VBox box = new VBox(8);
        box.getStyleClass().add("document-audio-flow");
        if (profile.diagnosticMode()) {
            box.getChildren().addAll(
                    label("Motor de prueba"),
                    notice("El modo de prueba solo valida el flujo. Prepara una voz real desde Configuración para escuchar la lectura."),
                    manageVoices,
                    useGeneralVoice);
            return new VBox(8, summary, box);
        }

        box.getChildren().addAll(label("Voz y tono predeterminados de la lectura"), voiceSelector, voiceStatus, useDefaultVoice, manageVoices);
        VoiceProfile selectedVoice = selectedVoiceProfile();
        if (voiceSelector.getItems().isEmpty()) {
            box.getChildren().add(notice(emptyVoiceNotice(profile)));
        } else if (profile.supportsEmotion() || profile.supportsExpressiveStyle()) {
            if (selectedVoice == null) {
                box.getChildren().add(notice("Elige una voz. El campo Tono solo mostrará emociones que esa voz ya tenga grabadas o importadas."));
            } else if (usesDefaultEngineVoice(selectedVoice)) {
                box.getChildren().add(notice("Narrador predeterminado usara la voz por defecto del motor. Para tonos grabados, registra una voz con muestra Neutral."));
            } else {
                Label toneStatus = new Label();
                toneStatus.getStyleClass().add("document-context-note");
                toneStatus.setWrapText(true);
                toneStatus.textProperty().bind(viewModel.documentVoiceToneStatusProperty());
                box.getChildren().addAll(label("Tono predeterminado"), toneSelector, toneStatus);
            }
        } else if (profile.simpleLocalMode()) {
            box.getChildren().add(notice("La Voz local simple usa la voz local disponible. No muestra tonos por muestra humana; para tonos grabados usa Voz IA avanzada."));
        } else if (!profile.supportsAdvancedExpressiveControls()) {
            box.getChildren().add(notice(profile.blockedReason().isBlank() ? "Este motor no declara soporte expresivo avanzado." : profile.blockedReason()));
        }
        box.getChildren().addAll(label("Voz y tono del fragmento seleccionado"),
                assignSpecificVoice, useGeneralVoice, clearSpecificVoices);
        return new VBox(8, summary, box);
    }


    private static String generatedVoiceNotice(VoiceEngineCapabilityProfile profile) {
        if (profile == null) {
            return "Voz generada: elige una voz disponible para la selección o usa Audio del computador.";
        }
        if (profile.advancedAiMode()) {
            return "Voz generada: usa Narrador predeterminado o selecciona una voz registrada con muestra Neutral. Las muestras son referencias para generar texto nuevo.";
        }
        if (profile.simpleLocalMode()) {
            return "Voz generada: usa la voz local simple disponible. Si el runtime soporta el dispositivo elegido, lo recibirá al renderizar.";
        }
        if (profile.diagnosticMode()) {
            return "Voz generada: el modo de prueba valida el flujo, pero no produce una voz final real.";
        }
        return profile.sidebarNotice();
    }

    private void useSelectedVoiceForWholeDocument() {
        if (updatingChoices) {
            return;
        }
        VoiceProfile voice = selectedVoiceProfile();
        if (voice == null) {
            return;
        }
        String selectedBlockId = viewModel.selectedDocumentBlockIdProperty().get();
        boolean hasSelection = selectedBlockId != null && !selectedBlockId.isBlank();
        VoiceReferenceTone tone = toneSelector.getValue() == null
                ? VoiceReferenceTone.NEUTRAL : toneSelector.getValue();
        long overrides = viewModel.specificDocumentVoiceOverrideCount();
        Alert confirm = NativeDialogResponse.alert(Alert.AlertType.CONFIRMATION);
        StudioMessageDialog.configure(
                confirm,
                ownerWindow(),
                "Usar voz en toda la lectura",
                "¿Quieres aplicar «" + voice.displayName() + "» con tono "
                        + VoiceToneLabelPolicy.comboLabel(tone) + " a toda la lectura?",
                "Los fragmentos con configuración global se re-renderizarán.\n"
                        + "Se respetarán " + overrides
                        + " fragmento(s) con voz o tono específico."
                        + (hasSelection
                        ? "\n\nPuedes re-renderizar desde el fragmento seleccionado o desde el inicio."
                        : ""),
                "");
        ButtonType fromSelection = NativeDialogResponse.button("Desde fragmento seleccionado", ButtonBar.ButtonData.YES);
        ButtonType fromStart = NativeDialogResponse.button("Desde el inicio", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(hasSelection ? List.of(fromSelection, fromStart, ButtonType.CANCEL) : List.of(fromStart, ButtonType.CANCEL));
        confirm.showAndWait().ifPresent(button -> {
            if (button == fromSelection && hasSelection) {
                if (viewModel.configureVoiceForDocument(voice, tone)) {
                    requestDocumentAudioAction(DocumentAudioAction.GENERATE_SELECTION);
                }
            } else if (button == fromStart) {
                if (viewModel.configureVoiceForDocument(voice, tone)) {
                    requestDocumentAudioAction(DocumentAudioAction.GENERATE_ALL);
                }
            }
        });
        renderOptions();
    }

    private void configureSelectedVoiceWithoutRendering() {
        if (updatingChoices) return;
        VoiceProfile voice = selectedVoiceProfile();
        if (voice == null) return;
        VoiceReferenceTone tone = toneSelector.getValue() == null
                ? VoiceReferenceTone.NEUTRAL : toneSelector.getValue();
        viewModel.configureVoiceForDocument(voice, tone);
    }

    private void requestDocumentAudioAction(DocumentAudioAction action) {
        if (documentAudioActionRequest != null) {
            documentAudioActionRequest.accept(action);
        } else if (action == DocumentAudioAction.GENERATE_SELECTION) {
            viewModel.generateAudioChunksFromSelectedFragment();
        } else {
            viewModel.generateAudioChunksWithoutPlayback();
        }
    }

    private void assignSelectedVoiceToSelectedFragment() {
        if (updatingChoices) {
            return;
        }
        VoiceProfile voice = selectedVoiceProfile();
        if (voice == null || !hasSelectedDocumentBlock()) {
            return;
        }
        if (toneSelector.getItems().isEmpty()) {
            viewModel.assignVoiceToSelectedDocumentRange(voice.id());
            return;
        }
        VoiceReferenceTone tone = toneSelector.getValue() == null ? VoiceReferenceTone.NEUTRAL : toneSelector.getValue();
        viewModel.assignVoiceToneToSelectedDocumentRange(voice.id(), tone);
    }

    private boolean hasSelectedDocumentBlock() {
        String selectedBlockId = viewModel.selectedDocumentBlockIdProperty().get();
        return selectedBlockId != null && !selectedBlockId.isBlank();
    }

    private void refreshVoiceChoices(VoiceEngineCapabilityProfile profile) {
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        String previousVoiceId = voiceSelector.getValue() == null ? "" : voiceSelector.getValue().voiceId();
        String configuredVoiceId = viewModel.configuredVoiceProfileId();
        updatingChoices = true;
        try {
            List<VoiceAssignmentOption> voices = library == null
                    ? List.of()
                    : assignmentOptionsForCurrentSource(library, configuredVoiceId);
            voiceSelector.getItems().setAll(voices);
            VoiceAssignmentOption selected = voices.stream()
                    .filter(voice -> voice.voiceId().equals(previousVoiceId))
                    .findFirst()
                    .orElseGet(() -> voices.stream()
                            .filter(voice -> voice.voiceId().equals(configuredVoiceId))
                            .findFirst()
                            .orElseGet(() -> voices.stream()
                                    .filter(option -> DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(option.voiceId()))
                                    .filter(VoiceAssignmentOption::selectable)
                                    .findFirst()
                                    .orElseGet(() -> voices.stream()
                                            .filter(VoiceAssignmentOption::selectable)
                                            .findFirst()
                                            .orElse(voices.isEmpty() ? null : voices.getFirst()))));
            voiceSelector.setValue(selected);
            voiceStatus.setText(appliedVoiceStatus() + "\n" + voiceStatus(selected));
            refreshToneChoicesForSelectedVoice(profile, voiceProfile(selected));
        } finally {
            updatingChoices = false;
        }
    }

    private List<VoiceAssignmentOption> assignmentOptionsForCurrentSource(VoiceLibrary library, String currentVoiceId) {
        var roots = viewModel.runtimeWorkspace();
        VoiceAssignmentReadinessContext readiness = new VoiceAssignmentReadinessContext(
                roots.installationRoot(), roots.runtimeRoot(),
                viewModel.currentProjectDirectory().orElse(roots.runtimeRoot()));
        return viewModel.administrationWorkspace().voice().buildVoiceAssignmentOptions()
                .build(library, descriptorForCurrentSource(), List.of(), currentVoiceId, readiness);
    }

    private AudioEngineDescriptor descriptorForCurrentSource() {
        AudioSourceOption selected = sourceSelector.getValue();
        if (selected == null || "computer-audio".equals(selected.id())) {
            return viewModel.audioEngineDescriptor();
        }
        AudioEngineAvailability availability = viewModel.documentAudioSourceAvailability().stream()
                .filter(candidate -> candidate.engineId().equalsIgnoreCase(selected.id()))
                .findFirst().orElse(null);
        if (availability == null) return viewModel.audioEngineDescriptor();
        java.util.Set<EngineFeature> features = new java.util.LinkedHashSet<>();
        if (availability.supportsVoiceSamples()) features.add(EngineFeature.REFERENCE_VOICE);
        if (availability.supportsTones()) features.add(EngineFeature.EXPRESSIVE_STYLE);
        if (!availability.supportsVoiceSamples() && availability.realTts()) {
            features.add(EngineFeature.PACKAGED_VOICE);
        }
        return new AudioEngineDescriptor(availability.engineId(), availability.displayName(),
                availability.mode(), availability.usableInDocument(), availability.realTts(), "",
                availability.userMessage(), features, !availability.realTts());
    }

    private boolean sourceIs(String id) {
        AudioSourceOption selected = sourceSelector.getValue();
        return selected != null && id.equals(selected.id());
    }

    private void configureSourceSelectorCells() {
        sourceSelector.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(AudioSourceOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setDisable(false);
                    return;
                }
                setText(item.accessibleLabel() + (item.selectable() ? "" : " — no disponible"));
                setDisable(!item.selectable());
                String detail = item.message() + (item.recommendedAction().isBlank()
                        ? "" : " " + item.recommendedAction());
                setTooltip(detail.isBlank() ? null : new Tooltip(detail));
            }
        });
        sourceSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(AudioSourceOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.accessibleLabel());
            }
        });
    }

    private void refreshToneChoicesForSelectedVoice(VoiceEngineCapabilityProfile profile, VoiceProfile selectedVoice) {
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        VoiceReferenceTone previousTone = toneSelector.getValue();
        if (previousTone == null) {
            previousTone = viewModel.configuredDocumentVoiceTone();
        }
        List<VoiceReferenceTone> tones = registeredDocumentTones(library, selectedVoice, profile);
        toneSelector.getItems().setAll(tones);
        toneSelector.getSelectionModel().clearSelection();
        if (tones.isEmpty()) {
            return;
        }
        if (previousTone != null && tones.contains(previousTone)) {
            toneSelector.setValue(previousTone);
        } else if (tones.contains(VoiceReferenceTone.NEUTRAL)) {
            toneSelector.setValue(VoiceReferenceTone.NEUTRAL);
        } else {
            toneSelector.setValue(tones.get(0));
        }
    }

    private List<VoiceReferenceTone> registeredDocumentTones(VoiceLibrary library, VoiceProfile voice, VoiceEngineCapabilityProfile profile) {
        if (library == null || voice == null || profile == null) {
            return List.of();
        }
        if (!(profile.supportsEmotion() || profile.supportsExpressiveStyle())) {
            return List.of();
        }
        if (usesDefaultEngineVoice(voice)) {
            return List.of();
        }
        return library.referenceSampleSetByVoiceId(voice.id())
                .filter(sampleSet -> sampleSet.hasNeutral())
                .map(sampleSet -> neutralFirst(sampleSet.registeredTones()).stream()
                        .filter(tone -> sampleSet.sampleFor(tone)
                                .filter(this::referenceSampleAvailable).isPresent())
                        .toList())
                .orElse(List.of());
    }

    private boolean referenceSampleAvailable(
            com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample sample) {
        try {
            var roots = viewModel.runtimeWorkspace();
            new VoiceReferenceSamplePathResolver(roots.installationRoot(), roots.runtimeRoot())
                    .resolve(viewModel.currentProjectDirectory().orElse(roots.runtimeRoot()), sample,
                            "muestra de tono");
            return true;
        } catch (IOException | RuntimeException unavailable) {
            return false;
        }
    }

    private static List<VoiceReferenceTone> neutralFirst(List<VoiceReferenceTone> registeredTones) {
        LinkedHashSet<VoiceReferenceTone> ordered = new LinkedHashSet<>();
        if (registeredTones != null && registeredTones.contains(VoiceReferenceTone.NEUTRAL)) {
            ordered.add(VoiceReferenceTone.NEUTRAL);
        }
        if (registeredTones != null) {
            registeredTones.stream()
                    .filter(tone -> tone != null && tone != VoiceReferenceTone.NEUTRAL)
                    .forEach(ordered::add);
        }
        return List.copyOf(ordered);
    }

    static boolean usableInDocumentForEngine(VoiceLibrary library, VoiceProfile voice, VoiceEngineCapabilityProfile profile) {
        if (voice == null || profile == null) {
            return false;
        }
        if (profile.advancedAiMode()) {
            if (usesDefaultEngineVoice(voice)
                    || usesAdvancedPredesignedNeutral(voice)
                    || OfficialAdvancedVoicePresetCatalog.isOfficialPreset(voice)) {
                return true;
            }
            return library != null
                    && profile.supportsVoiceProfile(voice)
                    && library.referenceSampleSetByVoiceId(voice.id()).filter(sampleSet -> sampleSet.hasNeutral()).isPresent();
        }
        return true;
    }

    private static String emptyVoiceNotice(VoiceEngineCapabilityProfile profile) {
        if (profile.advancedAiMode()) {
            return "Registra una voz con muestra Neutral o restaura la biblioteca para recuperar el Narrador predeterminado. Las voces no listas permanecen visibles con su motivo.";
        }
        if (profile.simpleLocalMode()) {
            return "No hay una voz local simple disponible. Prepárala desde Configuración o Voces.";
        }
        if (profile.diagnosticMode()) {
            return "No hay una voz de prueba disponible. Revisa la biblioteca de voces inicial.";
        }
        return "No hay voces disponibles para el motor activo.";
    }

    static boolean usesDefaultEngineVoice(VoiceProfile voice) {
        return voice != null && DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(voice.id());
    }

    static boolean usesAdvancedPredesignedNeutral(VoiceProfile voice) {
        return voice != null && "VOC-OWN-PLACEHOLDER".equalsIgnoreCase(voice.id())
                && !voice.metadata().containsKey("userManaged");
    }

    private boolean visibleForEngine(VoiceProfile voice, VoiceEngineCapabilityProfile profile) {
        if (voice == null) {
            return false;
        }
        return profile.supportsVoiceProfile(voice);
    }

    private VBox computerAudioOptions() {
        Label summary = new Label("Usa un archivo de audio propio o extrae sonido de un video. Este camino no usa voz generada.");
        summary.getStyleClass().add("document-context-copy");
        summary.setWrapText(true);

        Button choose = action("Elegir audio", this::chooseAudioFromComputer);
        Button extract = action("Extraer video", this::extractAudioFromVideo);
        Button remove = ActionButtonFactory.danger("Quitar audio", viewModel::removePrimaryAssignmentForSelectedDocumentRange);
        bindToSelection(remove);

        VBox box = new VBox(8,
                label("Audio del computador"),
                new RailActionRow(choose),
                new RailActionRow(extract),
                remove);
        box.getStyleClass().add("document-audio-flow");
        return new VBox(8, summary, box);
    }

    private void chooseAudioFromComputer() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
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
        FileChooser chooser = NativeSourceChooser.fileChooser();
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

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-context-field-label");
        return label;
    }

    private Label notice(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-context-note");
        label.setWrapText(true);
        return label;
    }

    private Button action(String text, Runnable handler) {
        Button button = ActionButtonFactory.rail(text, handler);
        bindToSelection(button);
        return button;
    }

    private void bindToSelection(Control control) {
        control.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            return blockId == null || blockId.isBlank();
        }, viewModel.selectedDocumentBlockIdProperty()));
    }

    private void bindToSelectionAndUsableVoice(Control control) {
        control.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            VoiceAssignmentOption voice = voiceSelector.getValue();
            return blockId == null || blockId.isBlank() || voice == null || !voice.selectable();
        }, viewModel.selectedDocumentBlockIdProperty(), voiceSelector.valueProperty()));
    }

    private VoiceProfile selectedVoiceProfile() {
        return voiceProfile(voiceSelector.getValue());
    }

    private VoiceProfile voiceProfile(VoiceAssignmentOption option) {
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        return library == null || option == null
                ? null
                : library.voiceById(option.voiceId()).orElse(null);
    }

    private static String voiceStatus(VoiceAssignmentOption option) {
        if (option == null) {
            return "Selecciona una voz disponible.";
        }
        return option.status() + ". " + option.detail();
    }

    private String appliedVoiceStatus() {
        String voiceId = viewModel.configuredVoiceProfileId();
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        String displayName = library == null ? voiceId : library.voiceById(voiceId)
                .map(VoiceProfile::displayName).orElse(voiceId);
        if (displayName == null || displayName.isBlank()) {
            displayName = "Narrador predeterminado";
        }
        return "Aplicada: " + displayName + " · tono "
                + VoiceToneLabelPolicy.comboLabel(
                viewModel.configuredDocumentVoiceTone()) + ".";
    }
}
