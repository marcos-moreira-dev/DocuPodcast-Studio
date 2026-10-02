package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceLibraryCapabilityReport;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceProfileCapability;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceRegistrationWizardPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPrompt;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.SettingsDialog;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.detailStack;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.detachNode;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.balancedMasterDetail;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.masterDetail;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.moduleRoot;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.section;

/** Workspace for the user-facing voice library. It is a sober modular administration surface. */
public final class VoiceLibraryWorkspaceView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final ReadOnlyObjectProperty<VoiceLibrary> libraryProperty;
    private final ObjectProperty<VoiceModuleId> activeModule = new SimpleObjectProperty<>(VoiceModuleId.HOME);
    private final ObjectProperty<VoiceManageMode> manageMode = new SimpleObjectProperty<>(VoiceManageMode.LIST);
    private final ObjectProperty<VoiceProfile> managedVoiceSelection = new SimpleObjectProperty<>();
    private boolean refreshingVoiceSelectionControls;
    private final VoiceWorkspaceShell shell;
    private final VoiceSampleActions sampleActions;
    private final VoiceEngineSettingsControls engineSettings;
    private final ListView<VoiceProfile> voiceBrowser = StudioCollectionControls.listView();
    private final TextField voiceNameField = StudioFormControls.textField();
    private final VBox selectedVoiceDetails = new VBox(8);
    private final ListView<String> summaryList = StudioCollectionControls.listView();
    private final ComboBox<VoiceToneRecordingPrompt> tonePromptSelector = StudioFormControls.comboBox();
    private final Label tonePromptText = new Label("Selecciona una voz para ver la frase guía del tono.");
    private final Label toneRecordingContract = new Label("Cancelar no reemplaza muestras anteriores.");
    private final Label selectedSampleStatus = new Label("Sin muestra.");
    private final TextArea generatedTestText = StudioFormControls.textArea();
    private final Label generatedTestStatus = new Label();
    private final BooleanProperty voiceTestGenerating = new SimpleBooleanProperty(false);
    private final VoiceReadyToneMicroCard readyToneMicroCard = new VoiceReadyToneMicroCard();
    private final BooleanProperty samplePlaybackRunning = new SimpleBooleanProperty(false);
    private VoiceReferenceTone samplePlaybackTone = VoiceReferenceTone.NEUTRAL;

    public VoiceLibraryWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        this.libraryProperty = viewModel.activeVoiceLibraryProperty();
        this.sampleActions = new VoiceSampleActions(viewModel, new VoiceSampleActions.Context() {
            @Override
            public javafx.stage.Window ownerWindow() {
                return getScene() == null ? null : getScene().getWindow();
            }

            @Override
            public VoiceToneRecordingPlan currentToneRecordingPlan() {
                return VoiceLibraryWorkspaceView.this.currentToneRecordingPlan();
            }

            @Override
            public VoiceProfile selectedVoiceForSample() throws IOException {
                return manageMode.get() == VoiceManageMode.NEW_VOICE
                        ? ensureEditorVoiceDraft()
                        : selectedManagedVoice();
            }

            @Override
            public VoiceProfile selectedVoice() {
                return selectedManagedVoice();
            }

            @Override
            public VoiceReferenceTone selectedReferenceTone() {
                return VoiceLibraryWorkspaceView.this.selectedReferenceTone();
            }

            @Override
            public String selectedVoiceName() {
                return VoiceLibraryWorkspaceView.this.selectedVoiceName();
            }

            @Override
            public void refreshVoices() {
                VoiceLibraryWorkspaceView.this.refreshVoices(libraryProperty.get());
                updateSelectedSampleStatus(selectedManagedVoice());
                refreshReadyToneMicroCard(selectedManagedVoice());
            }

            @Override
            public void renderCurrentLibrary() {
                render(libraryProperty.get());
            }

            @Override
            public void showSummary(String... lines) {
                summaryList.getItems().setAll(lines);
            }

            @Override
            public void markSamplePlaybackStarted(VoiceReferenceTone tone) {
                samplePlaybackTone = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
                samplePlaybackRunning.set(true);
                updateSelectedSampleStatus(selectedManagedVoice());
                refreshReadyToneMicroCard(selectedManagedVoice());
            }

            @Override
            public void markSamplePlaybackFinished(VoiceReferenceTone tone) {
                VoiceReferenceTone normalized = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
                if (samplePlaybackTone == normalized) {
                    samplePlaybackRunning.set(false);
                }
                updateSelectedSampleStatus(selectedManagedVoice());
                refreshReadyToneMicroCard(selectedManagedVoice());
            }

            @Override
            public long selectedSampleDurationMillis(VoiceReferenceTone tone) {
                VoiceReferenceTone normalized = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
                return sampleSetFor(selectedManagedVoice())
                        .flatMap(set -> set.sampleFor(normalized))
                        .map(VoiceReferenceSample::durationMillis)
                        .orElse(1800L);
            }
        });
        this.engineSettings = new VoiceEngineSettingsControls(
                viewModel, lines -> summaryList.getItems().setAll(lines));
        getStyleClass().add("voice-library-workspace");
        setPadding(new Insets(10));

        configureVoiceBrowser();
        configureVoiceNameControl();
        configureTonePromptSelector();
        configureGeneratedTestControls();
        configureSampleStatus();

        VoiceModuleNavigation navigation = new VoiceModuleNavigation(moduleDescriptors(), activeModule);
        shell = new VoiceWorkspaceShell(navigation);
        setCenter(shell);

        libraryProperty.addListener((obs, oldValue, newValue) -> render(newValue));
        activeModule.addListener((obs, oldValue, newValue) -> {
            if (newValue != VoiceModuleId.MANAGE) {
                manageMode.set(VoiceManageMode.LIST);
            } else {
                VoiceManagedSelectionCoordinator.resetGeneratedTest(viewModel, generatedTestText, voiceBrowser.getSelectionModel().getSelectedItem());
            }
            render(libraryProperty.get());
        });
        manageMode.addListener((obs, oldValue, newValue) -> render(libraryProperty.get()));
        render(libraryProperty.get());
    }

    private static List<VoiceModuleDescriptor> moduleDescriptors() {
        return List.of(
                VoiceModuleDescriptor.of(VoiceModuleId.HOME, "OPERACIÓN"),
                VoiceModuleDescriptor.of(VoiceModuleId.ENGINE, "MOTOR"),
                VoiceModuleDescriptor.of(VoiceModuleId.MANAGE, "BIBLIOTECA")
        );
    }

    private void configureVoiceBrowser() {
        voiceBrowser.getStyleClass().add("voice-browser-list");
        voiceBrowser.setPlaceholder(new Label("No hay voces creadas todavía."));
        voiceBrowser.setCellFactory(list -> new ListCell<>() {
            { selectedProperty().addListener((obs, oldValue, newValue) -> renderItem()); }

            @Override
            protected void updateItem(VoiceProfile item, boolean empty) { super.updateItem(item, empty); renderItem(); }

            private void renderItem() {
                VoiceProfile item = getItem();
                if (isEmpty() || item == null) { setText(null); setGraphic(null); return; }
                setText(null);
                Button useVoice = isSelected() ? ActionButtonFactory.primary("Usar voz", () -> useVoiceForDocument(item)) : null;
                setGraphic(new VoiceListItemView(item, libraryProperty.get(), useVoice));
            }
        });
        voiceBrowser.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                if (!refreshingVoiceSelectionControls) {
                    managedVoiceSelection.set(newValue);
                    voiceNameField.setText(newValue.displayName());
                    if (activeModule.get() == VoiceModuleId.MANAGE) {
                        VoiceManagedSelectionCoordinator.resetGeneratedTest(viewModel, generatedTestText, newValue);
                    }
                }
            }
            renderSelectedVoice(newValue);
            voiceBrowser.refresh();
        });
    }

    private void useVoiceForDocument(VoiceProfile voice) {
        if (voice == null) { return; }
        managedVoiceSelection.set(voice); voiceBrowser.getSelectionModel().select(voice);
        viewModel.useVoiceForDocument(voice); renderSelectedVoice(voice); voiceBrowser.refresh();
    }

    private void configureVoiceNameControl() {
        voiceNameField.getStyleClass().add("voice-name-field");
        voiceNameField.setPromptText("Nombre de la voz, por ejemplo: Pepito");
    }

    private void configureTonePromptSelector() {
        tonePromptSelector.getStyleClass().add("voice-library-combo");
        tonePromptSelector.setPromptText("Elegir tono o emoción");
        tonePromptSelector.setVisibleRowCount(16);
        tonePromptText.getStyleClass().add("voice-interpretation-phrase");
        tonePromptSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(VoiceToneRecordingPrompt item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : toneLabel(item));
            }
        });
        tonePromptSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VoiceToneRecordingPrompt item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : toneLabel(item));
            }
        });
        tonePromptSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            updateTonePromptText(selectedManagedVoice());
            refreshReadyToneMicroCard(selectedManagedVoice());
        });
    }

    private void configureGeneratedTestControls() {
        generatedTestText.getStyleClass().add("voice-generated-test-text");
        generatedTestText.setWrapText(true);
        generatedTestText.setPrefRowCount(3);
        generatedTestText.setPromptText("Esta es una prueba de lectura con la voz seleccionada.");
        generatedTestStatus.getStyleClass().add("voice-generated-test-status");
        generatedTestStatus.setWrapText(true);
        generatedTestStatus.textProperty().bind(viewModel.generatedVoiceTestStatusProperty());
    }

    private void configureSampleStatus() {
        selectedSampleStatus.getStyleClass().add("voice-recording-state");
        selectedSampleStatus.setWrapText(true);
        viewModel.voiceRecordingRunningProperty().addListener((obs, oldValue, newValue) -> updateSelectedSampleStatus(selectedManagedVoice()));
    }

    private void render(VoiceLibrary library) {
        if (library == null) {
            shell.setModuleContent(emptyLibraryModule());
            return;
        }
        VoiceLibraryCapabilityReport report = viewModel.voiceCapabilityReport();
        refreshVoices(library);
        ArrayList<String> sideLines = new ArrayList<>(report.summaryLines());
        sideLines.addAll(viewModel.voiceLibraryValidationLabels());
        summaryList.getItems().setAll(sideLines);

        VBox module = switch (activeModule.get()) {
            case HOME -> homeModule(library, report);
            case ENGINE -> engineModule(report);
            case MANAGE -> manageModule(report);
        };
        shell.setModuleContent(module);
        renderSelectedVoice(voiceBrowser.getSelectionModel().getSelectedItem());
    }

    private VBox emptyLibraryModule() {
        VBox box = new VBox(8);
        box.getStyleClass().add("voice-module-empty");
        box.getChildren().add(new Label("Biblioteca de voces no disponible."));
        return box;
    }

    private VBox homeModule(VoiceLibrary library, VoiceLibraryCapabilityReport report) {
        VBox box = moduleRoot("Inicio", "Voces disponibles", "Consulta qué voces puede usar Documento. La voz simple no requiere muestras; la voz avanzada usa Neutral y tonos cuando existan.");
        box.getChildren().add(voiceLibraryHero(report));
        VBox list = voiceSelectionPanel("Voces creadas");
        list.getStyleClass().add("voice-home-selection-panel");
        detachNode(voiceBrowser);
        voiceBrowser.setPrefHeight(280);
        voiceBrowser.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(voiceBrowser, Priority.ALWAYS);
        Button edit = ActionButtonFactory.secondary("Gestionar voz", () -> activeModule.set(VoiceModuleId.MANAGE));
        edit.disableProperty().bind(Bindings.createBooleanBinding(
                this::homeManageVoiceDisabled,
                voiceBrowser.getSelectionModel().selectedItemProperty(),
                managedVoiceSelection));
        Button create = ActionButtonFactory.primary("Agregar voz", this::startNewVoiceDraft);
        VoiceActionStrip actions = VoiceActionStrip.of(create, edit);
        actions.getStyleClass().add("voice-module-actions");
        Label deleteNote = new Label("Eliminar una voz se hará desde Gestionar voces con confirmación y aviso de archivos de audio asociados.");
        deleteNote.setWrapText(true);
        deleteNote.getStyleClass().add("document-side-text");
        list.getChildren().addAll(voiceBrowser, actions, deleteNote);
        VBox detail = detailStack(selectedVoiceSection());
        box.getChildren().add(balancedMasterDetail(list, detail));
        return box;
    }

    private boolean homeManageVoiceDisabled() {
        VoiceProfile selected = selectedManagedVoice();
        return selected == null || VoiceProfilePresentationPolicy.predefinedVoice(selected);
    }

    private VBox engineModule(VoiceLibraryCapabilityReport report) {
        VBox box = moduleRoot("Configurar motor", "Motor activo", "Selecciona y verifica el motor de voz desde una superficie humana. La configuración interna compartida se sincroniza con Configuración.");
        engineSettings.refresh();
        VBox selection = engineSettings.selectionSection();
        Button openAdministration = ActionButtonFactory.secondary(
                "Motores y dependencias", this::openVoiceEngineSettings);
        selection.getChildren().add(openAdministration);
        VBox card = section("Prueba rápida del motor seleccionado");
        card.getStyleClass().add("voice-engine-test-card");
        if (generatedTestText.getText() == null || generatedTestText.getText().isBlank()) {
            generatedTestText.setText(localSimpleMode()
                    ? "Esta es una prueba breve de lectura local simple."
                    : "Esta es una prueba breve para comprobar la voz activa.");
        }
        Button test = generateVoiceTestButton("Generar voz de prueba");
        Button play = playGeneratedVoiceTestButton();
        detachNode(generatedTestText);
        detachNode(generatedTestStatus);
        VoiceGeneratedTestPanel testPanel = new VoiceGeneratedTestPanel("Voz de prueba",
                "Motor y dispositivo seleccionados",
                "Genera un WAV corto con el motor y dispositivo de la columna izquierda. Si elegiste GPU, se intentará esa GPU y el diagnóstico indicará si el runtime la rechaza.",
                generatedTestText, generatedTestStatus, test, play);
        card.getChildren().add(testPanel);
        VBox detail = detailStack(card);
        box.getChildren().add(masterDetail(selection, detail));
        return box;
    }

    private VBox manageModule(VoiceLibraryCapabilityReport report) {
        if (manageMode.get() == VoiceManageMode.NEW_VOICE) {
            return voiceProfileSampleEditorModule(false);
        }
        if (manageMode.get() == VoiceManageMode.EDIT_VOICE) {
            VoiceProfile selected = selectedManagedVoice();
            if (selected == null
                    || VoiceProfilePresentationPolicy.simpleVoice(selected)
                    || VoiceProfilePresentationPolicy.predefinedVoice(selected)) {
                manageMode.set(VoiceManageMode.LIST);
            } else {
                return voiceProfileSampleEditorModule(true);
            }
        }
        VBox box = moduleRoot("Gestionar voces", "Muestras de voz por emoción", "Crea, revisa y reemplaza muestras por emoción. Las filas se mantienen sobrias: nombre, estado y acciones justas.");
        VBox master = voiceManagementHeader();
        VBox detail = detailStack(manageOverviewSection());
        box.getChildren().add(masterDetail(master, detail));
        return box;
    }

    private VBox manageOverviewSection() {
        VoiceProfile voice = voiceBrowser.getSelectionModel().getSelectedItem();
        Button newVoice = ActionButtonFactory.primary("Nueva voz", this::startNewVoiceDraft);
        Button manageVoice = ActionButtonFactory.primary("Gestionar voz seleccionada", this::startManageSelectedVoice);
        VoiceProfileCapability capability = voice == null ? null : viewModel.voiceCapabilityReport().voice(voice.id()).orElse(null);
        List<VoiceToneRecordingPrompt> registeredPrompts = voice == null ? List.of() : updateTonePromptControlsForRegisteredSamples(voice);
        return new VoiceManageOverviewPanel(voice,
                capability == null ? "Revisa el motor activo antes de usar esta voz." : documentUsageHint(capability),
                registeredPrompts,
                tonePromptSelector,
                generatedTestText,
                generatedTestStatus,
                generateVoiceTestButton("Generar voz"),
                playGeneratedVoiceTestButton(),
                newVoice,
                manageVoice);
    }

    private VBox voiceProfileSampleEditorModule(boolean editingExisting) {
        VoiceProfile voice = selectedManagedVoice();
        if (editingExisting
                && (VoiceProfilePresentationPolicy.simpleVoice(voice)
                || VoiceProfilePresentationPolicy.predefinedVoice(voice))) {
            backToManageVoices();
            return manageModule(viewModel.voiceCapabilityReport());
        }
        updateTonePromptControls(voice);
        detachNode(voiceNameField);
        voiceNameField.setMaxWidth(Double.MAX_VALUE);
        detachNode(tonePromptSelector);
        detachNode(tonePromptText);
        detachNode(toneRecordingContract);
        detachNode(selectedSampleStatus);
        detachNode(readyToneMicroCard);
        tonePromptSelector.setMaxWidth(Double.MAX_VALUE);
        updateSelectedSampleStatus(voice);
        refreshReadyToneMicroCard(voice);
        Button delete = editingExisting ? ActionButtonFactory.secondary("Eliminar voz", this::deleteSelectedVoiceProfile) : null;
        return new VoiceProfileSampleEditorPanel(
                editingExisting,
                voiceNameField, tonePromptSelector, tonePromptText, toneRecordingContract, selectedSampleStatus,
                voiceSampleActionGroups(false),
                readyToneMicroCard,
                ActionButtonFactory.secondary("Volver a gestionar voces", this::backToManageVoices),
                ActionButtonFactory.primary("Guardar / actualizar voz", this::saveVoiceFromEditor),
                delete,
                ActionButtonFactory.secondary("Exportar muestras", this::exportSelectedVoiceSamples));
    }

    private void backToManageVoices() {
        manageMode.set(VoiceManageMode.LIST);
        activeModule.set(VoiceModuleId.MANAGE);
    }

    private VoiceProfile selectedManagedVoice() {
        VoiceProfile fromList = voiceBrowser.getSelectionModel().getSelectedItem();
        return fromList == null ? managedVoiceSelection.get() : fromList;
    }

    private VoiceProfile ensureEditorVoiceDraft() throws IOException {
        VoiceProfile selected = selectedManagedVoice();
        if (selected != null) {
            return selected;
        }
        String name = voiceNameField.getText();
        if (name == null || name.isBlank()) {
            throw new IOException("Escribe el nombre de la voz antes de grabar o importar una muestra.");
        }
        VoiceProfile saved = viewModel.saveAdvancedVoiceProfile("", name);
        refreshVoices(libraryProperty.get());
        managedVoiceSelection.set(saved);
        voiceBrowser.getSelectionModel().select(saved);
        return saved;
    }

    private void saveVoiceFromEditor() {
        VoiceProfile selected = selectedManagedVoice();
        if (selected == null) {
            try {
                selected = ensureEditorVoiceDraft();
            } catch (IOException | RuntimeException ex) {
                summaryList.getItems().setAll("No se pudo guardar la voz: " + ex.getMessage());
                return;
            }
        } else {
            try {
                selected = viewModel.saveAdvancedVoiceProfile(selected.id(), voiceNameField.getText());
                refreshVoices(libraryProperty.get());
                managedVoiceSelection.set(selected);
                voiceBrowser.getSelectionModel().select(selected);
            } catch (IOException | RuntimeException ex) {
                summaryList.getItems().setAll("No se pudo guardar la voz: " + ex.getMessage());
                return;
            }
        }
        boolean hasNeutral = sampleSetFor(selected).map(VoiceReferenceSampleSet::hasNeutral).orElse(false);
        if (!hasNeutral) {
            summaryList.getItems().setAll("Para registrar una voz avanzada necesitas al menos una muestra Neutral.");
            updateSelectedSampleStatus(selected);
            return;
        }
        summaryList.getItems().setAll("Voz lista: " + selected.displayName() + ". Documento ya puede usar las emociones registradas para esta voz.");
        updateSelectedSampleStatus(selected);
    }

    private VBox voiceManagementHeader() {
        return new VoiceManagementHeaderPanel(voiceBrowser);
    }

    private void startNewVoiceDraft() {
        manageMode.set(VoiceManageMode.NEW_VOICE);
        activeModule.set(VoiceModuleId.MANAGE);
        managedVoiceSelection.set(null);
        voiceBrowser.getSelectionModel().clearSelection();
        voiceNameField.setText("");
        updateTonePromptControls(null);
        generatedTestText.setText("Esta es una prueba de lectura con la nueva voz.");
        summaryList.getItems().setAll("Nueva voz: escribe un nombre y registra Neutral. Las demás emociones son opcionales.");
        renderSelectedVoice(null);
    }

    private void startManageSelectedVoice() {
        VoiceProfile selected = selectedManagedVoice();
        if (selected == null) {
            summaryList.getItems().setAll("Selecciona una voz antes de abrir su gestión completa.");
            return;
        }
        if (VoiceProfilePresentationPolicy.simpleVoice(selected)) {
            summaryList.getItems().setAll("La voz simple no usa muestras ni emociones.");
            return;
        }
        if (VoiceProfilePresentationPolicy.predefinedVoice(selected)) {
            summaryList.getItems().setAll("Las voces prediseñadas no se editan. Crea una voz nueva para registrar tus muestras.");
            return;
        }
        managedVoiceSelection.set(selected);
        voiceNameField.setText(selected.displayName());
        updateTonePromptControls(selected);
        manageMode.set(VoiceManageMode.EDIT_VOICE);
        activeModule.set(VoiceModuleId.MANAGE);
    }

    private void deleteSelectedVoiceProfile() {
        VoiceProfile selected = selectedManagedVoice();
        if (selected == null) {
            summaryList.getItems().setAll("Selecciona una voz antes de eliminarla.");
            return;
        }
        Alert confirm = NativeDialogResponse.alert(Alert.AlertType.CONFIRMATION);
        StudioMessageDialog.configure(
                confirm,
                getScene() == null ? null : getScene().getWindow(),
                "Eliminar voz",
                "Eliminar voz \"" + selected.displayName() + "\"",
                viewModel.deleteVoiceProfileImpactLabel(selected)
                        + "\n\nTambién se retirarán sus emociones registradas y los archivos de audio "
                        + "gestionados por el proyecto. Los fragmentos que usen esta voz deberán "
                        + "volver a asignarse.",
                "");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            summaryList.getItems().setAll("Eliminación cancelada. La voz se conserva.");
            return;
        }
        try {
            viewModel.deleteVoiceProfile(selected);
            summaryList.getItems().setAll("Voz eliminada: " + selected.displayName() + ".");
            refreshVoices(libraryProperty.get());
            managedVoiceSelection.set(null);
            voiceNameField.clear();
        } catch (IOException | RuntimeException ex) {
            summaryList.getItems().setAll("No se pudo eliminar la voz: " + ex.getMessage());
        }
    }

    private void exportSelectedVoiceSamples() {
        VoiceProfile selected = selectedManagedVoice();
        if (selected == null) {
            summaryList.getItems().setAll("Selecciona una voz antes de exportar sus muestras.");
            return;
        }
        DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
        chooser.setTitle("Exportar muestras de " + selected.displayName());
        File target = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) {
            summaryList.getItems().setAll("Exportación cancelada.");
            return;
        }
        List<VoiceReferenceSample> samples = sampleSetFor(selected)
                .map(VoiceReferenceSampleSet::samples)
                .orElse(List.of());
        if (samples.isEmpty()) {
            summaryList.getItems().setAll("La voz " + selected.displayName() + " no tiene muestras para exportar.");
            return;
        }
        int exported = 0;
        for (VoiceReferenceSample sample : samples) {
            try {
                viewModel.downloadVoiceReferenceSample(selected, sample.tone(), target.toPath());
                exported++;
            } catch (IOException | RuntimeException ex) {
                summaryList.getItems().setAll("Se exportaron " + exported + " muestra(s), pero falló " + sample.tone().displayName() + ": " + ex.getMessage());
                return;
            }
        }
        summaryList.getItems().setAll("Muestras exportadas de " + selected.displayName() + ": " + exported + ".");
    }

    private VBox voiceLibraryHero(VoiceLibraryCapabilityReport report) {
        VBox hero = new VBox(8);
        hero.getStyleClass().add("voice-library-hero");
        Label title = new Label("Biblioteca de voces");
        title.getStyleClass().add("voice-library-title");
        Label summary = new Label("Biblioteca operativa para lectura simple y voces avanzadas. Documento usa voces listas; Voces gestiona muestras cuando el motor lo permite.");
        summary.setWrapText(true);
        summary.getStyleClass().add("voice-library-summary");
        HBox chips = new HBox(8);
        Label readiness = new Label(report.readinessLabel());
        readiness.getStyleClass().add("voice-capability-chip");
        readiness.getStyleClass().add(report.synthesizableVoices() > 0 ? "voice-capability-ready" : "voice-capability-roadmap");
        Label assignment = new Label("Documento usa voces listas");
        assignment.getStyleClass().add("voice-capability-chip");
        assignment.getStyleClass().add("voice-capability-reference");
        chips.getChildren().addAll(readiness, assignment);
        hero.getChildren().addAll(title, summary, chips);
        return hero;
    }

    private VBox voiceSampleActionGroups(boolean includeExport) {
        return sampleActions.actionGroups(includeExport);
    }

    private static VBox voiceSelectionPanel(String title) {
        VBox box = section(title);
        box.getStyleClass().add("voice-selection-panel");
        return box;
    }

    private VBox selectedVoiceSection() {
        VBox box = section("Detalle de voz seleccionada");
        detachNode(selectedVoiceDetails);
        selectedVoiceDetails.getStyleClass().add("voice-selected-detail");
        box.getChildren().add(selectedVoiceDetails);
        return box;
    }

    private void renderSelectedVoice(VoiceProfile voice) {
        selectedVoiceDetails.getChildren().clear();
        if (activeModule.get() != VoiceModuleId.MANAGE || manageMode.get() != VoiceManageMode.LIST) {
            updateTonePromptControls(voice);
        }
        if (voice == null) {
            Label empty = new Label("Selecciona una voz de la lista para revisar sus muestras, emociones y uso recomendado.");
            empty.setWrapText(true);
            empty.getStyleClass().add("document-side-text");
            selectedVoiceDetails.getChildren().add(empty);
            return;
        }
        VoiceProfileCapability capability = viewModel.voiceCapabilityReport().voice(voice.id()).orElse(null);
        Label name = new Label(VoiceProfilePresentationPolicy.displayName(voice));
        name.getStyleClass().add("voice-selected-title");
        Label humanStatus = new Label(VoiceProfilePresentationPolicy.typeLabel(voice) + " · " + voice.language().toUpperCase(java.util.Locale.ROOT) + " · " + voice.qualityPreset().displayName());
        humanStatus.getStyleClass().add("voice-selected-subtitle");
        humanStatus.setWrapText(true);
        HBox chips = new HBox(6);
        Label capabilityChip = new Label(capability == null ? "Sin capacidad" : capability.status());
        capabilityChip.getStyleClass().add("voice-capability-chip");
        capabilityChip.getStyleClass().add(capability == null ? "voice-capability-blocked" : capability.statusCssClass());
        boolean hasNeutralSample = VoiceProfilePresentationPolicy.hasNeutralReference(voice, libraryProperty.get());
        Label sampleChip = new Label(VoiceProfilePresentationPolicy.sampleLabel(voice, libraryProperty.get()));
        sampleChip.getStyleClass().add("voice-capability-chip");
        sampleChip.getStyleClass().add(voice.hasSample() || hasNeutralSample || VoiceProfilePresentationPolicy.simpleVoice(voice) ? "voice-capability-reference" : "voice-capability-roadmap");
        chips.getChildren().addAll(capabilityChip, sampleChip);
        Label description = new Label(capability == null ? voice.consentNote() : capability.message());
        description.setWrapText(true);
        description.getStyleClass().add("voice-library-body");
        String usageNote = hasNeutralSample && !VoiceProfilePresentationPolicy.simpleVoice(voice)
                ? (VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice)
                ? "Neutral prediseñada disponible. Las emociones adicionales son opcionales y se agregan desde Gestionar voces."
                : "Neutral registrada. Las emociones adicionales son opcionales y se agregan desde Gestionar voces.")
                : capability == null ? "Revisa el estado del motor antes de usar esta voz." : documentUsageHint(capability);
        Label note = new Label(usageNote);
        note.setWrapText(true);
        note.getStyleClass().add("document-side-text");
        if (VoiceProfilePresentationPolicy.simpleVoice(voice)) {
            Label simple = new Label("Voz simple lista para lectura neutral. No usa muestras humanas, emociones ni clonación.");
            simple.setWrapText(true);
            simple.getStyleClass().add("voice-library-body");
            Label simpleUse = new Label("No hay pasos de preparación para esta voz desde Inicio. Úsala en Documento o cambia a Voz IA avanzada si necesitas muestras y tonos.");
            simpleUse.setWrapText(true);
            simpleUse.getStyleClass().add("document-side-text");
            selectedVoiceDetails.getChildren().addAll(name, humanStatus, chips, description, simple, simpleUse);
            return;
        }
        Label registered = new Label(registeredTonesLabel(voice));
        registered.setWrapText(true);
        registered.getStyleClass().add("voice-library-body");
        selectedVoiceDetails.getChildren().addAll(name, humanStatus, chips, description, registered, referenceSampleMatrix(voice), note);
    }

    private static String documentUsageHint(VoiceProfileCapability capability) {
        if (capability == null) {
            return "Revisa el estado del motor antes de usar esta voz.";
        }
        if (capability.synthesizableNow()) {
            return "Lista para Documento: puede sintetizarse con el motor activo.";
        }
        if (capability.referenceReady()) {
            return "Referencia registrada: verifica el motor activo antes de usarla en Documento.";
        }
        if (capability.requiresSample()) {
            return "Falta Neutral o una muestra válida antes de que aparezca como voz lista para Documento.";
        }
        if (capability.requiresEngineConfiguration()) {
            return "Falta preparar el motor activo desde Configurar motor o Configuración.";
        }
        return capability.message();
    }

    private Button generateVoiceTestButton(String readyLabel) {
        Button button = ActionButtonFactory.primary(readyLabel, this::startGenerateVoiceTest);
        button.textProperty().bind(Bindings.when(voiceTestGenerating).then("Generando voz...").otherwise(readyLabel));
        button.disableProperty().bind(voiceTestGenerating);
        return button;
    }

    private Button playGeneratedVoiceTestButton() {
        Button button = ActionButtonFactory.secondary("Escuchar voz de prueba", "Reproducir voz de prueba generada", this::playGeneratedVoiceTest);
        button.disableProperty().bind(Bindings.or(Bindings.isNull(viewModel.lastGeneratedVoiceTestPathProperty()), voiceTestGenerating));
        return button;
    }

    private void startGenerateVoiceTest() {
        if (voiceTestGenerating.get()) { return; }
        VoiceProfile voice = voiceBrowser.getSelectionModel().getSelectedItem();
        try {
            var job = viewModel.prepareVoiceTestGeneration(voice, selectedReferenceTone(), generatedTestText.getText());
            viewModel.markVoiceTestGenerationStarted();
            voiceTestGenerating.set(true);
            Task<com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceGeneratedTestResult> task = new Task<>() {
                @Override protected com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceGeneratedTestResult call() throws Exception { return job.call(); }
            };
            task.setOnSucceeded(event -> {
                var result = task.getValue();
                viewModel.completeVoiceTestGeneration(result);
                summaryList.getItems().setAll(result.userMessage());
                voiceTestGenerating.set(false);
            });
            task.setOnFailed(event -> failVoiceTestGeneration(task.getException()));
            Thread worker = new Thread(task, "docupodcast-voice-test");
            worker.setDaemon(true);
            worker.start();
        } catch (IOException | RuntimeException ex) {
            failVoiceTestGeneration(ex);
        }
    }

    private void failVoiceTestGeneration(Throwable ex) {
        String detail = ex == null ? "" : ex.getMessage();
        viewModel.markVoiceTestGenerationFailed(detail);
        summaryList.getItems().setAll("No se pudo generar la prueba de voz: " + detail);
        voiceTestGenerating.set(false);
    }

    private void playGeneratedVoiceTest() {
        try {
            viewModel.playLastGeneratedVoiceTest();
        } catch (IOException | RuntimeException ex) {
            summaryList.getItems().setAll("No se pudo reproducir la prueba generada: " + ex.getMessage());
        }
    }

    private void refreshVoices(VoiceLibrary library) {
        VoiceProfile selectedVoice = voiceBrowser.getSelectionModel().getSelectedItem();
        refreshingVoiceSelectionControls = true;
        try {
            voiceBrowser.getItems().setAll(library.voices());
            if (manageMode.get() == VoiceManageMode.NEW_VOICE && managedVoiceSelection.get() == null) {
                voiceBrowser.getSelectionModel().clearSelection();
                voiceNameField.clear();
                return;
            }
            if (selectedVoice != null) {
                library.voiceById(selectedVoice.id()).ifPresentOrElse(voice -> {
                            voiceBrowser.getSelectionModel().select(voice);
                            managedVoiceSelection.set(voice);
                            voiceNameField.setText(voice.displayName());
                        },
                        () -> selectFirstVoice(library));
            } else {
                selectFirstVoice(library);
            }
        } finally {
            refreshingVoiceSelectionControls = false;
        }
    }

    private void selectFirstVoice(VoiceLibrary library) {
        if (!library.voices().isEmpty()) {
            VoiceProfile first = library.voices().get(0);
            voiceBrowser.getSelectionModel().select(first);
            managedVoiceSelection.set(first);
            voiceNameField.setText(first.displayName());
        }
    }

    private void updateTonePromptControls(VoiceProfile voice) {
        VoiceRegistrationWizardPlan wizard = viewModel.voiceRegistrationWizardPlan(voice);
        VoiceReferenceTone previousTone = selectedReferenceTone();
        List<VoiceToneRecordingPrompt> prompts = new ArrayList<>();
        prompts.add(wizard.neutralPrompt());
        prompts.addAll(wizard.recommendedPrompts());
        prompts.addAll(wizard.theatricalPrompts());
        tonePromptSelector.getItems().setAll(prompts);
        VoiceToneRecordingPrompt selected = prompts.stream()
                .filter(prompt -> prompt.tone() == previousTone)
                .findFirst()
                .orElse(wizard.neutralPrompt());
        tonePromptSelector.getSelectionModel().select(selected);
        updateTonePromptText(voice);
    }

    private List<VoiceToneRecordingPrompt> updateTonePromptControlsForRegisteredSamples(VoiceProfile voice) {
        List<VoiceToneRecordingPrompt> prompts = VoiceRegisteredTonePrompts.forVoice(voice, libraryProperty.get());
        tonePromptSelector.getItems().setAll(prompts);
        if (!prompts.isEmpty()) {
            VoiceReferenceTone previousTone = selectedReferenceTone();
            VoiceToneRecordingPrompt selected = prompts.stream()
                    .filter(prompt -> prompt.tone() == previousTone)
                    .findFirst()
                    .orElse(prompts.get(0));
            tonePromptSelector.getSelectionModel().select(selected);
        }
        updateTonePromptText(voice);
        return prompts;
    }

    private void updateTonePromptText(VoiceProfile voice) {
        VoiceToneRecordingPrompt prompt = tonePromptSelector.getSelectionModel().getSelectedItem();
        if (prompt == null) {
            tonePromptText.setText("Selecciona un tono para ver la frase guía.");
            toneRecordingContract.setText("Cancelar no reemplaza muestras anteriores.");
            updateSelectedSampleStatus(voice);
            return;
        }
        VoiceToneRecordingPlan plan = viewModel.voiceToneRecordingPlan(voice, prompt.tone());
        tonePromptText.setText(plan.promptText());
        toneRecordingContract.setText((prompt.required() ? "Neutral necesaria" : "Tono")
                + " · " + plan.startLabel() + " / " + plan.stopLabel() + " / " + plan.saveLabel()
                + " · " + (plan.cancelKeepsPreviousSample() ? "Cancelar conserva la muestra anterior." : "Cancelar puede descartar cambios."));
        updateSelectedSampleStatus(voice);
    }

    private void updateSelectedSampleStatus(VoiceProfile voice) {
        VoiceReferenceTone tone = selectedReferenceTone();
        if (viewModel.voiceRecordingRunningProperty().get()) {
            selectedSampleStatus.setText("Grabando muestra " + tone.displayName() + ".");
            return;
        }
        if (samplePlaybackRunning.get() && samplePlaybackTone == tone) {
            selectedSampleStatus.setText("Reproduciendo muestra " + tone.displayName() + ".");
            return;
        }
        boolean exactSample = sampleSetFor(voice)
                .flatMap(set -> set.sampleFor(tone))
                .isPresent();
        if (exactSample) {
            selectedSampleStatus.setText("Muestra lista: " + tone.displayName() + ".");
            return;
        }
        if (VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice) && tone == VoiceReferenceTone.NEUTRAL) {
            selectedSampleStatus.setText("Neutral prediseñada disponible. Puedes reemplazarla con una muestra propia.");
            return;
        }
        selectedSampleStatus.setText("Sin muestra para " + tone.displayName() + ".");
    }

    private void refreshReadyToneMicroCard(VoiceProfile voice) {
        readyToneMicroCard.refresh(voice, sampleSetFor(voice), selectedReferenceTone(), this::selectTonePrompt);
    }

    private void selectTonePrompt(VoiceReferenceTone tone) {
        VoiceToneRecordingPrompt prompt = tonePromptSelector.getItems().stream()
                .filter(item -> item.tone() == tone)
                .findFirst()
                .orElse(null);
        if (prompt != null) {
            tonePromptSelector.getSelectionModel().select(prompt);
        }
        updateTonePromptText(selectedManagedVoice());
        refreshReadyToneMicroCard(selectedManagedVoice());
    }

    private VoiceToneRecordingPlan currentToneRecordingPlan() {
        return viewModel.voiceToneRecordingPlan(selectedManagedVoice(), selectedReferenceTone());
    }

    private VoiceReferenceTone selectedReferenceTone() {
        VoiceToneRecordingPrompt prompt = tonePromptSelector.getSelectionModel().getSelectedItem();
        return prompt == null ? VoiceReferenceTone.NEUTRAL : prompt.tone();
    }

    private String selectedVoiceName() {
        VoiceProfile voice = selectedManagedVoice();
        if (voice != null) {
            return VoiceProfilePresentationPolicy.displayName(voice);
        }
        String name = voiceNameField.getText();
        return name == null || name.isBlank() ? "la voz seleccionada" : name;
    }

    private VoiceEngineCapabilityProfile activeEngineProfile() {
        return viewModel.administrationWorkspace().voice().voiceCapabilityPolicy().activeEngineProfile(viewModel.audioEngineDescriptor());
    }

    private boolean localSimpleMode() {
        return activeEngineProfile().simpleLocalMode();
    }

    private Optional<VoiceReferenceSampleSet> sampleSetFor(VoiceProfile voice) {
        VoiceLibrary library = libraryProperty.get();
        if (library == null || voice == null) {
            return Optional.empty();
        }
        return library.referenceSampleSetByVoiceId(voice.id());
    }

    private String registeredTonesLabel(VoiceProfile voice) {
        if (VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice) && registeredToneCount(voice) == 0) {
            return "Neutral prediseñada disponible. Sin emociones adicionales registradas.";
        }
        return sampleSetFor(voice)
                .map(set -> {
                    if (set.samples().isEmpty()) {
                        return "Sin tonos registrados.";
                    }
                    String tones = set.samples().stream()
                            .map(sample -> sample.tone().displayName())
                            .distinct()
                            .collect(java.util.stream.Collectors.joining(", "));
                    return set.samples().size() + " muestra(s) registradas: " + tones + ".";
                })
                .orElse("Sin tonos registrados.");
    }

    private int registeredToneCount(VoiceProfile voice) {
        return sampleSetFor(voice)
                .map(set -> set.samples().size())
                .orElse(0);
    }

    private VBox referenceSampleMatrix(VoiceProfile voice) {
        VBox matrix = new VBox(7);
        matrix.getStyleClass().add("voice-sample-grid");
        Label title = new Label("Tonos registrados");
        title.getStyleClass().add("voice-sample-grid-title");
        matrix.getChildren().add(title);
        List<VoiceReferenceSample> samples = sampleSetFor(voice)
                .map(VoiceReferenceSampleSet::samples)
                .orElse(List.of())
                .stream()
                .sorted(java.util.Comparator.comparingInt(sample -> sample.tone().ordinal()))
                .toList();
        if (samples.isEmpty()) {
            String emptyText = VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice)
                    ? "Neutral prediseñada disponible. Agrega emociones opcionales solo si las necesitas."
                    : "Todavía no hay tonos registrados. Registra primero Neutral y luego los tonos que quieras usar.";
            Label empty = new Label(emptyText);
            empty.setWrapText(true);
            empty.getStyleClass().add("voice-sample-empty");
            matrix.getChildren().add(empty);
            return matrix;
        }
        for (VoiceReferenceSample sample : samples) {
            matrix.getChildren().add(referenceSampleRow(sample));
        }
        return matrix;
    }

    private static HBox referenceSampleRow(VoiceReferenceSample sample) {
        return new VoiceSampleRow(sample);
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }

    private VBox ethicsSection() {
        VBox box = section("Uso responsable");
        Label text = new Label("Registra únicamente voces propias o autorizadas. Las muestras son referencias para generar texto nuevo con la voz y el tono elegidos; no clips fijos para repetir siempre igual.");
        text.setWrapText(true);
        text.getStyleClass().add("voice-library-body");
        box.getChildren().add(text);
        return box;
    }

    private void openVoiceEngineSettings() {
        new SettingsDialog(viewModel.administrationWorkspace().capabilities()).showVoiceEngines(
                getScene() == null ? null : getScene().getWindow(), viewModel.administrationWorkspace().settings());
        engineSettings.refresh();
        summaryList.getItems().setAll("Configuración cerrada. Vuelve a verificar o actualizar la biblioteca si cambiaste el motor de voz.");
    }

    private static String toneLabel(VoiceToneRecordingPrompt prompt) {
        if (prompt == null) {
            return "Tono";
        }
        return VoiceToneLabelPolicy.comboLabel(prompt.tone());
    }

    private enum VoiceManageMode { LIST, NEW_VOICE, EDIT_VOICE }

}
