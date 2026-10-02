package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentProcessingIntervalArchitectureTest {
    @Test
    void sidebarUsesTypedScopeAndTransversalSpinnerFactory() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "document",
                "DocumentContextDetailsPanel.java"));

        assertTrue(source.contains("ComboBox<DocumentProcessingScope>"));
        assertTrue(source.contains("DocumentProcessingScope.values()"));
        assertTrue(source.contains("StudioFormControls.spinner(1, 1, 1, 1)"));
        assertTrue(source.contains("new Label(\"Inicio\")"));
        assertTrue(source.contains("new Label(\"Fin\")"));
        assertFalse(source.contains("private enum AudioPreparationScopeChoice"));
        assertFalse(source.contains("new Spinner<"));
        assertFalse(source.contains("new ComboBox<"));
    }

    @Test
    void intervalRoutesThroughExistingPdfPageRangePipeline() throws Exception {
        String coordinator = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell", "workflow",
                "PdfNarratablePreparationCoordinator.java"));
        String shell = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "DocuPodcastShellView.java"));

        assertTrue(shell.contains("DocumentAudioAction.PROCESS_INTERVAL"));
        assertTrue(shell.contains("prepareIntervalThenRun"));
        assertTrue(coordinator.contains("PdfPreparationScope.PAGE_RANGE"));
        assertTrue(coordinator.contains("PdfPreparationOrigin.PROCESS_INTERVAL"));
        assertFalse(coordinator.contains("new PdfPagePreparationScheduler"));
    }

    @Test
    void intervalOwnsListeningPreparationBeforeAudioAndKeepsTypedStates()
            throws Exception {
        String viewModel = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "DocuPodcastShellViewModel.java"));
        int processStart = viewModel.indexOf(
                "public void processPdfIntervalWithoutPlayback(");
        int processEnd = viewModel.indexOf(
                "public void submitAudioGenerationWithPendingExport(", processStart);
        String process = viewModel.substring(processStart, processEnd);
        int prepare = process.indexOf("prepareIntervalListeningNarration(selection, validated)");
        int translate = process.indexOf("prepareEffectiveNarrationAsync(selection.narration()");
        int verify = process.indexOf("after.listeningPrepared()");
        int audio = process.indexOf("submitEffectiveAudioGeneration(effective");

        assertTrue(prepare >= 0);
        assertTrue(translate > prepare);
        assertTrue(verify > translate);
        assertTrue(audio > verify, "TTS must start only after listening readiness succeeds");
        assertFalse(process.contains(
                "submitAudioGeneration(intervalScript, DocumentPlaybackIntent.none())"));
        assertTrue(process.contains("semanticPrepared=true"));
        assertTrue(process.contains("listeningPrepared=false"));

        String snapshot = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "workflow", "DocumentListeningPreparationSnapshot.java"));
        assertTrue(snapshot.contains("boolean semanticallyPrepared"));
        assertTrue(snapshot.contains("boolean listeningPrepared()"));
    }

    @Test
    void stopAnalysisCancelsTranslationBeforePreparationCoordinators()
            throws Exception {
        String shell = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "DocuPodcastShellView.java"));
        int start = shell.indexOf("private void cancelLocalDocumentAnalysis()");
        int end = shell.indexOf("private void handleGenerateSelectedChunkFromStatusBar", start);
        String cancellation = shell.substring(start, end);

        assertTrue(cancellation.contains(
                "viewModel.cancelNarrationTranslationAnalysis()"));
        assertTrue(cancellation.indexOf("cancelNarrationTranslationAnalysis")
                < cancellation.indexOf("wordSemanticPreparation.cancelLocalAnalysis"));
    }

    @Test
    void audioWorkflowPropagatesTheEffectiveScriptLanguage() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "workflow", "AudioWorkflowCoordinator.java"));

        assertTrue(source.contains("jobName, script.language()"));
        assertFalse(source.contains("jobName, \"es\""));
    }
}
