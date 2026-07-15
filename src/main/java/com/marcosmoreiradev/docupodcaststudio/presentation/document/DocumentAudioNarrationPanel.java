package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceAssignmentOption;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RailActionRow;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceToneLabelPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Contextual audio/narration module for the selected sentence. */
public final class DocumentAudioNarrationPanel extends VBox {
    private static final String ADVANCED_VOICE = "Voz IA avanzada";
    private static final String LOCAL_SIMPLE_VOICE = "Voz local simple";
    private static final String MOCK_VOICE = "Modo de prueba";
    private static final String COMPUTER_AUDIO = "Audio del computador";
    private static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";

    private final DocuPodcastShellViewModel viewModel;
    private final VBox options = new VBox(8);
    private final ComboBox<String> sourceSelector = new ComboBox<>();
    private final Label sourceStatus = new Label();
    private final ComboBox<VoiceProfile> voiceSelector = new ComboBox<>();
    private final ComboBox<VoiceReferenceTone> toneSelector = new ComboBox<>();
    private boolean updatingChoices;
    private boolean updatingSourceChoices;

    public DocumentAudioNarrationPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
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
        sourceSelector.getStyleClass().add("document-context-combo");
        sourceSelector.setMaxWidth(Double.MAX_VALUE);
        sourceSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingSourceChoices && newValue != null && !COMPUTER_AUDIO.equals(newValue)) {
                viewModel.selectDocumentAudioSource(newValue);
            }
            renderOptions();
        });
        sourceStatus.getStyleClass().add("document-context-note");
        sourceStatus.setWrapText(true);

        CheckBox readAfterColon = new CheckBox("Leer desde después de ':' (requiere reconstruir fragmentos de audio)");
        readAfterColon.getStyleClass().add("document-context-checkbox");
        readAfterColon.setSelected(viewModel.readAfterColonForNarrationProperty().get());
        viewModel.readAfterColonForNarrationProperty().addListener((obs, oldValue, newValue) -> {
            if (readAfterColon.isSelected() != newValue.booleanValue()) { readAfterColon.setSelected(newValue); }
        });
        readAfterColon.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue == newValue) { return; }
            String selectedBlockId = viewModel.selectedDocumentBlockIdProperty().get();
            boolean hasSelection = selectedBlockId != null && !selectedBlockId.isBlank();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Leer después de dos puntos");
            confirm.setHeaderText("¿A partir de dónde quieres reconstruir los fragmentos de audio?");
            confirm.setContentText("Los fragmentos de audio se re-renderizarán con la voz global y el tono seleccionados."
                    + (hasSelection ? "\n\nPuedes re-renderizar desde el fragmento seleccionado o desde el inicio." : ""));
            ButtonType fromSelection = new ButtonType("Desde fragmento seleccionado", ButtonBar.ButtonData.YES);
            ButtonType fromStart = new ButtonType("Desde el inicio", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(hasSelection ? List.of(fromSelection, fromStart, ButtonType.CANCEL) : List.of(fromStart, ButtonType.CANCEL));
            DialogStyler.apply(confirm, ownerWindow());
            confirm.showAndWait().ifPresentOrElse(button -> {
                if (button == fromSelection && hasSelection) {
                    viewModel.setReadAfterColonForNarration(newValue, true);
                } else if (button == fromStart) {
                    viewModel.setReadAfterColonForNarration(newValue, false);
                } else {
                    readAfterColon.setSelected(oldValue);
                }
            }, () -> readAfterColon.setSelected(oldValue));
        });
        Label note = new Label("El documento fuente no cambia. La voz, el audio y la regla de ':' se guardan como capas del proyecto. Al cambiar la voz del documento se regeneran los fragmentos de audio; las voces específicas de fragmento se respetan.");
        note.getStyleClass().add("document-context-note");
        note.setWrapText(true);

        VBox body = new VBox(8, header, selected, origin, sourceSelector, sourceStatus, options, readAfterColon, note);
        body.getStyleClass().add("document-context-body");
        body.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(body);
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
            public String toString(VoiceProfile voice) {
                return voice == null ? "" : voice.displayName();
            }

            @Override
            public VoiceProfile fromString(String string) {
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
            if (!updatingChoices) {
                updatingChoices = true;
                try {
                    refreshToneChoicesForSelectedVoice(profileForCurrentSource(), newValue);
                } finally {
                    updatingChoices = false;
                }
            }
        });
        viewModel.activeVoiceLibraryProperty().addListener((obs, oldValue, newValue) -> renderOptions());
        viewModel.statusMessageProperty().addListener((obs, oldValue, newValue) -> {
            syncSourceChoices();
            renderOptions();
        });
    }

    private void renderOptions() {
        syncSourceChoices();
        options.getChildren().clear();
        if (COMPUTER_AUDIO.equals(sourceSelector.getValue())) {
            options.getChildren().addAll(computerAudioOptions());
        } else {
            options.getChildren().addAll(voiceEngineOptions());
        }
    }

    private void syncSourceChoices() {
        String current = sourceSelector.getValue();
        List<String> choices = viewModel.documentAudioSourceAvailability().stream()
                .filter(AudioEngineAvailability::usableInDocument)
                .map(AudioEngineAvailability::displayName)
                .distinct()
                .toList();
        if (choices.isEmpty()) {
            choices = java.util.List.of(MOCK_VOICE, COMPUTER_AUDIO);
        }
        updatingSourceChoices = true;
        try {
            if (!sourceSelector.getItems().equals(choices)) {
                sourceSelector.getItems().setAll(choices);
            }
            if (current != null && choices.contains(current)) {
                sourceSelector.setValue(current);
                updateSourceStatus(choices);
                return;
            }
            String active = voiceSourceLabel(viewModel.applicationServices().voice().voiceCapabilityPolicy()
                    .activeEngineProfile(viewModel.audioEngineDescriptor()));
            sourceSelector.setValue(choices.contains(active) ? active : choices.get(0));
            updateSourceStatus(choices);
        } finally {
            updatingSourceChoices = false;
        }
    }

    private void updateSourceStatus(List<String> choices) {
        String selected = sourceSelector.getValue();
        String active = selected == null || selected.isBlank() ? (choices.isEmpty() ? MOCK_VOICE : choices.get(0)) : selected;
        String readinessDetail = String.join("\n", viewModel.audioEngineReadinessLines());
        sourceStatus.setTooltip(new Tooltip(readinessDetail));
        if (COMPUTER_AUDIO.equals(active)) {
            sourceStatus.setText("Audio del computador: elige un archivo local para esta selección; no usa motor de voz.");
            return;
        }
        if (choices.contains(ADVANCED_VOICE)) {
            sourceStatus.setText("Origen operativo: solo se listan motores que pasaron preparación y prueba suficiente para usarse en Documento.");
            return;
        }
        sourceStatus.setText("Voz IA avanzada no aparece aquí si no pasó prueba WAV. Usa Configuración para repararla o continúa con Voz local simple / Modo de prueba.");
    }

    private static String voiceSourceLabel(VoiceEngineCapabilityProfile profile) {
        if (profile == null) {
            return MOCK_VOICE;
        }
        if (profile.piperMode()) {
            return LOCAL_SIMPLE_VOICE;
        }
        if (profile.coquiXttsMode()) {
            return ADVANCED_VOICE;
        }
        if (profile.mockMode()) {
            return MOCK_VOICE;
        }
        return "Motor de voz local";
    }


    private VoiceEngineCapabilityProfile profileForCurrentSource() {
        String selected = sourceSelector.getValue();
        if (LOCAL_SIMPLE_VOICE.equals(selected)) {
            return VoiceEngineCapabilityProfile.piper(true, LOCAL_SIMPLE_VOICE);
        }
        if (ADVANCED_VOICE.equals(selected)) {
            return VoiceEngineCapabilityProfile.coquiXtts(true, ADVANCED_VOICE);
        }
        if (MOCK_VOICE.equals(selected)) {
            return VoiceEngineCapabilityProfile.mock();
        }
        return viewModel.applicationServices().voice().voiceCapabilityPolicy()
                .activeEngineProfile(viewModel.audioEngineDescriptor());
    }

    private static boolean isVoiceSourceLabel(String value) {
        return ADVANCED_VOICE.equals(value)
                || LOCAL_SIMPLE_VOICE.equals(value)
                || MOCK_VOICE.equals(value)
                || "Motor de voz local".equals(value);
    }

    private VBox voiceEngineOptions() {
        VoiceEngineCapabilityProfile profile = profileForCurrentSource();
        refreshVoiceChoices(profile);
        Label summary = new Label(generatedVoiceNotice(profile));
        summary.getStyleClass().add("document-context-copy");
        summary.setWrapText(true);

        Button manageVoices = ActionButtonFactory.secondary("Ir a biblioteca de voces", viewModel::showVoiceLibraryWorkspace);
        Button useDefaultVoice = ActionButtonFactory.primary("Usar voz en todo el documento", this::useSelectedVoiceForWholeDocument);
        useDefaultVoice.disableProperty().bind(voiceSelector.valueProperty().isNull());
        Button assignSpecificVoice = ActionButtonFactory.secondary("Asignar voz al fragmento seleccionado", this::assignSelectedVoiceToSelectedFragment);
        bindToSelection(assignSpecificVoice);
        Button useGeneralVoice = ActionButtonFactory.warning(
                "Asignar voz general",
                "El fragmento volverá a usar la voz predeterminada del documento.",
                viewModel::removePrimaryAssignmentForSelectedDocumentRange);
        Button clearSpecificVoices = ActionButtonFactory.danger("Eliminar voces específicas del documento", viewModel::removeAllSpecificVoicesFromDocument);
        bindToSelection(useGeneralVoice);

        VBox box = new VBox(8);
        box.getStyleClass().add("document-audio-flow");
        if (profile.mockMode()) {
            box.getChildren().addAll(
                    label("Motor de prueba"),
                    notice("El modo de prueba solo valida el flujo. Prepara una voz real desde Configuración para escuchar documentos."),
                    manageVoices,
                    useGeneralVoice);
            return new VBox(8, summary, box);
        }

        box.getChildren().addAll(label("Voz predeterminada del documento"), voiceSelector, useDefaultVoice, manageVoices);
        VoiceProfile selectedVoice = voiceSelector.getValue();
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
                box.getChildren().addAll(label("Tono"), toneSelector, toneStatus);
            }
        } else if (profile.piperMode()) {
            box.getChildren().add(notice("La Voz local simple usa la voz local disponible. No muestra tonos por muestra humana; para tonos grabados usa Voz IA avanzada."));
        } else if (!profile.supportsAdvancedExpressiveControls()) {
            box.getChildren().add(notice(profile.blockedReason().isBlank() ? "Este motor no declara soporte expresivo avanzado." : profile.blockedReason()));
        }
        box.getChildren().addAll(assignSpecificVoice, useGeneralVoice, clearSpecificVoices);
        return new VBox(8, summary, box);
    }


    private static String generatedVoiceNotice(VoiceEngineCapabilityProfile profile) {
        if (profile == null) {
            return "Voz generada: elige una voz disponible para la selección o usa Audio del computador.";
        }
        if (profile.coquiXttsMode()) {
            return "Voz generada: usa Narrador predeterminado o selecciona una voz registrada con muestra Neutral. Las muestras son referencias para generar texto nuevo.";
        }
        if (profile.piperMode()) {
            return "Voz generada: usa la voz local simple disponible. Si el runtime soporta el dispositivo elegido, lo recibirá al renderizar.";
        }
        if (profile.mockMode()) {
            return "Voz generada: el modo de prueba valida el flujo, pero no produce una voz final real.";
        }
        return profile.sidebarNotice();
    }

    private void useSelectedVoiceForWholeDocument() {
        if (updatingChoices) {
            return;
        }
        VoiceProfile voice = voiceSelector.getValue();
        if (voice == null) {
            return;
        }
        String selectedBlockId = viewModel.selectedDocumentBlockIdProperty().get();
        boolean hasSelection = selectedBlockId != null && !selectedBlockId.isBlank();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Usar voz en todo el documento");
        confirm.setHeaderText("¿Quieres aplicar la voz «" + voice.displayName() + "» a todo el documento?");
        confirm.setContentText("Los fragmentos de audio que tienen voz global se re-renderizarán.\n"
                + "Los fragmentos de audio con voz específica se respetan."
                + (hasSelection ? "\n\nPuedes re-renderizar desde el fragmento seleccionado o desde el inicio." : ""));
        ButtonType fromSelection = new ButtonType("Desde fragmento seleccionado", ButtonBar.ButtonData.YES);
        ButtonType fromStart = new ButtonType("Desde el inicio", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(hasSelection ? List.of(fromSelection, fromStart, ButtonType.CANCEL) : List.of(fromStart, ButtonType.CANCEL));
        DialogStyler.apply(confirm, ownerWindow());
        confirm.showAndWait().ifPresent(button -> {
            if (button == fromSelection && hasSelection) {
                viewModel.useVoiceForDocumentFrom(voice, selectedBlockId);
            } else if (button == fromStart) {
                viewModel.useVoiceForDocument(voice);
            }
        });
        renderOptions();
    }

    private void assignSelectedVoiceToSelectedFragment() {
        if (updatingChoices) {
            return;
        }
        VoiceProfile voice = voiceSelector.getValue();
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
        String previousVoiceId = voiceSelector.getValue() == null ? "" : voiceSelector.getValue().id();
        String configuredVoiceId = viewModel.configuredVoiceProfileId();
        updatingChoices = true;
        try {
            List<VoiceProfile> voices = library == null
                    ? List.of()
                    : assignmentOptionsForCurrentSource(library, previousVoiceId.isBlank() ? configuredVoiceId : previousVoiceId).stream()
                    .filter(VoiceAssignmentOption::selectable)
                    .map(option -> library.voiceById(option.voiceId()).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(voice -> usableInDocumentForEngine(library, voice, profile))
                    .toList();
            voiceSelector.getItems().setAll(voices);
            VoiceProfile selected = voices.stream()
                    .filter(voice -> voice.id().equals(previousVoiceId))
                    .findFirst()
                    .orElseGet(() -> voices.stream()
                            .filter(voice -> voice.id().equals(configuredVoiceId))
                            .findFirst()
                            .orElseGet(() -> voices.stream()
                                    .filter(DocumentAudioNarrationPanel::usesDefaultEngineVoice)
                                    .findFirst()
                                    .orElse(null)));
            voiceSelector.setValue(selected);
            refreshToneChoicesForSelectedVoice(profile, selected);
        } finally {
            updatingChoices = false;
        }
    }

    private List<VoiceAssignmentOption> assignmentOptionsForCurrentSource(VoiceLibrary library, String currentVoiceId) {
        return viewModel.applicationServices().voice().buildVoiceAssignmentOptions()
                .build(library, descriptorForCurrentSource(), List.of(), currentVoiceId);
    }

    private AudioEngineDescriptor descriptorForCurrentSource() {
        String selected = sourceSelector.getValue();
        if (LOCAL_SIMPLE_VOICE.equals(selected)) {
            return AudioEngineDescriptor.process(LOCAL_SIMPLE_VOICE, true, "", "Configurado desde Audio.");
        }
        if (ADVANCED_VOICE.equals(selected)) {
            return AudioEngineDescriptor.process(ADVANCED_VOICE, true, "", "Configurado desde Audio.");
        }
        if (MOCK_VOICE.equals(selected)) {
            return AudioEngineDescriptor.mock();
        }
        return viewModel.audioEngineDescriptor();
    }

    private void refreshToneChoicesForSelectedVoice(VoiceEngineCapabilityProfile profile, VoiceProfile selectedVoice) {
        VoiceLibrary library = viewModel.activeVoiceLibraryProperty().get();
        VoiceReferenceTone previousTone = toneSelector.getValue();
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
                .map(sampleSet -> neutralFirst(sampleSet.registeredTones()))
                .orElse(List.of());
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
        if (profile.coquiXttsMode()) {
            if (usesDefaultEngineVoice(voice)
                    || usesAdvancedPredesignedNeutral(voice)
                    || OfficialAdvancedVoicePresetCatalog.isOfficialPreset(voice)) {
                return true;
            }
            return library != null
                    && voice.engineType() != VoiceEngineType.PIPER
                    && library.referenceSampleSetByVoiceId(voice.id()).filter(sampleSet -> sampleSet.hasNeutral()).isPresent();
        }
        return true;
    }

    private static String emptyVoiceNotice(VoiceEngineCapabilityProfile profile) {
        if (profile.coquiXttsMode()) {
            return "Registra una voz con muestra Neutral o restaura la biblioteca para recuperar el Narrador predeterminado. Documento solo muestra voces realmente usables.";
        }
        if (profile.piperMode()) {
            return "No hay una voz local simple disponible. Prepárala desde Configuración o Voces.";
        }
        if (profile.mockMode()) {
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
        if (profile.piperMode()) {
            return voice.engineType() == VoiceEngineType.PIPER
                    || voice.engineType() == VoiceEngineType.LOCAL_TTS_PROCESS
                    || voice.engineType() == VoiceEngineType.MOCK;
        }
        if (profile.coquiXttsMode()) {
            return voice.engineType() != VoiceEngineType.PIPER;
        }
        if (profile.mockMode()) {
            return voice.engineType() == VoiceEngineType.MOCK;
        }
        return true;
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
}
