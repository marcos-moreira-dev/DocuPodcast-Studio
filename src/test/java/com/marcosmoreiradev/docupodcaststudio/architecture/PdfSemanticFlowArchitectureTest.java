package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Product guardrails for the unified secondary-semantic PDF flow. */
final class PdfSemanticFlowArchitectureTest {
    @Test
    void productionCompositionUsesTheBlockV1ParserExclusively() throws Exception {
        String composition = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/bootstrap/WorkspaceCompositionFactory.java");

        assertTrue(composition.contains("new BlockPdfSemanticPageResponseParser()"));
        assertFalse(composition.contains("new StructuredPdfSemanticPageResponseParser()"));
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/infrastructure/document/"
                + "StructuredPdfSemanticPageResponseParser.java")));
        String reader = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/application/document/"
                + "AnalyzePdfPageSemanticallyUseCase.java");
        assertFalse(reader.contains("RESPONSE_SCHEMA"));
        assertFalse(reader.contains("VERIFICATION_SCHEMA"));
        assertTrue(reader.contains("TRANSPORT_VERSION"));
    }

    @Test
    void targetedRecoveryNeverReadmitsCpuHeavyInsideTheQwenLease()
            throws Exception {
        String reader = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/application/document/"
                + "AnalyzePdfPageSemanticallyUseCase.java");
        String recoveryCrop = method(reader,
                "private PdfPageRenderResult renderRecoveryCrop(",
                "private ContentAnalysisRequest targetedRecoveryRequest(");

        assertTrue(recoveryCrop.contains("renderEngine.renderCrop"));
        assertFalse(recoveryCrop.contains("resourceScheduler().acquire"));
        assertFalse(recoveryCrop.contains("ResourceId.CPU_HEAVY"));
    }

    @Test
    void pdfPanelExposesTheCompactVocabularyAndNoQ4Selector() throws Exception {
        String panel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "DocumentContextDetailsPanel.java");
        String status = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/status/StatusBarView.java");

        assertTrue(panel.contains("Lectura de componentes semánticos secundarios"));
        assertTrue(panel.contains("Escuchar desde aquí"));
        assertTrue(panel.contains("Procesar este fragmento"));
        assertTrue(panel.contains("Revisión avanzada"));
        assertFalse(panel.contains("qwen3-vl:4b-instruct-q4"));
        assertFalse(panel.contains("Cuadros y tablas durante la lectura"));
        assertTrue(status.contains("Procesar lectura completa"));
        assertFalse(status.contains("Reconstruir fragmentos de audio"));
    }

    @Test
    void emptyClickAndEscapeOnlyClearSelection() throws Exception {
        String view = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "PdfVisualDocumentView.java");
        int clickStart = view.indexOf("private void clickTextTarget(");
        int clickEnd = view.indexOf("private void showTextTargetContextMenu(",
                clickStart);
        String clickHandler = view.substring(clickStart, clickEnd);

        assertTrue(view.contains("event.getCode() != KeyCode.ESCAPE"));
        assertTrue(view.contains("rectangle.setFill(javafx.scene.paint.Color.TRANSPARENT)"));
        assertTrue(clickHandler.contains("clearPinnedTextTarget()"));
        assertTrue(clickHandler.contains("emptyTextTargetSelectionHandler.run()"));
        assertFalse(clickHandler.contains("requestTextPreparation("));
    }

    @Test
    void contextMenuHasOneNarrationEntryAndBlankAreasDismissEverything()
            throws Exception {
        String view = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "PdfVisualDocumentView.java");
        String factory = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "DocumentSelectionContextMenuFactory.java");
        String menu = method(view,
                "private void showTextTargetContextMenu(",
                "private boolean canHitTestText()");

        assertTrue(menu.contains("dismissTextTargetContextMenu()"));
        assertTrue(menu.contains("DocumentSelectionContextMenuFactory.create"));
        assertTrue(menu.contains("copyTargetText(target)"));
        assertTrue(factory.contains("Narrar desde aquí"));
        assertTrue(factory.contains("Copiar texto"));
        assertTrue(factory.contains("Ver contenido y descripción…"));
        assertTrue(menu.contains("clearPinnedTextTarget()"));
        assertTrue(menu.contains("emptyTextTargetSelectionHandler.run()"));
        assertTrue(view.contains("private ContextMenu activeTextTargetContextMenu"));
        assertTrue(method(view, "public void clearPinnedTextTarget()",
                "private void showHoverTextTarget(")
                .contains("dismissTextTargetContextMenu()"));
    }

    @Test
    void wordAndPdfUseTheSamePassiveContextMenuAndVideoVocabulary()
            throws Exception {
        String word = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdf = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String panel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/DocumentStudyVideoPanel.java");
        String settings = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/DocumentStudyVideoSettingsPanel.java");

        assertTrue(word.contains("DocumentSelectionContextMenuFactory.create"));
        assertTrue(pdf.contains("DocumentSelectionContextMenuFactory.create"));
        assertTrue(word.contains("ClipboardContent"));
        assertTrue(pdf.contains("ClipboardContent"));
        assertFalse(method(word, "private ContextMenu paragraphContextMenu(",
                "private String readableKind(").contains("prepare"));
        assertFalse(method(pdf, "private void showTextTargetContextMenu(",
                "private boolean canHitTestText()").contains("requestTextPreparation"));
        assertTrue(panel.contains(
                "Incluir extras narrables en diapositiva propia"));
        assertTrue(settings.contains(
                "Duración predeterminada de elementos semánticos secundarios:"));
    }

    @Test
    void narrationJumpIsGeneralPreservesOldJobsAndWaitsForPdfPreparation()
            throws Exception {
        String shell = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String coordinator = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/workflow/"
                + "PdfNarratablePreparationCoordinator.java");
        String audio = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/workflow/"
                + "AudioWorkflowCoordinator.java");
        String jump = method(shell,
                "private void handleNarrateFromPdfTarget(",
                "private void handlePlaySelection()");
        String priority = method(audio,
                "public CompletableFuture<String> interruptAndSubmitAsync(",
                "public CompletableFuture<Void> prepareFreshWorkspaceAsync(");

        assertTrue(jump.contains("viewModel.stopPlayback()"));
        assertTrue(jump.contains("reprioritizeFromSelectionThenRun"));
        assertTrue(jump.contains("viewModel.narrateFromSelectedPdfTarget()"));
        assertTrue(coordinator.contains("selectedPdfRegionProperty().get().pageNumber()"));
        assertTrue(coordinator.contains("PdfNarratablePreparationMode.FAST_LISTEN"));
        assertTrue(coordinator.contains("prepareFastWindow(start, Math.min(last, start + lookAhead - 1)"));
        assertTrue(priority.contains("cancelAndAwait"));
        assertFalse(priority.contains("deletePersistedJobs"));
    }

    @Test
    void exportAudioGapSubmissionPreservesReusableJobs() throws Exception {
        String viewModel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String submission = method(viewModel,
                "private void submitAudioGeneration(DocumentPlaybackIntent playbackIntent)",
                "public void submitMockAudioGeneration()");
        String narrationInvalidation = method(viewModel,
                "private void invalidatePersistedAudioForNarrationChange()",
                "private void submitFreshAudioRequestAsync(");

        assertTrue(submission.contains("reusableAudioCoverage.resolve"));
        assertTrue(submission.contains("retainGenerationUnits"));
        assertTrue(submission.contains("submitPriorityAudioRequestAsync"));
        assertFalse(submission.contains("submitFreshAudioRequestAsync"));
        assertFalse(submission.contains("deletePersistedJobs"));
        assertTrue(narrationInvalidation.contains("ReusableAudioCoverage"));
        assertFalse(narrationInvalidation.contains("deletePersistedAudioAsync"));
    }

    @Test
    void advancedReviewOnlyExistsForASelectionAndHidesInternalPipelineButtons()
            throws Exception {
        String panel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "DocumentContextDetailsPanel.java");
        String configure = method(panel,
                "private void configurePdfAnalysisSection()",
                "private void refreshPdfAnalysisSection()");
        String refresh = method(panel,
                "private void refreshPdfAnalysisSection()",
                "private static String capabilitySummary(");

        assertTrue(refresh.contains("pdfAdvancedReview.setVisible(visible)"));
        assertTrue(refresh.contains("pdfAdvancedReview.setExpanded(false)"));
        assertFalse(configure.contains("recognizePdfTableStructure"));
        assertFalse(configure.contains("selectPdfTableRange"));
        assertFalse(configure.contains("reviewPageNarratability"));
        assertTrue(configure.contains("approvePdfTreatment"));
        assertTrue(configure.contains("rejectPdfTreatment"));
        assertTrue(configure.contains("defineManualDescription"));
    }

    @Test
    void qwenSemanticRequestsAreQ8OnlyStatelessAndRequestFailuresDoNotHardUnload()
            throws Exception {
        String qwen = source("studio-local-media-adapters/src/main/java/com/"
                + "marcosmoreiradev/docupodcaststudio/localmedia/"
                + "QwenVisualAnalysisEngine.java");
        String administration = source(
                "studio-local-media-adapters/src/main/java/com/"
                        + "marcosmoreiradev/docupodcaststudio/localmedia/"
                        + "QwenVisualAnalysisAdministration.java");

        assertTrue(qwen.contains("String configuredModel = selectedModel()"));
        assertTrue(qwen.contains("if (!configuredModel.equals(requestedModel))"));
        assertTrue(qwen.contains("for (int attempt = 0; attempt < 2; attempt++)"));
        assertTrue(qwen.contains("process.openModelRequest(residencyKey, requestExecution)"));
        assertTrue(qwen.contains("retryable(failure.code())"));
        assertFalse(qwen.contains("process.unloadModel(Q8_MODEL, execution)"));
        assertFalse(qwen.contains("process.unloadModelBestEffort(model)"));
        assertTrue(qwen.contains("\\\"messages\\\":[{\\\"role\\\":\\\"user\\\""));
        assertTrue(qwen.contains(
                "request.options().getOrDefault(\"model\", configuredModel)"));
        assertFalse(qwen.toLowerCase(java.util.Locale.ROOT).contains("q4"));
        assertFalse(administration.toLowerCase(
                java.util.Locale.ROOT).contains("q4"));
        assertFalse(administration.contains(
                "new EngineConfigurationField(\"model\""));
    }

    @Test
    void completeProcessingNeverStartsPlaybackButListeningDoes()
            throws Exception {
        String shell = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String processHandler = method(shell,
                "private void handleGenerateChunksFromStatusBar()",
                "private void handleGenerateSelectedChunkFromStatusBar()");
        String listenHandler = method(shell,
                "private void handleListenDocument()",
                "private void handlePlaySelection()");
        String coordinator = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/workflow/"
                + "PdfNarratablePreparationCoordinator.java");

        assertTrue(processHandler.contains(
                "processCompleteReadingWithoutPlayback()"));
        assertFalse(processHandler.contains("runDocumentPrimaryAction()"));
        assertTrue(processHandler.contains("documentProcessingActive()"));
        assertTrue(processHandler.contains("Se mantiene el trabajo actual"));
        assertFalse(processHandler.contains("cancelAudioAndThen(start)"));
        assertTrue(listenHandler.contains("runDocumentPrimaryAction()"));
        assertTrue(listenHandler.contains("prepareFastListenThenRun"));
        assertTrue(listenHandler.contains("runPreparedPdfPrimaryActionFromPage"));
        assertTrue(coordinator.contains(
                "PdfNarratablePreparationMode.BACKGROUND_LOOK_AHEAD"));
        assertTrue(coordinator.contains(
                "prepareFastCandidate(candidate + 1, end, continuation)"));
    }

    @Test
    void playbackPauseResumeAndStopDoNotInvalidatePdfOrAudioCaches()
            throws Exception {
        String viewModel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String pause = method(viewModel, "public void pausePlayback()",
                "public void resumePlayback()");
        String resume = method(viewModel, "public void resumePlayback()",
                "public void setPlaybackRate(");
        String stop = method(viewModel, "public void stopPlayback()",
                "public void playDocumentFromBeginning()");

        assertTrue(pause.contains("playbackTransport.pause()"));
        assertTrue(resume.contains("playbackTransport.resume(local)"));
        assertTrue(stop.contains("resetPlaybackTransportOnly()"));
        assertFalse(pause.contains("deletePersistedJobs"));
        assertFalse(resume.contains("deletePersistedJobs"));
        assertFalse(stop.contains("deletePersistedJobs"));
        assertFalse(stop.contains("unloadModel"));
        assertFalse(stop.contains("preparedPdf"));
    }

    @Test
    void fullRestartWaitsForTheSubmittedJobIdAndRealWorkerTermination()
            throws Exception {
        String viewModel = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String workflow = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java");
        String gateway = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/infrastructure/audio/VoiceEngineAudioGenerationGateway.java");

        assertTrue(viewModel.contains("activeAudioSubmission"));
        assertTrue(viewModel.contains("currentAudioJobId()"));
        assertTrue(viewModel.contains("cancelKnownAudioJobAndThen"));
        assertTrue(workflow.contains("cancelAndAwaitAsync"));
        assertTrue(gateway.contains("AudioJobWorkspaceLockRegistry.writer"));
    }

    private static String method(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        return source.substring(from, to);
    }

    private static String source(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }
}
