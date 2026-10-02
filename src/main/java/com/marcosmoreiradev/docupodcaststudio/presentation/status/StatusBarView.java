package com.marcosmoreiradev.docupodcaststudio.presentation.status;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationProgress;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentReadingReadinessSnapshot;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Locale;
import java.util.function.IntConsumer;

/** Bottom feedback bar for user-visible status messages and reading comfort controls. */
public final class StatusBarView extends HBox {
    // Historical label retained for source guardrails; visible UX now says "Mostrar detalles de generación".
    private static final String LEGACY_PREPARATION_LABEL = "Mostrar preparación";
    /** Compatibility constructor for hosts that have not adopted document intervals yet. */
    /** Compatibility overload for callers predating persisted-chunk availability. */
    /** Compatibility overload for callers predating reading-readiness exposure. */
    public StatusBarView(
            ObservableValue<String> statusMessage,
            IntegerProperty readingFontSize,
            Runnable decreaseReadingSize,
            Runnable resetReadingSize,
            Runnable increaseReadingSize,
            IntConsumer setReadingSize,
            ObservableValue<AudioJobStatusDto> activeAudioJobStatus,
            ObservableValue<PdfPreparationProgress> pdfPreparationProgress,
            ObservableValue<Boolean> localDocumentAnalysisRunning,
            ObservableValue<ReadableDocument> currentDocument,
            ObservableValue<PreparedPdfSource> currentPreparedPdfSource,
            ObservableValue<String> selectedDocumentBlockId,
            ObservableValue<Number> pdfVisualDocumentProgress,
            BooleanProperty processOverlayExpanded,
            ObservableValue<Boolean> audioOperationRunning,
            ObservableValue<Boolean> audioGenerationAvailable,
            ObservableValue<DocumentProcessingScope> documentProcessingScope,
            ObservableValue<Boolean> documentProcessingIntervalSupported,
            ObservableValue<Boolean> documentProcessingIntervalValid,
            ObservableValue<Boolean> managedAudioChunksAvailable,
            Runnable startAudioGeneration,
            Runnable startAudioGenerationFromSelection,
            Runnable startIntervalProcessing,
            Runnable resumeAudioGeneration,
            Runnable cancelAudioGeneration,
            Runnable deleteAllAudioChunks,
            Runnable configureOcr) {
        this(statusMessage, readingFontSize, decreaseReadingSize, resetReadingSize,
                increaseReadingSize, setReadingSize, activeAudioJobStatus,
                pdfPreparationProgress, localDocumentAnalysisRunning, currentDocument,
                currentPreparedPdfSource, selectedDocumentBlockId,
                pdfVisualDocumentProgress, processOverlayExpanded,
                audioOperationRunning, audioGenerationAvailable,
                documentProcessingScope, documentProcessingIntervalSupported,
                documentProcessingIntervalValid, managedAudioChunksAvailable,
                new javafx.beans.property.SimpleObjectProperty<>(
                        DocumentReadingReadinessSnapshot.unavailable("")),
                startAudioGeneration, startAudioGenerationFromSelection,
                startIntervalProcessing, resumeAudioGeneration, cancelAudioGeneration,
                deleteAllAudioChunks, configureOcr);
    }

    public StatusBarView(
            ObservableValue<String> statusMessage,
            IntegerProperty readingFontSize,
            Runnable decreaseReadingSize,
            Runnable resetReadingSize,
            Runnable increaseReadingSize,
            IntConsumer setReadingSize,
            ObservableValue<AudioJobStatusDto> activeAudioJobStatus,
            ObservableValue<PdfPreparationProgress> pdfPreparationProgress,
            ObservableValue<Boolean> localDocumentAnalysisRunning,
            ObservableValue<ReadableDocument> currentDocument,
            ObservableValue<PreparedPdfSource> currentPreparedPdfSource,
            ObservableValue<String> selectedDocumentBlockId,
            ObservableValue<Number> pdfVisualDocumentProgress,
            BooleanProperty processOverlayExpanded,
            ObservableValue<Boolean> audioOperationRunning,
            ObservableValue<Boolean> audioGenerationAvailable,
            ObservableValue<DocumentProcessingScope> documentProcessingScope,
            ObservableValue<Boolean> documentProcessingIntervalSupported,
            ObservableValue<Boolean> documentProcessingIntervalValid,
            Runnable startAudioGeneration,
            Runnable startAudioGenerationFromSelection,
            Runnable startIntervalProcessing,
            Runnable resumeAudioGeneration,
            Runnable cancelAudioGeneration,
            Runnable deleteAllAudioChunks,
            Runnable configureOcr) {
        this(statusMessage, readingFontSize, decreaseReadingSize, resetReadingSize,
                increaseReadingSize, setReadingSize, activeAudioJobStatus,
                pdfPreparationProgress, localDocumentAnalysisRunning, currentDocument,
                currentPreparedPdfSource, selectedDocumentBlockId,
                pdfVisualDocumentProgress, processOverlayExpanded,
                audioOperationRunning, audioGenerationAvailable, documentProcessingScope,
                documentProcessingIntervalSupported, documentProcessingIntervalValid,
                new javafx.beans.property.SimpleBooleanProperty(false),
                new javafx.beans.property.SimpleObjectProperty<>(
                        DocumentReadingReadinessSnapshot.unavailable("")),
                startAudioGeneration, startAudioGenerationFromSelection,
                startIntervalProcessing, resumeAudioGeneration, cancelAudioGeneration,
                deleteAllAudioChunks, configureOcr);
    }

    public StatusBarView(
            ObservableValue<String> statusMessage,
            IntegerProperty readingFontSize,
            Runnable decreaseReadingSize,
            Runnable resetReadingSize,
            Runnable increaseReadingSize,
            IntConsumer setReadingSize,
            ObservableValue<AudioJobStatusDto> activeAudioJobStatus,
            ObservableValue<PdfPreparationProgress> pdfPreparationProgress,
            ObservableValue<Boolean> localDocumentAnalysisRunning,
            ObservableValue<ReadableDocument> currentDocument,
            ObservableValue<PreparedPdfSource> currentPreparedPdfSource,
            ObservableValue<String> selectedDocumentBlockId,
            ObservableValue<Number> pdfVisualDocumentProgress,
            BooleanProperty processOverlayExpanded,
            ObservableValue<Boolean> audioOperationRunning,
            ObservableValue<Boolean> audioGenerationAvailable,
            Runnable startAudioGeneration,
            Runnable startAudioGenerationFromSelection,
            Runnable resumeAudioGeneration,
            Runnable cancelAudioGeneration,
            Runnable deleteAllAudioChunks,
            Runnable configureOcr) {
        this(statusMessage, readingFontSize, decreaseReadingSize, resetReadingSize,
                increaseReadingSize, setReadingSize, activeAudioJobStatus,
                pdfPreparationProgress, localDocumentAnalysisRunning, currentDocument,
                currentPreparedPdfSource, selectedDocumentBlockId,
                pdfVisualDocumentProgress, processOverlayExpanded,
                audioOperationRunning, audioGenerationAvailable,
                new javafx.beans.property.SimpleObjectProperty<>(
                        DocumentProcessingScope.FROM_SELECTION),
                new javafx.beans.property.SimpleBooleanProperty(false),
                new javafx.beans.property.SimpleBooleanProperty(false),
                new javafx.beans.property.SimpleBooleanProperty(false),
                new javafx.beans.property.SimpleObjectProperty<>(
                        DocumentReadingReadinessSnapshot.unavailable("")),
                startAudioGeneration, startAudioGenerationFromSelection, null,
                resumeAudioGeneration, cancelAudioGeneration, deleteAllAudioChunks,
                configureOcr);
    }

    public StatusBarView(
            ObservableValue<String> statusMessage,
            IntegerProperty readingFontSize,
            Runnable decreaseReadingSize,
            Runnable resetReadingSize,
            Runnable increaseReadingSize,
            IntConsumer setReadingSize,
            ObservableValue<AudioJobStatusDto> activeAudioJobStatus,
            ObservableValue<PdfPreparationProgress> pdfPreparationProgress,
            ObservableValue<Boolean> localDocumentAnalysisRunning,
            ObservableValue<ReadableDocument> currentDocument,
            ObservableValue<PreparedPdfSource> currentPreparedPdfSource,
            ObservableValue<String> selectedDocumentBlockId,
            ObservableValue<Number> pdfVisualDocumentProgress,
            BooleanProperty processOverlayExpanded,
            ObservableValue<Boolean> audioOperationRunning,
            ObservableValue<Boolean> audioGenerationAvailable,
            ObservableValue<DocumentProcessingScope> documentProcessingScope,
            ObservableValue<Boolean> documentProcessingIntervalSupported,
            ObservableValue<Boolean> documentProcessingIntervalValid,
            ObservableValue<Boolean> managedAudioChunksAvailable,
            ObservableValue<DocumentReadingReadinessSnapshot>
                    fullDocumentReadingReadiness,
            Runnable startAudioGeneration,
            Runnable startAudioGenerationFromSelection,
            Runnable startIntervalProcessing,
            Runnable resumeAudioGeneration,
            Runnable cancelAudioGeneration,
            Runnable deleteAllAudioChunks,
            Runnable configureOcr
    ) {
        getStyleClass().add("status-bar");
        setPadding(new Insets(4, 10, 4, 10));
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);

        Label prefix = new Label("Estado");
        prefix.getStyleClass().add("status-bar-prefix");
        keepReadable(prefix);
        Label label = new Label();
        label.textProperty().bind(statusMessage);
        label.getStyleClass().add("status-bar-label");
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(label, new Tooltip("Usa la rueda del mouse o desplaza horizontalmente sobre este mensaje para leer el estado completo."));
        ScrollPane statusScroller = StudioViewportControls.scrollPane(label);
        statusScroller.getStyleClass().addAll("status-bar-scroll", "status-bar-scroll-hidden");
        statusScroller.setFitToHeight(true);
        statusScroller.setFitToWidth(false);
        statusScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        statusScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        statusScroller.setPannable(true);
        statusScroller.setMinWidth(80);
        statusScroller.setPrefViewportWidth(420);
        HBox.setHgrow(statusScroller, Priority.ALWAYS);
        statusMessage.addListener((obs, oldValue, newValue) -> refreshScrollableStatusWidth(label, statusScroller));
        statusScroller.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> refreshScrollableStatusWidth(label, statusScroller));
        statusScroller.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            double delta = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? event.getDeltaX() : event.getDeltaY();
            if (Math.abs(delta) > 0.1) {
                double next = Math.max(0.0, Math.min(1.0, statusScroller.getHvalue() - delta / 900.0));
                statusScroller.setHvalue(next);
                event.consume();
            }
        });
        javafx.application.Platform.runLater(() -> refreshScrollableStatusWidth(label, statusScroller));
        Button configureOcrButton = ActionButtonFactory.primary("Configurar OCR", configureOcr);
        configureOcrButton.getStyleClass().add("status-generation-button");
        configureOcrButton.setMinWidth(Region.USE_PREF_SIZE);
        configureOcrButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(configureOcrButton, new Tooltip("Abrir Configuración para preparar la extracción de texto documental."));
        Runnable updateOcrButton = () -> {
            String text = statusMessage == null || statusMessage.getValue() == null
                    ? ""
                    : statusMessage.getValue();
            boolean show = configureOcr != null && text.toLowerCase(Locale.ROOT).contains("ocr no configurado");
            configureOcrButton.setVisible(show);
            configureOcrButton.setManaged(show);
        };
        if (statusMessage != null) {
            statusMessage.addListener((obs, oldValue, newValue) -> updateOcrButton.run());
        }
        updateOcrButton.run();
        Button processButton = ActionButtonFactory.secondary("Mostrar detalles de generación", () -> processOverlayExpanded.set(true));
        processButton.getStyleClass().add("status-process-button");
        processButton.setMinWidth(Region.USE_PREF_SIZE);
        processButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(processButton, new Tooltip(LEGACY_PREPARATION_LABEL
                + ": volver a mostrar la generación de voz, el análisis local "
                + "o la preparación del documento."));

        Button startGenerationButton = ActionButtonFactory.secondary("Procesar lectura completa", () -> {
            if (startAudioGeneration != null) {
                startAudioGeneration.run();
            }
        });
        startGenerationButton.getStyleClass().add("status-generation-button");
        startGenerationButton.setMinWidth(Region.USE_PREF_SIZE);
        startGenerationButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(startGenerationButton, new Tooltip(
                "Preparar la semántica y la narración efectiva, reutilizar el audio vigente "
                        + "y generar todos los WAV faltantes sin iniciar reproducción."));

        Button selectedGenerationButton = ActionButtonFactory.primary("Procesar desde aquí", () -> {
            if (processOverlayExpanded != null) {
                processOverlayExpanded.set(true);
            }
            if (startAudioGenerationFromSelection != null) {
                startAudioGenerationFromSelection.run();
            }
        });
        selectedGenerationButton.getStyleClass().add("status-generation-button");
        selectedGenerationButton.setMinWidth(Region.USE_PREF_SIZE);
        selectedGenerationButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(selectedGenerationButton, new Tooltip("Generar audio desde la oración o bloque seleccionado para estudiar desde ese punto."));

        Button intervalGenerationButton = ActionButtonFactory.primary(
                "Procesar intervalo", () -> {
                    if (processOverlayExpanded != null) {
                        processOverlayExpanded.set(true);
                    }
                    if (startIntervalProcessing != null) {
                        startIntervalProcessing.run();
                    }
                });
        intervalGenerationButton.getStyleClass().add("status-generation-button");
        intervalGenerationButton.setMinWidth(Region.USE_PREF_SIZE);
        intervalGenerationButton.setMaxWidth(Region.USE_PREF_SIZE);
        StudioFormControls.installTooltip(intervalGenerationButton,
                "Preparar y generar audio para el intervalo de páginas PDF o bloques Word configurado.");

        Button resumeGenerationButton = ActionButtonFactory.secondary("Seguir generando", () -> {
            if (processOverlayExpanded != null) {
                processOverlayExpanded.set(true);
            }
            if (resumeAudioGeneration != null) {
                resumeAudioGeneration.run();
            }
        });
        resumeGenerationButton.getStyleClass().add("status-generation-button");
        resumeGenerationButton.setMinWidth(Region.USE_PREF_SIZE);
        resumeGenerationButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(resumeGenerationButton, new Tooltip("Reanudar el job cancelado o incompleto conservando los fragmentos de audio ya generados."));

        Button cancelGenerationButton = ActionButtonFactory.danger("Cancelar generación", cancelAudioGeneration);
        cancelGenerationButton.getStyleClass().add("status-generation-danger-button");
        cancelGenerationButton.setMinWidth(Region.USE_PREF_SIZE);
        cancelGenerationButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(cancelGenerationButton, new Tooltip("Cancelar solo la generación de audio. La reproducción se controla desde la barra flotante."));

        Button deleteChunksButton = ActionButtonFactory.danger("Eliminar todos los chunks de audio", deleteAllAudioChunks);
        deleteChunksButton.getStyleClass().add("status-generation-danger-button");
        deleteChunksButton.setMinWidth(Region.USE_PREF_SIZE);
        deleteChunksButton.setMaxWidth(Region.USE_PREF_SIZE);
        Tooltip.install(deleteChunksButton, new Tooltip("Eliminar todos los jobs, chunks y manifiestos de audio generados para este proyecto."));

        Runnable updateProcessButton = () -> {
            AudioJobStatusDto dto = activeAudioJobStatus == null ? null : activeAudioJobStatus.getValue();
            boolean audioRunning = (dto != null && dto.running())
                    || (audioOperationRunning != null
                    && Boolean.TRUE.equals(audioOperationRunning.getValue()));
            PdfPreparationProgress pdfProgress = pdfPreparationProgress == null
                    ? null : pdfPreparationProgress.getValue();
            boolean preparationRunning = preparationActive(pdfProgress);
            boolean analysisRunning = localDocumentAnalysisRunning != null
                    && Boolean.TRUE.equals(localDocumentAnalysisRunning.getValue());
            boolean running = audioRunning || preparationRunning || analysisRunning;
            boolean showButton = running && processOverlayExpanded != null
                    && !processOverlayExpanded.get();
            processButton.setVisible(showButton);
            processButton.setManaged(showButton);
            if (analysisRunning) {
                processButton.setText("Detalles · análisis local");
            } else if (audioRunning && dto != null && dto.totalSegments() > 0) {
                processButton.setText("Detalles · " + dto.segmentCounterLabel());
            } else if (audioRunning) {
                processButton.setText("Detalles · procesamiento");
            } else if (preparationRunning) {
                processButton.setText("Detalles · preparación");
            } else {
                processButton.setText("Detalles");
            }
            SemanticActionIcons.refresh(processButton, processButton.getText());
            processButton.setAccessibleText(processButton.getText()
                    + ". Mostrar el panel de procesos.");
            boolean canCancel = audioRunning && cancelAudioGeneration != null;
            cancelGenerationButton.setVisible(canCancel);
            cancelGenerationButton.setManaged(canCancel);
            boolean canDeleteChunks = !audioRunning
                    && deleteAllAudioChunks != null
                    && managedAudioChunksAvailable != null
                    && Boolean.TRUE.equals(managedAudioChunksAvailable.getValue());
            deleteChunksButton.setVisible(canDeleteChunks);
            deleteChunksButton.setManaged(canDeleteChunks);
            boolean canStart = startAudioGeneration != null
                    && (audioGenerationAvailable == null || Boolean.TRUE.equals(audioGenerationAvailable.getValue()));
            startGenerationButton.setVisible(canStart);
            startGenerationButton.setManaged(canStart);
            startGenerationButton.setDisable(running);
            DocumentReadingReadinessSnapshot readingReadiness =
                    fullDocumentReadingReadiness == null
                            ? null : fullDocumentReadingReadiness.getValue();
            startGenerationButton.setText(readingReadiness == null
                    ? "Procesar lectura completa"
                    : readingReadiness.completeReadingActionLabel());
            SemanticActionIcons.refresh(startGenerationButton,
                    startGenerationButton.getText());
            boolean canStartFromSelection = !audioRunning
                    && startAudioGenerationFromSelection != null
                    && selectedDocumentBlockId != null
                    && selectedDocumentBlockId.getValue() != null
                    && !selectedDocumentBlockId.getValue().isBlank()
                    && (audioGenerationAvailable == null || Boolean.TRUE.equals(audioGenerationAvailable.getValue()));
            selectedGenerationButton.setVisible(canStartFromSelection);
            selectedGenerationButton.setManaged(canStartFromSelection);
            boolean intervalSelected = documentProcessingScope != null
                    && documentProcessingScope.getValue()
                    == DocumentProcessingScope.INTERVAL;
            boolean intervalSupported = documentProcessingIntervalSupported != null
                    && Boolean.TRUE.equals(documentProcessingIntervalSupported.getValue());
            boolean showInterval = intervalSelected && intervalSupported;
            intervalGenerationButton.setVisible(showInterval);
            intervalGenerationButton.setManaged(showInterval);
            intervalGenerationButton.setDisable(running
                    || startIntervalProcessing == null
                    || documentProcessingIntervalValid == null
                    || !Boolean.TRUE.equals(documentProcessingIntervalValid.getValue())
                    || (audioGenerationAvailable != null
                    && !Boolean.TRUE.equals(audioGenerationAvailable.getValue())));
            boolean canResume = dto != null
                    && !running
                    && resumeAudioGeneration != null
                    && !dto.jobId().isBlank()
                    && dto.totalSegments() > 0
                    && dto.completedSegments() < dto.totalSegments();
            resumeGenerationButton.setVisible(canResume);
            resumeGenerationButton.setManaged(canResume);
        };
        if (activeAudioJobStatus != null) {
            activeAudioJobStatus.addListener((obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (audioOperationRunning != null) {
            audioOperationRunning.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (pdfPreparationProgress != null) {
            pdfPreparationProgress.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (localDocumentAnalysisRunning != null) {
            localDocumentAnalysisRunning.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (processOverlayExpanded != null) {
            processOverlayExpanded.addListener((obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (audioGenerationAvailable != null) {
            audioGenerationAvailable.addListener((obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (documentProcessingScope != null) {
            documentProcessingScope.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (selectedDocumentBlockId != null) {
            selectedDocumentBlockId.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (documentProcessingIntervalSupported != null) {
            documentProcessingIntervalSupported.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (documentProcessingIntervalValid != null) {
            documentProcessingIntervalValid.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (managedAudioChunksAvailable != null) {
            managedAudioChunksAvailable.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        if (fullDocumentReadingReadiness != null) {
            fullDocumentReadingReadiness.addListener(
                    (obs, oldValue, newValue) -> updateProcessButton.run());
        }
        updateProcessButton.run();

        Label documentProgress = new Label(documentProgressLabel(
                currentDocument == null ? null : currentDocument.getValue(),
                currentPreparedPdfSource != null && currentPreparedPdfSource.getValue() != null,
                selectedDocumentBlockId == null ? "" : selectedDocumentBlockId.getValue(),
                pdfVisualProgress(pdfVisualDocumentProgress)));
        documentProgress.getStyleClass().add("status-document-progress");
        keepReadable(documentProgress);
        Tooltip.install(documentProgress, new Tooltip("En PDF se calcula por la posicion del scroll; en otros documentos, por el bloque seleccionado."));
        Runnable refreshDocumentProgress = () -> documentProgress.setText(documentProgressLabel(
                currentDocument == null ? null : currentDocument.getValue(),
                currentPreparedPdfSource != null && currentPreparedPdfSource.getValue() != null,
                selectedDocumentBlockId == null ? "" : selectedDocumentBlockId.getValue(),
                pdfVisualProgress(pdfVisualDocumentProgress)));
        if (currentDocument != null) {
            currentDocument.addListener((obs, oldValue, newValue) -> refreshDocumentProgress.run());
        }
        if (currentPreparedPdfSource != null) {
            currentPreparedPdfSource.addListener((obs, oldValue, newValue) ->
                    refreshDocumentProgress.run());
        }
        if (selectedDocumentBlockId != null) {
            selectedDocumentBlockId.addListener((obs, oldValue, newValue) -> refreshDocumentProgress.run());
        }
        if (pdfVisualDocumentProgress != null) {
            pdfVisualDocumentProgress.addListener((obs, oldValue, newValue) -> refreshDocumentProgress.run());
        }

        ReadingZoomControl zoomControl = new ReadingZoomControl(
                readingFontSize,
                decreaseReadingSize,
                resetReadingSize,
                increaseReadingSize,
                setReadingSize);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(prefix, statusScroller, configureOcrButton, selectedGenerationButton,
                intervalGenerationButton,
                startGenerationButton, resumeGenerationButton, cancelGenerationButton, deleteChunksButton,
                processButton, spacer, documentProgress, zoomControl);
    }

    private static boolean preparationActive(PdfPreparationProgress progress) {
        return progress != null
                && (progress.state() == PdfPreparationProgress.State.RUNNING
                || progress.state() == PdfPreparationProgress.State.PAUSED
                || progress.queued() > 0);
    }

    private static void keepReadable(Label label) {
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private static String documentProgressLabel(ReadableDocument document, boolean preparedPdf,
                                                String selectedBlockId, double pdfVisualProgress) {
        if (preparedPdf) {
            int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, pdfVisualProgress)) * 100.0);
            return "Documento " + percent + "%";
        }
        if (document == null || document.blocks().isEmpty()) {
            return "Documento —";
        }
        String blockId = selectedBlockId == null ? "" : selectedBlockId.strip();
        if (blockId.isBlank()) {
            return "Documento 0%";
        }
        int index = -1;
        java.util.List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).id().equals(blockId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return "Documento 0%";
        }
        int percent = blocks.size() <= 1 ? 100 : (int) Math.round(index * 100.0 / (blocks.size() - 1));
        percent = Math.max(0, Math.min(100, percent));
        return "Documento " + percent + "%";
    }

    private static double pdfVisualProgress(ObservableValue<Number> progress) {
        Number value = progress == null ? null : progress.getValue();
        return value == null ? 0.0 : value.doubleValue();
    }

    private static void refreshScrollableStatusWidth(Label label, ScrollPane scroller) {
        if (label == null || scroller == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            double viewport = Math.max(1.0, scroller.getViewportBounds().getWidth());
            double preferred = Math.max(viewport + 80.0, label.prefWidth(-1) + 48.0);
            label.setMinWidth(preferred);
            label.setPrefWidth(preferred);
        });
    }

}
