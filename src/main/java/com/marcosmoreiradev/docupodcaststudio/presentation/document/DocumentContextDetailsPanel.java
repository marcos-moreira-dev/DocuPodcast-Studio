package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfAnalysisVisualEvidence;
import com.marcosmoreiradev.docupodcaststudio.application.document.ApplyManualWordSemanticDescriptionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDerivedTreatmentGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfRegionContext;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfProcessingCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioAccordion;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningLanguage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RailActionRow;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.TheatreChoralVoiceWorkflow;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.SettingsDialog;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.concurrent.Task;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contextual inspector module for the selected document sentence or block. */
public final class DocumentContextDetailsPanel extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final VBox choralVoiceSection = new VBox(8);
    private final VBox choralVoiceOptions = new VBox(6);
    private final CheckBox allCharacters = StudioFormControls.checkBox("Todos los personajes (sin narrador)");
    private final Label choralVoiceStatus = new Label("Selecciona una intervencion teatral.");
    private final ProgressBar choralVoiceProgress = StudioFeedbackControls.progressBar(0.0);
    private final Button renderChoralVoice = ActionButtonFactory.primary("Renderizar voz multipersona", this::renderChoralVoice);
    private final Button restoreSimpleVoice = ActionButtonFactory.danger("Restaurar voz simple", this::restoreSimpleVoice);
    private final Map<CheckBox, TheatreChoralVoiceWorkflow.Option> choralChecks = new LinkedHashMap<>();
    private final VBox pdfAnalysisSection = new VBox(7);
    private final Label pdfAnalysisStatus = new Label("Selecciona una región PDF.");
    private final ProgressBar pdfAnalysisProgress = StudioFeedbackControls.progressBar(0.0);
    private final TextArea pdfAnalysisResult = StudioFormControls.textArea("");
    private final Button describePdfRegion = ActionButtonFactory.primary(
            "Generar interpretación visual", () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.IMAGE_DESCRIPTION));
    private final Button readPdfFormula = ActionButtonFactory.primary(
            "Generar narración matemática", () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.MATHEMATICAL_READING));
    private final Button preparePdfTable = ActionButtonFactory.primary(
            "Generar interpretación breve", () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.TABLE_NARRATION));
    private final Button recognizePdfTableStructure = ActionButtonFactory.secondary(
            "Reconocer estructura avanzada de tabla",
            () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.TABLE_STRUCTURE, true, false,
                    com.marcosmoreiradev.docupodcaststudio.application.document
                            .TransversalPpTableStructurePdfTreatmentEngine.ID));
    private final Button editPdfTableCells = ActionButtonFactory.secondary(
            "Corregir texto o celdas…", this::editPdfTableCells);
    private final Button selectPdfTableRange = ActionButtonFactory.secondary(
            "Elegir filas o columnas…", this::selectPdfTableRange);
    private final Button explainPdfTable = ActionButtonFactory.secondary(
            "Generar resumen contextual con IA local",
            () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.TABLE_NARRATION, true, false,
                    com.marcosmoreiradev.docupodcaststudio.application.document
                            .TransversalTableExplanationPdfTreatmentEngine.ID));
    private final Button correctPdfRegion = ActionButtonFactory.secondary(
            "Corregir texto reconocido", () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.CONTEXTUAL_CORRECTION));
    private final Button reviewPageNarratability = ActionButtonFactory.secondary(
            "Revisar galimatías de esta página", () -> generatePdfTreatment(
                    PdfDerivedTreatmentKind.NARRATABILITY_REVIEW));
    private final Button approvePdfTreatment = ActionButtonFactory.primary(
            "Aprobar para narración", () -> reviewPdfTreatment(PdfDerivedTreatmentState.APPROVED));
    private final Button rejectPdfTreatment = ActionButtonFactory.danger(
            "Rechazar propuesta", () -> reviewPdfTreatment(PdfDerivedTreatmentState.REJECTED));
    private final Button reviewPageDrafts = ActionButtonFactory.secondary(
            "Revisar borradores de esta página…", () -> reviewDraftBatch(true));
    private final Button reviewDocumentDrafts = ActionButtonFactory.secondary(
            "Revisar todos los borradores…", () -> reviewDraftBatch(false));
    private final Button copyPdfTreatmentDetails = ActionButtonFactory.secondary(
            "Copiar detalles técnicos", this::copyPdfTreatmentDetails);
    private final Button defineManualDescription = ActionButtonFactory.secondary(
            "Definir descripción manualmente…", this::defineManualDescription);
    private PdfDerivedTreatment activePdfTreatment;
    private int activePdfTreatmentPageNumber = -1;
    private String editedPdfTableText = "";
    private String selectedPdfTableRows = "";
    private String selectedPdfTableColumns = "";
    private final Button regeneratePdfTreatment = ActionButtonFactory.secondary(
            "Regenerar propuesta", () -> {
                if (activePdfTreatment != null) {
                    generatePdfTreatment(activePdfTreatment.kind(), true, true);
                }
            });
    private final Button openDocumentAiSettings = ActionButtonFactory.secondary(
            "Abrir Motores y dependencias", this::openDocumentAiSettings);
    private final ComboBox<SecondarySemanticReadingPolicy> semanticReadingPolicy =
            StudioFormControls.comboBox(FXCollections.observableArrayList(
                    SecondarySemanticReadingPolicy.values()));
    private final Label semanticReadingPolicyNotice = new Label();
    private final CheckBox readCompleteWordTables = StudioFormControls.checkBox(
            "Leer todas las celdas de las tablas Word");
    private final Label wordTableReadingNotice = new Label(
            "Procesamiento determinístico; solo Word.");
    private final VBox semanticReadingSettings = new VBox(6,
            new SectionHeader("Lectura de componentes semánticos secundarios", ""),
            semanticReadingPolicy,
            semanticReadingPolicyNotice,
            readCompleteWordTables,
            wordTableReadingNotice);
    private final TitledPane pdfAdvancedReview = StudioAccordion.pane(
            "Revisión avanzada", pdfAnalysisSection);
    private final VBox visualReviewActions = new VBox(6);
    private final VBox mathReviewActions = new VBox(6);
    private final VBox tableReviewActions = new VBox(6);
    private final VBox textReviewActions = new VBox(6);
    private final VBox reviewResultActions = new VBox(6);
    private final VBox batchReviewActions = new VBox(6);
    private final VBox uncertainReviewActions = new VBox(6);
    private final Button keepUncertainAsNarratable = ActionButtonFactory.primary(
            "Conservar como texto narrable",
            () -> applySelectedPdfNarratability(PdfNarratability.NARRATABLE));
    private final Button omitUncertainFromReading = ActionButtonFactory.secondary(
            "Omitir de la lectura",
            () -> applySelectedPdfNarratability(PdfNarratability.NON_NARRATABLE));
    private final Button analyzeUncertainLocally = ActionButtonFactory.secondary(
            "Pedir una propuesta a la IA local",
            () -> generatePdfTreatment(PdfDerivedTreatmentKind.NARRATABILITY_REVIEW));
    private final VBox audioPreparationScopeSection = new VBox(6);
    private final VBox documentTranslationSection = new VBox(6);
    private final CheckBox documentTranslationEnabled =
            StudioFormControls.checkBox("Activar traducción (requiere IA)");
    private final ComboBox<DocumentListeningLanguage> documentListeningLanguage =
            StudioFormControls.comboBox(FXCollections.observableArrayList(
                    DocumentListeningLanguage.values()));
    private final Label documentTranslationNotice = new Label();
    private final Label audioPreparationSummary = new Label();
    private final ComboBox<DocumentProcessingScope> audioPreparationScope =
            StudioFormControls.comboBox(FXCollections.observableArrayList(
                    DocumentProcessingScope.values()));
    private final VBox documentProcessingIntervalControls = new VBox(6);
    private final Spinner<Integer> documentProcessingIntervalStart =
            StudioFormControls.spinner(1, 1, 1, 1);
    private final Spinner<Integer> documentProcessingIntervalEnd =
            StudioFormControls.spinner(1, 1, 1, 1);
    private boolean pdfAnalysisRunning;
    private boolean refreshingChoralVoice;
    private boolean refreshingAudioPreparationScope;
    private boolean refreshingListeningPreferences;

    public DocumentContextDetailsPanel(DocuPodcastShellViewModel viewModel) {
        this(viewModel, viewModel::playFromSelectedSegment,
                viewModel::generateAudioChunksFromSelectedFragment);
    }

    public DocumentContextDetailsPanel(
            DocuPodcastShellViewModel viewModel,
            Runnable playSelectionAction,
            Runnable processSelectionAction) {
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
            var snapshot = viewModel.resolveCurrentDocumentSelection();
            if (snapshot.isPresent() && snapshot.get().pdf()) {
                var value = snapshot.get();
                return "Página " + value.pageNumber() + " · " + value.type()
                        + " · revisión " + value.revision();
            }
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            DocumentTextRange textRange = viewModel.selectedDocumentTextRangeProperty().get();
            if (blockId == null || blockId.isBlank()) {
                return "Haz clic en una oración, bloque o región del documento.";
            }
            if (textRange == null) {
                return "Bloque " + blockId;
            }
            return "Bloque " + blockId + " · " + textRange.displayLabel();
        }, viewModel.selectedDocumentBlockIdProperty(), viewModel.selectedDocumentTextRangeProperty(),
                viewModel.selectedPdfRegionProperty()));

        Label sourceLocation = new Label();
        sourceLocation.getStyleClass().add("document-context-source-location");
        sourceLocation.setWrapText(true);
        sourceLocation.textProperty().bind(viewModel.selectedDocumentSourceLocationProperty());

        Button play = action("Escuchar desde aquí",
                playSelectionAction);
        Button processFragment = action("Procesar este fragmento",
                processSelectionAction);
        configureChoralVoiceSection();
        configurePdfAnalysisSection();
        configureAudioPreparationScope();
        configureDocumentTranslation();
        configureSemanticReadingPolicy();
        pdfAdvancedReview.setExpanded(false);
        pdfAdvancedReview.getStyleClass().add("document-context-advanced-review");

        VBox body = new VBox(9,
                header,
                selected,
                location,
                sourceLocation,
                new RailActionRow(play),
                new RailActionRow(processFragment),
                audioPreparationScopeSection,
                documentTranslationSection,
                semanticReadingSettings,
                pdfAdvancedReview,
                choralVoiceSection,
                new RailActionRow(openDocumentAiSettings));
        body.getStyleClass().add("document-context-body");
        body.getStyleClass().add("document-fragment-inspector");
        body.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = StudioViewportControls.scrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("document-context-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.selectedPdfRegionProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.selectedPdfRegionProperty().addListener((obs, oldValue, newValue) -> refreshPdfAnalysisSection());
        viewModel.currentPreparedPdfSourceProperty().addListener(
                (obs, oldValue, newValue) -> {
                    refreshPdfAnalysisSection();
                    refreshListeningPreferences();
                    refreshSemanticReadingVisibility();
                });
        viewModel.currentDocumentProperty().addListener(
                (obs, oldValue, newValue) -> {
                    refreshListeningPreferences();
                    refreshSemanticReadingVisibility();
                });
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.currentProjectModeProperty().addListener(
                (obs, oldValue, newValue) -> refreshListeningPreferences());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.activeVoiceLibraryProperty().addListener((obs, oldValue, newValue) -> refreshChoralVoiceSection());
        viewModel.choralVoiceRenderingProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        viewModel.choralVoiceRenderProgressProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        viewModel.choralVoiceRenderStatusProperty().addListener((obs, oldValue, newValue) -> updateChoralVoiceControls());
        viewModel.selectedDocumentRangeLabelProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.currentScriptProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        refreshChoralVoiceSection();
        refreshPdfAnalysisSection();
        refreshAudioPreparationScope();
        refreshListeningPreferences();
        refreshSemanticReadingVisibility();
    }

    private void refreshSemanticReadingVisibility() {
        setShown(semanticReadingSettings,
                viewModel.currentPreparedPdfSourceProperty().get() != null
                        || viewModel.currentDocumentProperty().get() != null);
        refreshWordTableReadingControl();
    }

    private void configureAudioPreparationScope() {
        audioPreparationScopeSection.getStyleClass().add(
                "document-audio-preparation-scope");
        audioPreparationSummary.setWrapText(true);
        audioPreparationSummary.getStyleClass().add("document-context-note");
        audioPreparationScope.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(audioPreparationScope,
                "Elige si el procesamiento abarca solo el fragmento, desde aquí "
                        + "un intervalo documental o la lectura completa.");
        audioPreparationScope.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(DocumentProcessingScope item,
                                                boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                boolean selectable = empty || item == null
                        || viewModel.documentInteractionProjectionProperty().get()
                        .selectable(item);
                setDisable(!selectable);
                setOpacity(selectable ? 1.0 : 0.48);
            }
        });
        audioPreparationScope.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(DocumentProcessingScope item,
                                                boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setOpacity(1.0);
            }
        });
        StudioFormControls.spinner(documentProcessingIntervalStart,
                "Primera página PDF o bloque narrable Word incluido.");
        StudioFormControls.spinner(documentProcessingIntervalEnd,
                "Última página PDF o bloque narrable Word incluido.");
        documentProcessingIntervalControls.getChildren().setAll(
                new Label("Inicio"), documentProcessingIntervalStart,
                new Label("Fin"), documentProcessingIntervalEnd);
        audioPreparationScope.valueProperty().addListener(
                (obs, oldValue, selected) -> {
                    if (refreshingAudioPreparationScope || selected == null) return;
                    if (selected == DocumentProcessingScope.INTERVAL
                            && oldValue != DocumentProcessingScope.INTERVAL
                            && viewModel.documentProcessingIntervalSupportedProperty().get()) {
                        int last = viewModel.documentProcessingPageCount();
                        int anchor = Math.max(1, Math.min(last,
                                viewModel.documentProcessingIntervalAnchor()));
                        viewModel.setDocumentProcessingInterval(anchor,
                                Math.min(last, anchor + 1));
                    }
                    viewModel.setDocumentProcessingScope(selected);
                    refreshAudioPreparationScope();
                });
        documentProcessingIntervalStart.valueProperty().addListener(
                (obs, oldValue, value) -> {
                    if (!refreshingAudioPreparationScope && value != null) {
                        viewModel.setDocumentProcessingIntervalStart(value);
                    }
                });
        documentProcessingIntervalEnd.valueProperty().addListener(
                (obs, oldValue, value) -> {
                    if (!refreshingAudioPreparationScope && value != null) {
                        viewModel.setDocumentProcessingIntervalEnd(value);
                    }
                });
        viewModel.documentProcessingScopeProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentProcessingPageCountProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentProcessingIntervalStartProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentProcessingIntervalEndProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentProcessingIntervalSupportedProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentAudioPortionEnabledProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentAudioPreparationExtentProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());
        viewModel.documentSelectionValidProperty().addListener(
                (obs, oldValue, newValue) -> refreshAudioPreparationScope());

        audioPreparationScopeSection.getChildren().setAll(
                new SectionHeader("Alcance", ""),
                audioPreparationScope,
                documentProcessingIntervalControls,
                audioPreparationSummary);
    }

    private void refreshAudioPreparationScope() {
        refreshingAudioPreparationScope = true;
        try {
            DocumentProcessingScope selected = viewModel.documentProcessingScope();
            // ComboBox reuses popup cells. Replacing the tiny enum list forces
            // their disabled/opacity state to follow the current selection
            // projection instead of retaining the state from the last popup.
            audioPreparationScope.getItems().setAll(
                    DocumentProcessingScope.values());
            audioPreparationScope.setValue(selected);
            audioPreparationScope.setDisable(false);
            int pageCount = Math.max(1, viewModel.documentProcessingPageCount());
            int start = Math.max(1, Math.min(pageCount,
                    viewModel.documentProcessingIntervalStartProperty().get()));
            int end = Math.max(1, Math.min(pageCount,
                    viewModel.documentProcessingIntervalEndProperty().get()));
            documentProcessingIntervalStart.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, pageCount, start, 1));
            documentProcessingIntervalEnd.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, pageCount, end, 1));
            boolean showInterval = selected == DocumentProcessingScope.INTERVAL
                    && viewModel.documentProcessingIntervalSupportedProperty().get();
            setShown(documentProcessingIntervalControls, showInterval);
            audioPreparationSummary.setText(
                    viewModel.documentAudioPreparationSummary());
        } finally {
            refreshingAudioPreparationScope = false;
        }
    }

    private void configureSemanticReadingPolicy() {
        semanticReadingSettings.getStyleClass().add(
                "document-semantic-reading-settings");
        semanticReadingPolicy.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(semanticReadingPolicy,
                "Clasifica primero cada componente y después aplica esta política.");
        semanticReadingPolicyNotice.setWrapText(true);
        semanticReadingPolicyNotice.getStyleClass().add("document-context-note");
        wordTableReadingNotice.setWrapText(true);
        wordTableReadingNotice.getStyleClass().add("document-context-note");
        readCompleteWordTables.setTooltip(new Tooltip(
                "Activado: lee encabezados y todas las celdas por filas. "
                        + "Desactivado: genera un resumen determinístico breve."));
        readCompleteWordTables.selectedProperty().addListener(
                (obs, oldValue, selected) -> {
                    if (refreshingListeningPreferences) return;
                    viewModel.setTableNarrationPolicy(selected
                            ? TableNarrationPolicy.READ_ALL
                            : TableNarrationPolicy.SUMMARIZE);
                    refreshWordTableReadingControl();
                });
        semanticReadingPolicy.valueProperty().addListener(
                (obs, oldValue, selected) -> {
                    if (refreshingListeningPreferences || selected == null) return;
                    viewModel.setDocumentListeningPreferences(
                            viewModel.documentListeningPreferences()
                                    .withSecondarySemanticPolicy(selected));
                    semanticReadingPolicyNotice.setText(selected.description());
                    refreshWordTableReadingControl();
                });
    }

    private void refreshWordTableReadingControl() {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        boolean word = document != null
                && document.format() == SourceDocumentFormat.DOCX;
        setShown(readCompleteWordTables, word);
        setShown(wordTableReadingNotice, word);
        if (!word) return;
        SecondarySemanticReadingPolicy policy = semanticReadingPolicy.getValue();
        if (policy == null) {
            policy = viewModel.documentListeningPreferences()
                    .secondarySemanticPolicy();
        }
        boolean tablesIncluded = policy.includes(
                SecondarySemanticComponentKind.TABLE);
        readCompleteWordTables.setDisable(!tablesIncluded);
        boolean wasRefreshing = refreshingListeningPreferences;
        refreshingListeningPreferences = true;
        try {
            readCompleteWordTables.setSelected(
                    viewModel.tableNarrationPolicy().readsAll());
        } finally {
            refreshingListeningPreferences = wasRefreshing;
        }
        wordTableReadingNotice.setText(tablesIncluded
                ? "Procesamiento determinístico; solo Word. Desactivado usa un resumen breve."
                : "La política seleccionada omite tablas y ecuaciones Word.");
    }

    private void configureDocumentTranslation() {
        documentTranslationSection.getStyleClass().add(
                "document-translation-settings");
        documentListeningLanguage.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(documentListeningLanguage,
                "Idioma utilizado por la narración y el motor de voz.");
        documentTranslationNotice.setWrapText(true);
        documentTranslationNotice.getStyleClass().add("document-context-note");
        documentTranslationEnabled.selectedProperty().addListener(
                (obs, oldValue, enabled) -> {
                    if (refreshingListeningPreferences) return;
                    viewModel.setDocumentTranslationPreferences(
                            viewModel.documentTranslationPreferences()
                                    .withEnabled(enabled));
                    refreshListeningPreferences();
                });
        documentListeningLanguage.valueProperty().addListener(
                (obs, oldValue, language) -> {
                    if (refreshingListeningPreferences || language == null) return;
                    viewModel.setDocumentTranslationPreferences(
                            viewModel.documentTranslationPreferences()
                                    .withListeningLanguage(language));
                    refreshListeningPreferences();
                });
        documentTranslationSection.getChildren().setAll(
                new SectionHeader("Idioma y traducción",
                        "Escucha el documento en Español o English sin alterar la fuente."),
                documentTranslationEnabled,
                new Label("Idioma de escucha"),
                documentListeningLanguage,
                documentTranslationNotice);
    }

    private void configurePdfAnalysisSection() {
        pdfAnalysisSection.getStyleClass().add("document-pdf-analysis-section");
        pdfAnalysisStatus.setWrapText(true);
        pdfAnalysisStatus.getStyleClass().add("document-context-note");
        pdfAnalysisProgress.setMaxWidth(Double.MAX_VALUE);
        pdfAnalysisProgress.setVisible(false);
        pdfAnalysisProgress.setManaged(false);
        pdfAnalysisResult.setEditable(false);
        pdfAnalysisResult.setWrapText(true);
        pdfAnalysisResult.setPrefRowCount(7);
        pdfAnalysisResult.setPromptText("La propuesta local aparecerá aquí para revisión.");
        refreshListeningPreferences();

        visualReviewActions.getChildren().setAll(
                new RailActionRow(describePdfRegion));
        mathReviewActions.getChildren().setAll(
                new RailActionRow(readPdfFormula));
        tableReviewActions.getChildren().setAll(
                new RailActionRow(preparePdfTable),
                new RailActionRow(editPdfTableCells));
        textReviewActions.getChildren().setAll(
                new RailActionRow(correctPdfRegion));
        reviewResultActions.getChildren().setAll(
                new RailActionRow(approvePdfTreatment),
                new RailActionRow(rejectPdfTreatment),
                new RailActionRow(regeneratePdfTreatment));
        batchReviewActions.getChildren().setAll(
                new RailActionRow(reviewPageDrafts),
                new RailActionRow(reviewDocumentDrafts));
        Label uncertainHelp = new Label(
                "DocuPodcast no está seguro de esta región. Puedes resolverla "
                        + "con una sola decisión o pedir una propuesta local.");
        uncertainHelp.setWrapText(true);
        uncertainHelp.getStyleClass().add("document-context-note");
        uncertainReviewActions.getChildren().setAll(
                uncertainHelp,
                new RailActionRow(keepUncertainAsNarratable),
                new RailActionRow(omitUncertainFromReading),
                new RailActionRow(analyzeUncertainLocally));
        pdfAnalysisSection.getChildren().setAll(
                new SectionHeader("Elemento seleccionado",
                        "Interpreta, corrige o decide si el borrador entra en la lectura."),
                pdfAnalysisStatus,
                uncertainReviewActions,
                pdfAnalysisProgress,
                visualReviewActions,
                mathReviewActions,
                tableReviewActions,
                textReviewActions,
                new RailActionRow(defineManualDescription),
                pdfAnalysisResult,
                reviewResultActions);
    }

    private void refreshPdfAnalysisSection() {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        var selection = viewModel.selectedPdfRegionProperty().get();
        boolean visible = source != null && selection != null;
        pdfAdvancedReview.setVisible(visible);
        pdfAdvancedReview.setManaged(visible);
        if (!visible) pdfAdvancedReview.setExpanded(false);
        pdfAnalysisSection.setVisible(visible);
        pdfAnalysisSection.setManaged(visible);
        activePdfTreatment = null;
        activePdfTreatmentPageNumber = -1;
        editedPdfTableText = "";
        selectedPdfTableRows = "";
        selectedPdfTableColumns = "";
        if (!visible) {
            updatePdfAnalysisControls();
            return;
        }
        try {
            var selectionSnapshot = viewModel.resolveCurrentDocumentSelection()
                    .orElse(null);
            boolean uncertain = selectionSnapshot != null
                    && selectionSnapshot.narratability()
                    == PdfNarratability.UNCERTAIN;
            uncertainReviewActions.setVisible(uncertain);
            uncertainReviewActions.setManaged(uncertain);
            List<PdfDerivedTreatment> treatments = viewModel.projectWorkspace().document()
                    .listPdfDerivedTreatments().forRegion(
                            source.workspace().projectRoot(), selection.pageNumber(),
                            selection.regionId());
            activePdfTreatment = treatments.isEmpty() ? null : treatments.getFirst();
            activePdfTreatmentPageNumber = activePdfTreatment == null
                    ? -1 : selection.pageNumber();
            if (activePdfTreatment == null) {
                pdfAnalysisStatus.setText("Región lista. Elige una acción de revisión.");
                pdfAnalysisResult.clear();
            } else {
                showPdfTreatment(activePdfTreatment);
            }
        } catch (Exception failure) {
            pdfAnalysisStatus.setText("No se pudieron leer las propuestas guardadas.");
            pdfAnalysisResult.clear();
        }
        updatePdfAnalysisControls();
    }

    private static String capabilitySummary(
            PdfProcessingCapabilityProfile profile) {
        java.util.ArrayList<String> available = new java.util.ArrayList<>();
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        addCapability(profile.ocrAvailable(), "OCR", available, missing);
        addCapability(profile.visualDescriptionAvailable(),
                "descripción visual", available, missing);
        addCapability(profile.tableStructureAvailable(),
                "tablas avanzadas", available, missing);
        addCapability(profile.mathRecognitionAvailable(),
                "reconocimiento matemático", available, missing);
        addCapability(profile.mathSpeechAvailable(),
                "voz matemática", available, missing);
        return "Disponible: "
                + (available.isEmpty() ? "lectura esencial" : String.join(", ", available))
                + ". No disponible: "
                + (missing.isEmpty() ? "ninguna dependencia opcional" : String.join(", ", missing))
                + ". Perfil de recursos: "
                + (profile.hardwareClass()
                == PdfProcessingCapabilityProfile.HardwareClass.LIMITED
                ? "conservador" : "estándar") + ".";
    }

    private static void addCapability(
            boolean present, String label, List<String> available,
            List<String> missing) {
        (present ? available : missing).add(label);
    }

    private void applySelectedPdfNarratability(PdfNarratability narratability) {
        try {
            viewModel.setSelectedPdfRegionNarratability(narratability);
            pdfAnalysisStatus.setText(narratability == PdfNarratability.NARRATABLE
                    ? "Decisión guardada: esta región se narrará."
                    : "Decisión guardada: esta región se omitirá de la lectura.");
            refreshPdfAnalysisSection();
        } catch (Exception failure) {
            pdfAnalysisStatus.setText("No se pudo guardar la decisión: "
                    + (failure.getMessage() == null
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage()));
        }
    }

    private void generatePdfTreatment(PdfDerivedTreatmentKind kind) {
        generatePdfTreatment(kind, true, false, "");
    }

    private void generatePdfTreatment(PdfDerivedTreatmentKind kind, boolean retryAllowed) {
        generatePdfTreatment(kind, retryAllowed, false, "");
    }

    private void generatePdfTreatment(PdfDerivedTreatmentKind kind, boolean retryAllowed,
                                      boolean forceRegenerate) {
        generatePdfTreatment(kind, retryAllowed, forceRegenerate, "");
    }

    private void generatePdfTreatment(PdfDerivedTreatmentKind kind,
                                      boolean retryAllowed,
                                      boolean forceRegenerate,
                                      String engineOverride) {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        var selection = viewModel.selectedPdfRegionProperty().get();
        var visualTarget = viewModel.selectedPdfVisualTargetProperty().get();
        var snapshot = viewModel.resolveCurrentDocumentSelection().orElse(null);
        if (source == null || selection == null || snapshot == null || snapshot.geometry() == null) {
            pdfAnalysisStatus.setText("Selecciona primero una región PDF preparada.");
            return;
        }
        Path requestedRoot = source.workspace().projectRoot()
                .toAbsolutePath().normalize();
        int requestedPage = selection.pageNumber();
        String requestedRegion = selection.regionId();
        List<String> requestedRegions = visualTarget != null
                && visualTarget.pageNumber() == requestedPage
                && visualTarget.kind()
                == com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfVisualTextTargetKind.SEMANTIC_COMPONENT
                ? visualTarget.sourceRegionIds() : List.of(requestedRegion);
        var requestedGeometry = visualTarget != null
                && visualTarget.pageNumber() == requestedPage
                && visualTarget.region() != null
                ? visualTarget.region() : snapshot.geometry();
        pdfAnalysisRunning = true;
        pdfAnalysisStatus.setText(switch (kind) {
            case IMAGE_DESCRIPTION -> "Preparando la zona seleccionada y su descripción…";
            case MATHEMATICAL_READING -> "Reconociendo la fórmula y preparando su lectura…";
            case TABLE_NARRATION ->
                    "Reconociendo celdas y preparando una lectura revisable…";
            case TABLE_STRUCTURE ->
                    "Reconociendo la estructura avanzada de la tabla…";
            case CONTEXTUAL_CORRECTION -> "Preparando una corrección contextual local…";
            case NARRATABILITY_REVIEW ->
                    "Revisando todas las regiones de la página sin modificar su texto…";
            default -> "Preparando la revisión local…";
        });
        viewModel.beginLocalDocumentAnalysis(
                localAnalysisTitle(kind), localAnalysisDetail(kind));
        updatePdfAnalysisControls();
        Task<PdfDerivedTreatment> task = new Task<>() {
            @Override
            protected PdfDerivedTreatment call() throws Exception {
                PreparedPdfRegionContext context = viewModel.projectWorkspace().document()
                        .buildPreparedPdfRegionContext()
                        .build(source.workspace(), selection, 3).orElse(null);
                Map<String, String> options = new LinkedHashMap<>();
                options.put("language", sourceLanguage(snapshot.text()));
                options.put("nearbyContext", contextText(context, snapshot.text()));
                options.put("forceRegenerate", Boolean.toString(forceRegenerate));
                options.put("model", DocumentListeningPreferences.QUALITY_MODEL);
                options.put("objectId", visualTarget == null
                        ? requestedRegion : visualTarget.id());
                options.put("componentGeometry", requestedGeometry.bbox());
                options.put("evidenceRegionIds", String.join(",", requestedRegions));
                options.put("semanticKind", switch (kind) {
                    case MATHEMATICAL_READING -> "EQUATION";
                    case TABLE_NARRATION -> "TABLE";
                    case IMAGE_DESCRIPTION -> "UNKNOWN".equalsIgnoreCase(snapshot.type())
                            ? "EXTRA" : "IMAGE";
                    default -> "";
                });
                options.put("semanticPolicy", viewModel
                        .documentListeningPreferences()
                        .secondarySemanticPolicy().name());
                options.put("includeContextPage", "true");
                options.put("computePriority",
                        ComputeJobPriority.INTERACTIVE_ANALYSIS.name());
                if (kind == PdfDerivedTreatmentKind.TABLE_NARRATION
                        && !editedPdfTableText.isBlank()) {
                    options.put("editedTableText", editedPdfTableText);
                }
                if (kind == PdfDerivedTreatmentKind.TABLE_NARRATION) {
                    options.put("semanticKind", "TABLE");
                    if (!selectedPdfTableRows.isBlank()) {
                        options.put("selectedRows", selectedPdfTableRows);
                    }
                    if (!selectedPdfTableColumns.isBlank()) {
                        options.put("selectedColumns",
                                selectedPdfTableColumns);
                    }
                }
                PdfAnalysisVisualEvidence visuals = null;
                try {
                    if (kind == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION
                            || kind == PdfDerivedTreatmentKind.MATHEMATICAL_READING
                            || kind == PdfDerivedTreatmentKind.TABLE_STRUCTURE
                            || kind == PdfDerivedTreatmentKind.TABLE_NARRATION) {
                        visuals = viewModel.projectWorkspace().document()
                                .capturePdfAnalysisVisualEvidence()
                                .capture(source.sourcePath(), requestedGeometry);
                        options.put("roiImage", visuals.roiImage().toString());
                        options.put("contextImage", visuals.markedPageImage().toString());
                    }
                    String engine = !engineOverride.isBlank()
                            ? engineOverride : switch (kind) {
                        case IMAGE_DESCRIPTION ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalVisualPdfTreatmentEngine.ID;
                        case MATHEMATICAL_READING ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalMathPdfTreatmentEngine.ID;
                        case CONTEXTUAL_CORRECTION ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalContextCorrectionPdfTreatmentEngine.ID;
                        case NARRATABILITY_REVIEW ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalNarratabilityReviewPdfTreatmentEngine.ID;
                        case TABLE_STRUCTURE ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalPpTableStructurePdfTreatmentEngine.ID;
                        case TABLE_NARRATION ->
                                com.marcosmoreiradev.docupodcaststudio.application.document
                                        .TransversalTableExplanationPdfTreatmentEngine.ID;
                        default -> throw new IllegalArgumentException(
                                "Tratamiento no disponible desde este panel.");
                    };
                    return viewModel.projectWorkspace().document().generatePdfDerivedTreatment()
                            .execute(new PdfDerivedTreatmentGenerationRequest(
                                    source.workspace().projectRoot(),
                                    selection.pageNumber(), kind,
                                    requestedRegions, engine, true, options));
                } finally {
                    if (visuals != null) visuals.close();
                }
            }
        };
        task.setOnSucceeded(event -> {
            pdfAnalysisRunning = false;
            viewModel.endLocalDocumentAnalysis();
            PdfDerivedTreatment generated = task.getValue();
            if (!currentPdfSelectionMatches(
                    requestedRoot, requestedPage, requestedRegion)) {
                viewModel.updateStatusMessage(
                        "El análisis local terminó y quedó guardado en su página de origen.");
                updatePdfAnalysisControls();
                return;
            }
            activePdfTreatment = generated;
            activePdfTreatmentPageNumber = requestedPage;
            showPdfTreatment(activePdfTreatment);
            viewModel.updateStatusMessage(
                    com.marcosmoreiradev.docupodcaststudio.application.document
                            .PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION
                            .equals(generated.metadata().get("automaticAdmission"))
                            ? "Borrador Qwen Q8 validado para la política activa; "
                            + "puedes aprobarlo, corregirlo o rechazarlo."
                            : "Borrador pendiente; se omitirá hasta que sea seguro o lo apruebes.");
            updatePdfAnalysisControls();
        });
        task.setOnFailed(event -> {
            pdfAnalysisRunning = false;
            viewModel.endLocalDocumentAnalysis();
            Throwable failure = task.getException();
            if (!currentPdfSelectionMatches(
                    requestedRoot, requestedPage, requestedRegion)) {
                updatePdfAnalysisControls();
                return;
            }
            pdfAnalysisStatus.setText("La propuesta no pudo generarse; la región original permanece intacta.");
            updatePdfAnalysisControls();
            ButtonType openAndRetry = NativeDialogResponse.button(
                    "Abrir Motores y reintentar", ButtonBar.ButtonData.OTHER);
            boolean missing = engineFailureCode(failure) == EngineDiagnosticCode.MISSING_RESOURCE
                    || engineFailureCode(failure) == EngineDiagnosticCode.INVALID_RESOURCE;
            var dialog = StudioMessageDialog.create(
                    getScene() == null ? null : getScene().getWindow(),
                    Alert.AlertType.WARNING,
                    "Análisis local",
                    "No se pudo completar el tratamiento",
                    "La región original y cualquier resultado válido anterior permanecen intactos. "
                            + (missing
                            ? "Puedes preparar o importar la dependencia desde Motores y dependencias."
                            : "Revisa los detalles técnicos antes de reintentar."),
                    StudioMessageDialog.technicalDetail(failure),
                    missing && retryAllowed ? openAndRetry : ButtonType.CLOSE,
                    ButtonType.CANCEL);
            var selected = dialog.showAndWait().orElse(ButtonType.CANCEL);
            if (selected == openAndRetry && retryAllowed) {
                openDocumentAiSettings();
                generatePdfTreatment(
                        kind, false, forceRegenerate, engineOverride);
            }
        });
        Thread.ofVirtual().name("pdf-local-derived-treatment").start(task);
    }

    private static String localAnalysisTitle(PdfDerivedTreatmentKind kind) {
        return switch (kind) {
            case IMAGE_DESCRIPTION -> "Describiendo un elemento técnico";
            case MATHEMATICAL_READING ->
                    "Preparando la lectura de una fórmula";
            case TABLE_NARRATION, TABLE_STRUCTURE ->
                    "Preparando la lectura de una tabla";
            case CONTEXTUAL_CORRECTION ->
                    "Revisando el texto seleccionado";
            case NARRATABILITY_REVIEW ->
                    "Revisando contenido dudoso";
            default -> "Analizando contenido del documento";
        };
    }

    private static String localAnalysisDetail(PdfDerivedTreatmentKind kind) {
        return switch (kind) {
            case IMAGE_DESCRIPTION ->
                    "La IA local está describiendo la zona seleccionada. "
                    + "El resultado quedará como borrador pendiente de revisión.";
            case MATHEMATICAL_READING ->
                    "DocuPodcast está reconociendo la expresión y preparando "
                    + "una lectura clara y reutilizable.";
            case TABLE_NARRATION, TABLE_STRUCTURE ->
                    "DocuPodcast está identificando la estructura y el "
                    + "contenido de la tabla sin modificar el documento.";
            case CONTEXTUAL_CORRECTION ->
                    "La IA local está proponiendo una corrección contextual "
                    + "sin reemplazar el texto original.";
            case NARRATABILITY_REVIEW ->
                    "La IA local está distinguiendo texto narrable, "
                    + "galimatías y elementos técnicos describibles. "
                    + "Las decisiones quedarán pendientes de aprobación.";
            default ->
                    "La IA local está preparando una propuesta revisable.";
        };
    }

    private void editPdfTableCells() {
        var snapshot = viewModel.resolveCurrentDocumentSelection().orElse(null);
        if (snapshot == null || !snapshot.pdf()
                || !"TABLE".equalsIgnoreCase(snapshot.type())) {
            pdfAnalysisStatus.setText("Selecciona primero una región de tabla.");
            return;
        }
        Dialog<String> dialog = StudioDialogShell.dialog();
        if (getScene() != null) dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Editar celdas detectadas");
        TextArea editor = StudioFormControls.textArea(
                editedPdfTableText.isBlank()
                        ? snapshot.text() : editedPdfTableText);
        editor.setWrapText(false);
        editor.setPrefRowCount(14);
        editor.setPromptText(
                "Una fila por línea; separa las celdas con tabuladores o |.");
        Label guidance = new Label(
                "Corrige únicamente la estructura derivada. El texto y la "
                        + "geometría del PDF original no se modifican.");
        guidance.setWrapText(true);
        VBox content = new VBox(8, guidance, editor);
        content.setPrefWidth(720);
        VBox.setVgrow(editor, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        ButtonType save = NativeDialogResponse.button(
                "Usar estas celdas", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == save
                ? editor.getText() : null);
        dialog.setResizable(true);
        dialog.showAndWait().ifPresent(value -> {
            editedPdfTableText = value == null ? "" : value.strip();
            pdfAnalysisStatus.setText(editedPdfTableText.isBlank()
                    ? "No se guardó una edición de celdas."
                    : "Celdas corregidas listas; prepara la lectura para "
                    + "crear un nuevo borrador.");
        });
    }

    private void selectPdfTableRange() {
        var snapshot = viewModel.resolveCurrentDocumentSelection().orElse(null);
        if (snapshot == null || !snapshot.pdf()
                || !"TABLE".equalsIgnoreCase(snapshot.type())) {
            pdfAnalysisStatus.setText("Selecciona primero una región de tabla.");
            return;
        }
        Dialog<String[]> dialog = StudioDialogShell.dialog();
        if (getScene() != null) dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Elegir filas o columnas");
        TextField rows = StudioFormControls.textField(selectedPdfTableRows);
        TextField columns = StudioFormControls.textField(
                selectedPdfTableColumns);
        rows.setPromptText("Ejemplo: 1-3,5");
        columns.setPromptText("Ejemplo: 1,3-4");
        Label guidance = new Label(
                "Usa números desde 1. Si completas ambos campos se narrará "
                        + "su intersección. Deja ambos vacíos para la política "
                        + "automática segura.");
        guidance.setWrapText(true);
        VBox content = new VBox(8,
                guidance,
                new Label("Filas"), rows,
                new Label("Columnas"), columns);
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        ButtonType use = NativeDialogResponse.button(
                "Usar selección", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(
                use, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == use
                ? new String[]{rows.getText(), columns.getText()} : null);
        dialog.setResizable(true);
        dialog.showAndWait().ifPresent(value -> {
            selectedPdfTableRows = value[0] == null
                    ? "" : value[0].strip();
            selectedPdfTableColumns = value[1] == null
                    ? "" : value[1].strip();
            pdfAnalysisStatus.setText(
                    selectedPdfTableRows.isBlank()
                            && selectedPdfTableColumns.isBlank()
                            ? "Se usará la política automática de tabla."
                            : "Selección de tabla lista; prepara la lectura "
                            + "para crear un nuevo borrador.");
        });
    }

    private void reviewPdfTreatment(PdfDerivedTreatmentState state) {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null || activePdfTreatment == null
                || activePdfTreatmentPageNumber < 1) return;
        try {
            viewModel.projectWorkspace().document().updatePdfDerivedTreatment().reviewOrUpsert(
                    source.workspace().projectRoot(), activePdfTreatmentPageNumber,
                    activePdfTreatment, state);
            activePdfTreatment = activePdfTreatment.withState(state);
            showPdfTreatment(activePdfTreatment);
            viewModel.updateStatusMessage(state == PdfDerivedTreatmentState.APPROVED
                    ? "Tratamiento aprobado; la próxima narración podrá utilizarlo."
                    : "Tratamiento rechazado; no se utilizará para narración.");
        } catch (Exception failure) {
            StudioMessageDialog.create(getScene() == null ? null : getScene().getWindow(),
                    Alert.AlertType.ERROR,
                    "Revisión de propuesta",
                    "No se pudo guardar la decisión",
                    "La propuesta conserva su estado anterior.",
                    StudioMessageDialog.technicalDetail(failure)).showAndWait();
        }
        updatePdfAnalysisControls();
    }

    private void reviewDraftBatch(boolean currentPageOnly) {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        var selection = viewModel.selectedPdfRegionProperty().get();
        if (source == null || currentPageOnly && selection == null) return;
        try {
            Integer page = currentPageOnly ? selection.pageNumber() : null;
            var useCase = viewModel.projectWorkspace().document()
                    .reviewPdfTreatmentBatch();
            var preview = useCase.preview(source.workspace().projectRoot(), page);
            if (preview.items().isEmpty()) {
                pdfAnalysisStatus.setText(currentPageOnly
                        ? "Esta página no tiene borradores pendientes."
                        : "El documento no tiene borradores pendientes.");
                return;
            }
            String details = preview.items().stream().limit(20)
                    .map(item -> "Página " + item.pageNumber() + " · "
                            + item.kind() + "\n" + item.preview())
                    .collect(java.util.stream.Collectors.joining("\n\n"));
            if (preview.items().size() > 20) {
                details += "\n\n… y " + (preview.items().size() - 20)
                        + " borradores más.";
            }
            ButtonType approve = NativeDialogResponse.button(
                    "Aprobar los mostrados", ButtonBar.ButtonData.OK_DONE);
            ButtonType reject = NativeDialogResponse.button(
                    "Rechazar los mostrados", ButtonBar.ButtonData.OTHER);
            var dialog = StudioMessageDialog.create(
                    getScene() == null ? null : getScene().getWindow(),
                    Alert.AlertType.CONFIRMATION,
                    "Revisión de borradores PDF",
                    "Previsualiza antes de aplicar la decisión",
                    "Ninguna propuesta se aprobará sin elegir explícitamente una acción.",
                    details, approve, reject, ButtonType.CANCEL);
            ButtonType selected = dialog.showAndWait().orElse(ButtonType.CANCEL);
            if (selected != approve && selected != reject) return;
            PdfDerivedTreatmentState state = selected == approve
                    ? PdfDerivedTreatmentState.APPROVED
                    : PdfDerivedTreatmentState.REJECTED;
            int changed = useCase.apply(source.workspace().projectRoot(),
                    preview.items().stream()
                            .map(item -> item.treatmentId()).toList(), state);
            pdfAnalysisStatus.setText(changed + " borradores "
                    + (state == PdfDerivedTreatmentState.APPROVED
                    ? "aprobados" : "rechazados") + ".");
            refreshPdfAnalysisSection();
        } catch (Exception failure) {
            pdfAnalysisStatus.setText("No se pudo completar la revisión por lote: "
                    + (failure.getMessage() == null
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage()));
        }
    }

    public void defineManualDescription() {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            defineManualWordDescription();
            return;
        }
        var selection = viewModel.selectedPdfRegionProperty().get();
        var snapshot = viewModel.resolveCurrentDocumentSelection().orElse(null);
        if (source == null || selection == null || snapshot == null
                || !snapshot.pdf()) {
            return;
        }

        Dialog<String> dialog = StudioDialogShell.dialog();
        if (getScene() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.setTitle("Descripción para la lectura");
        Label guidance = new Label(
                "Escribe cómo debe explicarse esta fórmula, figura o zona cuando "
                        + "DocuPodcast llegue a ella. El PDF original no se modifica.");
        guidance.setWrapText(true);
        TextArea editor = StudioFormControls.textArea(manualDescriptionSeed());
        editor.setWrapText(true);
        editor.setPrefRowCount(7);
        editor.setPromptText("Ejemplo: Gráfica que muestra cómo la función se aproxima a uno…");
        VBox content = new VBox(8, guidance, editor);
        content.setPrefWidth(620);
        VBox.setVgrow(editor, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        ButtonType save = NativeDialogResponse.button(
                "Guardar descripción", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == save ? editor.getText() : null);
        dialog.setResizable(true);
        dialog.showAndWait().ifPresent(value -> {
            String description = value == null ? "" : value.strip();
            if (description.isBlank()) {
                pdfAnalysisStatus.setText("No se guardó una descripción vacía.");
                return;
            }
            String stableKey = selection.pageNumber() + "|"
                    + selection.regionId() + "|manual-description";
            String treatmentId = "PDF-DER-" + UUID.nameUUIDFromBytes(
                    stableKey.getBytes(StandardCharsets.UTF_8));
            PdfDerivedTreatment manual = new PdfDerivedTreatment(
                    treatmentId,
                    PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                    List.of(selection.regionId()),
                    description,
                    "manual-user",
                    "1",
                    1.0,
                    Instant.now(),
                    PdfDerivedTreatmentState.APPROVED,
                    Math.max(1L, snapshot.revision()),
                    "manual|" + selection.regionId() + "|" + snapshot.revision(),
                    "Descripción definida por el usuario.",
                    Map.of(
                            "manual", "true",
                            "reusableFor", "narration,document-video"));
            try {
                viewModel.projectWorkspace().document().updatePdfDerivedTreatment().upsert(
                        source.workspace().projectRoot(),
                        selection.pageNumber(),
                        manual);
                activePdfTreatment = manual;
                activePdfTreatmentPageNumber = selection.pageNumber();
                showPdfTreatment(manual);
                viewModel.updateStatusMessage(
                        "Descripción manual guardada para la lectura y el video documental.");
                updatePdfAnalysisControls();
            } catch (Exception failure) {
                StudioMessageDialog.create(
                        getScene() == null ? null : getScene().getWindow(),
                        Alert.AlertType.ERROR,
                        "Descripción para la lectura",
                        "No se pudo guardar la descripción",
                        "La fuente original permanece intacta.",
                        StudioMessageDialog.technicalDetail(failure))
                        .showAndWait();
            }
        });
    }

    /**
     * Shows the canonical text and the effective approved description for the
     * selected PDF region without exposing repository or model terminology.
     */
    public void viewContentAndDescription() {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            viewWordContentAndDescription();
            return;
        }
        var selection = viewModel.selectedPdfRegionProperty().get();
        var snapshot = viewModel.resolveCurrentDocumentSelection().orElse(null);
        if (source == null || selection == null || snapshot == null
                || !snapshot.pdf()) {
            return;
        }

        PdfDerivedTreatment effective = null;
        try {
            effective = viewModel.projectWorkspace().document()
                    .listPdfDerivedTreatments().forRegion(
                            source.workspace().projectRoot(),
                            selection.pageNumber(), selection.regionId())
                    .stream()
                    .filter(DocumentContextDetailsPanel::visibleDescription)
                    .sorted(java.util.Comparator
                            .comparingInt(DocumentContextDetailsPanel
                                    ::descriptionPriority)
                            .reversed()
                            .thenComparing(PdfDerivedTreatment::createdAt,
                                    java.util.Comparator.reverseOrder()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            // The dialog still provides the canonical document text.
        }

        Dialog<Boolean> dialog = StudioDialogShell.dialog();
        DialogStyler.apply(dialog,
                getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Contenido y descripción");

        var visualTarget = viewModel.selectedPdfVisualTargetProperty().get();
        String detectedText = snapshot.text();
        boolean sameTarget = visualTarget != null
                && visualTarget.pageNumber() == selection.pageNumber()
                && visualTarget.sourceRegionIds().contains(selection.regionId());
        if (sameTarget && !visualTarget.text().isBlank()) {
            detectedText = visualTarget.text();
        }

        Label detectedHeading = new Label(sameTarget
                && visualTarget.kind() == com.marcosmoreiradev.docupodcaststudio
                .application.document.PdfVisualTextTargetKind.SEMANTIC_COMPONENT
                ? "Texto y etiquetas asociados al componente"
                : "Texto seleccionado en el documento");
        detectedHeading.getStyleClass().add("document-context-field-label");
        TextArea detected = StudioFormControls.textArea(detectedText);
        detected.setEditable(false);
        detected.setWrapText(true);
        detected.setPrefRowCount(5);
        detected.setAccessibleText("Texto detectado por el documento");

        VBox content = new VBox(8, detectedHeading, detected);
        content.getStyleClass().add("document-content-description-dialog");
        content.setPrefWidth(680);
        VBox.setVgrow(detected, Priority.ALWAYS);

        if (effective != null && !effective.derivedText().isBlank()) {
            Label descriptionHeading = new Label("Descripción para la lectura");
            descriptionHeading.getStyleClass().add(
                    "document-context-field-label");
            TextArea description = StudioFormControls.textArea(
                    effective.derivedText());
            description.setEditable(false);
            description.setWrapText(true);
            description.setPrefRowCount(5);
            description.setAccessibleText("Descripción para la lectura");
            Label provenance = new Label(
                    effective.metadata().containsKey("manual")
                            || effective.modelId().startsWith("manual")
                            ? "Definida por ti"
                            : "Preparada localmente desde el recorte completo del elemento");
            provenance.getStyleClass().add("document-context-copy");
            content.getChildren().addAll(
                    descriptionHeading, description, provenance);
            VBox.setVgrow(description, Priority.ALWAYS);
        }

        String model = effective == null ? "" : effective.modelId();
        Label technical = new Label(
                "Página " + snapshot.pageNumber()
                        + "\nTipo: " + snapshot.type()
                        + "\nRevisión: " + snapshot.revision()
                        + "\nRegión: " + selection.regionId()
                        + (model.isBlank() ? "" : "\nPreparador: " + model));
        technical.setWrapText(true);
        technical.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        TitledPane details = StudioAccordion.pane(
                "Detalles técnicos", technical);
        details.setExpanded(false);
        content.getChildren().add(details);

        ButtonType edit = NativeDialogResponse.button(
                effective == null ? "Definir descripción" : "Editar descripción",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(640);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().getButtonTypes().setAll(edit, ButtonType.CLOSE);
        dialog.setResultConverter(button -> button == edit);
        dialog.setResizable(true);
        dialog.showAndWait().filter(Boolean::booleanValue)
                .ifPresent(ignored -> defineManualDescription());
    }

    private void defineManualWordDescription() {
        DocumentBlock block = selectedWordSemanticBlock();
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (block == null || document == null) return;

        Dialog<String> dialog = StudioDialogShell.dialog();
        DialogStyler.apply(dialog,
                getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Descripción para la lectura");
        Label guidance = new Label(
                "Escribe cómo debe explicarse este componente cuando DocuPodcast llegue a él. "
                        + "El Word original no se modifica.");
        guidance.setWrapText(true);
        TextArea editor = StudioFormControls.textArea(
                block.metadata().getOrDefault("description", ""));
        editor.setWrapText(true);
        editor.setPrefRowCount(7);
        editor.setPromptText("Descripción breve y natural para la lectura…");
        VBox content = new VBox(8, guidance, editor);
        content.setPrefWidth(620);
        VBox.setVgrow(editor, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        ButtonType save = NativeDialogResponse.button(
                "Guardar descripción", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == save ? editor.getText() : null);
        dialog.setResizable(true);
        dialog.showAndWait().ifPresent(value -> {
            String description = value == null ? "" : value.strip();
            if (description.isBlank()) {
                viewModel.updateStatusMessage("No se guardó una descripción vacía.");
                return;
            }
            viewModel.replaceCurrentDocumentFromApplication(
                    new ApplyManualWordSemanticDescriptionUseCase().apply(
                            document, block.id(), description, Instant.now()),
                    "Descripción manual guardada para la lectura y el video documental.");
        });
    }

    private void viewWordContentAndDescription() {
        DocumentBlock block = selectedWordSemanticBlock();
        if (block == null) return;
        String descriptionText = block.metadata().getOrDefault("description", "").strip();
        String source = block.metadata().getOrDefault("descriptionSource", "").strip();
        String state = block.metadata().getOrDefault("descriptionState", "").strip();

        Dialog<Boolean> dialog = StudioDialogShell.dialog();
        DialogStyler.apply(dialog,
                getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Contenido y descripción");
        Label detectedHeading = new Label("Texto asociado al componente");
        detectedHeading.getStyleClass().add("document-context-field-label");
        TextArea detected = StudioFormControls.textArea(block.text());
        detected.setEditable(false);
        detected.setWrapText(true);
        detected.setPrefRowCount(4);
        detected.setAccessibleText("Texto asociado al componente");
        VBox content = new VBox(8, detectedHeading, detected);
        content.getStyleClass().add("document-content-description-dialog");
        content.setPrefWidth(680);

        if (!descriptionText.isBlank()) {
            Label descriptionHeading = new Label("Descripción para la lectura");
            descriptionHeading.getStyleClass().add("document-context-field-label");
            TextArea description = StudioFormControls.textArea(descriptionText);
            description.setEditable(false);
            description.setWrapText(true);
            description.setPrefRowCount(5);
            description.setAccessibleText("Descripción para la lectura");
            Label provenance = new Label(source.startsWith("manual")
                    ? "Definida por ti"
                    : "Preparada localmente desde la imagen completa");
            provenance.getStyleClass().add("document-context-copy");
            content.getChildren().addAll(descriptionHeading, description, provenance);
        }
        Label technical = new Label("Bloque " + block.id()
                + "\nTipo: " + block.type().name()
                + (state.isBlank() ? "" : "\nEstado: " + state)
                + (source.isBlank() ? "" : "\nPreparador: " + source));
        technical.setWrapText(true);
        TitledPane details = StudioAccordion.pane("Detalles técnicos", technical);
        details.setExpanded(false);
        content.getChildren().add(details);

        ButtonType edit = NativeDialogResponse.button(
                descriptionText.isBlank() ? "Definir descripción" : "Editar descripción",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(640);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().getButtonTypes().setAll(edit, ButtonType.CLOSE);
        dialog.setResultConverter(button -> button == edit);
        dialog.setResizable(true);
        dialog.showAndWait().filter(Boolean::booleanValue)
                .ifPresent(ignored -> defineManualWordDescription());
    }

    private DocumentBlock selectedWordSemanticBlock() {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        String blockId = viewModel.selectedDocumentBlockIdProperty().get();
        if (document == null || blockId == null || blockId.isBlank()) return null;
        return document.blockById(blockId)
                .filter(block -> block.type() == DocumentBlockType.IMAGE_NOTICE
                        || block.type() == DocumentBlockType.TABLE_NOTICE)
                .orElse(null);
    }

    private static boolean visibleDescription(PdfDerivedTreatment treatment) {
        if (treatment == null || treatment.derivedText().isBlank()) return false;
        if (treatment.state() == PdfDerivedTreatmentState.APPROVED) return true;
        return treatment.state() == PdfDerivedTreatmentState.DRAFT
                && new com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfSemanticNarrationSafetyValidator()
                .safeForAutomaticAdmission(treatment);
    }

    private static int descriptionPriority(PdfDerivedTreatment treatment) {
        if (treatment == null) return 0;
        if (treatment.metadata().containsKey("manual")
                || treatment.modelId().startsWith("manual")) return 3;
        if (treatment.metadata().containsKey("technicalElementId")) return 2;
        return 1;
    }

    private String manualDescriptionSeed() {
        if (activePdfTreatment == null
                || activePdfTreatment.state() == PdfDerivedTreatmentState.REJECTED
                || activePdfTreatment.kind()
                == PdfDerivedTreatmentKind.CONTEXTUAL_CORRECTION) {
            return "";
        }
        return activePdfTreatment.derivedText();
    }

    private boolean currentPdfSelectionMatches(Path projectRoot, int pageNumber,
                                               String regionId) {
        var source = viewModel.currentPreparedPdfSourceProperty().get();
        var selection = viewModel.selectedPdfRegionProperty().get();
        return source != null && selection != null
                && source.workspace().projectRoot().toAbsolutePath().normalize()
                .equals(projectRoot)
                && selection.pageNumber() == pageNumber
                && selection.regionId().equals(regionId);
    }

    private void showPdfTreatment(PdfDerivedTreatment treatment) {
        if (treatment == null) return;
        pdfAnalysisResult.setText(treatment.derivedText());
        pdfAnalysisStatus.setText(switch (treatment.state()) {
            case DRAFT -> com.marcosmoreiradev.docupodcaststudio.application.document
                    .PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION
                    .equals(treatment.metadata().get("automaticAdmission"))
                    ? "Borrador Qwen Q8 validado para la política activa."
                    : "Borrador pendiente; se omitirá hasta su aprobación.";
            case APPROVED -> treatment.metadata().containsKey("manual")
                    ? "Descripción manual guardada para la lectura."
                    : "Descripción local guardada para la lectura.";
            case REJECTED -> "Propuesta rechazada; se conserva como evidencia.";
            case STALE -> "Propuesta desactualizada por cambios en la región.";
        });
    }

    private void updatePdfAnalysisControls() {
        boolean available = pdfAnalysisSection.isManaged() && !pdfAnalysisRunning;
        pdfAnalysisProgress.setVisible(pdfAnalysisRunning);
        pdfAnalysisProgress.setManaged(pdfAnalysisRunning);
        pdfAnalysisProgress.setProgress(pdfAnalysisRunning ? ProgressBar.INDETERMINATE_PROGRESS : 0.0);
        String type = viewModel.resolveCurrentDocumentSelection()
                .map(value -> value.type().toUpperCase(java.util.Locale.ROOT))
                .orElse("");
        setShown(visualReviewActions,
                "IMAGE".equals(type) || "CAPTION".equals(type));
        setShown(mathReviewActions, "MATH".equals(type));
        setShown(tableReviewActions, "TABLE".equals(type));
        setShown(textReviewActions,
                !type.isBlank() && !"IMAGE".equals(type)
                        && !"CAPTION".equals(type)
                        && !"MATH".equals(type)
                        && !"TABLE".equals(type));
        describePdfRegion.setDisable(!available);
        readPdfFormula.setDisable(!available);
        boolean table = "TABLE".equals(type);
        preparePdfTable.setDisable(!available || !table);
        recognizePdfTableStructure.setDisable(!available || !table);
        editPdfTableCells.setDisable(!available || !table);
        selectPdfTableRange.setDisable(!available || !table);
        explainPdfTable.setDisable(!available || !table);
        correctPdfRegion.setDisable(!available);
        reviewPageNarratability.setDisable(!available);
        defineManualDescription.setDisable(!available);
        boolean reviewable = available && activePdfTreatment != null
                && activePdfTreatment.state() == PdfDerivedTreatmentState.DRAFT;
        approvePdfTreatment.setDisable(!reviewable);
        rejectPdfTreatment.setDisable(!reviewable);
        copyPdfTreatmentDetails.setDisable(activePdfTreatment == null);
        regeneratePdfTreatment.setDisable(!available || activePdfTreatment == null);
        reviewPageDrafts.setDisable(!available);
        reviewDocumentDrafts.setDisable(!available);
        setShown(pdfAnalysisResult, activePdfTreatment != null);
        setShown(reviewResultActions, activePdfTreatment != null
                && activePdfTreatment.state() == PdfDerivedTreatmentState.DRAFT);
    }

    private void refreshListeningPreferences() {
        refreshingListeningPreferences = true;
        try {
            DocumentListeningPreferences preferences =
                    viewModel.documentListeningPreferences();
            semanticReadingPolicy.setValue(
                    preferences.secondarySemanticPolicy());
            semanticReadingPolicyNotice.setText(
                    preferences.secondarySemanticPolicy().description());
            refreshWordTableReadingControl();
            DocumentTranslationPreferences translation =
                    viewModel.documentTranslationPreferences();
            documentTranslationEnabled.setSelected(translation.enabled());
            documentListeningLanguage.setValue(translation.listeningLanguage());
            documentListeningLanguage.setDisable(!translation.enabled());
            documentTranslationNotice.setText(translation.enabled()
                    ? "Solo cambia la narración y el audio. PDF, capturas, tablas, "
                    + "imágenes, fórmulas y grounding permanecen originales."
                    : "Sin traducción: lectura y visuales permanecen en el idioma original.");
        } finally {
            refreshingListeningPreferences = false;
        }
    }

    private static void setShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void openDocumentAiSettings() {
        new SettingsDialog(viewModel.administrationWorkspace().capabilities())
                .showVoiceEngines(getScene() == null ? null : getScene().getWindow(),
                        viewModel.administrationWorkspace().settings());
    }

    private void copyPdfTreatmentDetails() {
        if (activePdfTreatment == null) return;
        StringBuilder details = new StringBuilder()
                .append("id=").append(activePdfTreatment.id()).append('\n')
                .append("kind=").append(activePdfTreatment.kind()).append('\n')
                .append("state=").append(activePdfTreatment.state()).append('\n')
                .append("model=").append(activePdfTreatment.modelId()).append('\n')
                .append("confidence=").append(activePdfTreatment.confidence()).append('\n')
                .append("sourceFingerprint=").append(
                        activePdfTreatment.sourceFingerprint()).append('\n');
        activePdfTreatment.metadata().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> details.append(entry.getKey()).append('=')
                        .append(entry.getValue()).append('\n'));
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(details.toString());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        pdfAnalysisStatus.setText("Detalles técnicos copiados.");
    }

    private static EngineDiagnosticCode engineFailureCode(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof EngineExecutionException engineFailure) {
                return engineFailure.code();
            }
            current = current.getCause();
        }
        return null;
    }

    private static String contextText(PreparedPdfRegionContext context, String selected) {
        if (context == null) return selected == null ? "" : selected;
        StringBuilder value = new StringBuilder(
                "CONTEXTO ORIENTATIVO; NO ES EVIDENCIA VISUAL.\n");
        context.before().stream().skip(Math.max(0, context.before().size() - 3L))
                .forEach(item -> value.append("ANTERIOR: ")
                        .append(item.text()).append('\n'));
        context.after().stream().limit(1).forEach(item -> value
                .append("POSTERIOR: ").append(item.text()).append('\n'));
        return value.toString().strip();
    }

    private static String sourceLanguage(String text) {
        String value = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        return value.matches(".*\\b(el|la|de|que|para|con|una|los|las)\\b.*") ? "es" : "en";
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
                CheckBox check = StudioFormControls.checkBox(option.displayName());
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
        button.disableProperty().bind(
                viewModel.documentSelectionValidProperty().not());
        return button;
    }

}
