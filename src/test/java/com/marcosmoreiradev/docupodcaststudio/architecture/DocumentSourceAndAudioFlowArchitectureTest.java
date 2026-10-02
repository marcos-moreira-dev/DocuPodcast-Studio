package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSourceAndAudioFlowArchitectureTest {
    @Test
    void welcomeDoesNotExposeAStandaloneDocumentEntry() throws IOException {
        String welcome = source("presentation/welcome/WelcomeWorkspaceView.java");

        assertFalse(welcome.contains("commandRow(AppCommandId.OPEN_SOURCE_DOCUMENT"));
        assertTrue(welcome.contains("Todo documento debe pertenecer a un proyecto"));
    }

    @Test
    void sourceAttachmentCannotCreateAnImplicitProject() throws IOException {
        String viewModel = source("presentation/shell/DocuPodcastShellViewModel.java");
        String attach = between(viewModel, "public void attachImportedDocument(",
                "public Path createPdfSourceFromImageFolder");

        assertFalse(attach.contains("createNewProject("));
        assertTrue(attach.contains("Primero crea un proyecto"));
        assertTrue(attach.contains("sessions.activeSession().orElseThrow"));
    }

    @Test
    void newProjectExistsBeforeTheInitialSourceIsChosen() throws IOException {
        String shell = source("presentation/shell/DocuPodcastShellView.java");
        String creation = between(shell, "private void createProjectFromSetup(",
                "public void handleOpenProject()");

        assertTrue(creation.indexOf("viewModel.createNewProject(")
                < creation.indexOf("projectInitialSourceDialog.show(owner())"));
        assertFalse(creation.contains("runSourceDocumentImport(sourceFile,"));
    }

    @Test
    void fullDocumentPlaybackIntentIsRestoredAfterTheFreshRenderReset() throws IOException {
        String viewModel = source("presentation/shell/DocuPodcastShellViewModel.java");
        String primary = between(viewModel, "public void runDocumentPrimaryAction()",
                "/** Starts a freshly materialized PDF reading");
        String listen = between(viewModel, "public void listenToDocument()",
                "private void applyImportedOrGeneratedScript");
        String submit = between(viewModel, "private void submitAudioGeneration(DocumentPlaybackIntent",
                "public void submitMockAudioGeneration");
        String fresh = between(viewModel, "private void submitFreshAudioRequestAsync(",
                "private void submitPriorityAudioRequestAsync(");

        assertTrue(primary.contains("listenToDocument()"));
        assertFalse(primary.contains("selectedDocumentSegmentOrSelected()"));
        assertTrue(listen.contains("DocumentAudioAction.FAST_LISTEN"));
        assertTrue(listen.contains("DocumentProcessingScope.FULL_DOCUMENT"));
        assertTrue(listen.contains("submitAudioGeneration(playbackIntent)"));
        assertFalse(listen.contains("submitAudioGenerationFromSegment(selectedStart"));
        assertTrue(listen.contains("hasCompleteAudioCoverageForDocument()"));
        assertTrue(listen.contains("!manifest.emptyManifest() && completeDocumentAudio"));
        assertTrue(submit.contains("restorePlaybackIntent(playbackIntent)"));
        assertTrue(fresh.indexOf("resetPlaybackState();")
                < fresh.indexOf("restorePlaybackIntent.run();"));
    }

    @Test
    void shellUsesOneTypedPolicyForFastListeningAndCompleteProcessing() throws IOException {
        String shell = source("presentation/shell/DocuPodcastShellView.java");
        String workspace = source("presentation/document/DocumentWorkspaceView.java");
        String context = source("presentation/document/DocumentContextDetailsPanel.java");

        assertTrue(shell.contains("executeDocumentAudioAction(DocumentAudioAction.FAST_LISTEN"));
        assertTrue(shell.contains("executeDocumentAudioAction(DocumentAudioAction.PROCESS_COMPLETE"));
        assertTrue(shell.contains("action.requiresWordSemanticPreparation()"));
        assertTrue(shell.contains("showProjectRequiredMessage();"));
        assertFalse(workspace.contains("DocumentReadingProfilePanel"));
        assertTrue(context.contains("SecondarySemanticReadingPolicy"));
    }

    @Test
    void sidebarVoiceSelectionConfiguresWithoutRenderingAndRegenerationUsesTypedFlow()
            throws IOException {
        String panel = source("presentation/document/DocumentAudioNarrationPanel.java");
        String selection = between(panel,
                "private void configureSelectedVoiceWithoutRendering()",
                "private void requestDocumentAudioAction(");
        String apply = between(panel,
                "private void useSelectedVoiceForWholeDocument()",
                "private void configureSelectedVoiceWithoutRendering()");
        String workspace = source("presentation/document/DocumentWorkspaceView.java");

        assertTrue(selection.contains("configureVoiceForDocument(voice, tone)"));
        assertFalse(selection.contains("generateAudio"));
        assertTrue(apply.contains("DocumentAudioAction.GENERATE_SELECTION"));
        assertTrue(apply.contains("DocumentAudioAction.GENERATE_ALL"));
        assertFalse(apply.contains("useVoiceForDocumentFrom"));
        assertTrue(workspace.contains(
                "new DocumentAudioNarrationPanel(\n                                viewModel, documentAudioActionRequest)"));
    }

    @Test
    void playbackFocusSelectsAndScrollsWithoutSeekingTheTransport() throws IOException {
        String workspace = source("presentation/document/DocumentWorkspaceView.java");
        String viewModel = source("presentation/shell/DocuPodcastShellViewModel.java");
        String cueSync = between(workspace, "private void syncActivePlaybackCue(",
                "private void selectSentenceFromPlayback(");
        String playbackSelection = between(workspace,
                "private void selectSentenceFromPlayback(",
                "private void selectPlaybackBlock(");
        String blockResolution = between(workspace,
                "private String blockIdForPlaybackSegment(",
                "private void ensureBlockRendered(");
        String focus = between(viewModel,
                "public void focusDocumentTextRangeDuringPlayback(",
                "public void selectPdfRegion(");

        assertTrue(cueSync.contains("selectSentenceFromPlayback(span, segmentId)"));
        assertTrue(cueSync.contains("schedulePlaybackScroll(sentenceNode, sequence)"));
        assertTrue(workspace.contains("viewModel.activePlaybackCueProperty().addListener"));
        assertTrue(viewModel.contains("activePlaybackCue.set(cue)"));
        assertTrue(workspace.contains("ACTIVE_READING_GAP_BELOW_PLAYBAR = 30.0"));
        assertTrue(playbackSelection.contains("focusDocumentTextRangeDuringPlayback"));
        assertFalse(playbackSelection.contains("selectDocumentTextRange"));
        assertFalse(focus.contains("alignPlaybackCursorToSelectedSentence"));
        assertTrue(focus.contains("refreshDocumentInteractionProjection()"));
        assertTrue(blockResolution.contains("if (hasPdfSource())"));
        assertTrue(blockResolution.contains("wordSegment.sourceBlockIds()"));
    }

    @Test
    void documentaryVideoUsesTheSharedModalProgressCoordinator() throws IOException {
        String shell = source("presentation/shell/DocuPodcastShellView.java");
        String export = between(shell,
                "private void exportDocumentStudyTextAudioVideoInBackground(",
                "private void exportTheatreWorkInBackground(");

        assertTrue(export.contains("videoExportProgressCoordinator.export"));
        assertTrue(export.contains("viewModel::acceptDocumentExportProgress"));
        assertTrue(export.contains("viewModel::endDocumentExportRender"));
        assertFalse(export.contains("new Task<>"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio")
                .resolve(relative), StandardCharsets.UTF_8);
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("No se encontró el tramo entre " + startToken + " y " + endToken);
        }
        return source.substring(start, end);
    }
}
