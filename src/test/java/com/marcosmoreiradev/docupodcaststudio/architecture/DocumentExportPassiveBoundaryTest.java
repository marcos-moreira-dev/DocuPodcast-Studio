package com.marcosmoreiradev.docupodcaststudio.architecture;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentExportPassiveBoundaryTest {
    @Test void changingVoiceEngineDoesNotRenderOrDeleteAudio() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String method = between(source, "private void refreshChunksAfterDocumentVoiceChange(String result)",
                "private boolean invalidatePersistedAudioAfterVoiceSelection()");
        assertFalse(method.contains("generateAudio"));
        assertFalse(method.contains("deletePersisted"));
        assertFalse(method.contains("invalidatePersistedAudioAfterVoiceSelection"));
        assertTrue(method.contains("refreshFullDocumentReadingReadiness()"));
        assertTrue(method.contains("Cambio pendiente de procesar"));
    }
    @Test
    void documentaryExportChecksReadinessBeforeAutomaticRepair() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/"
                        + "DocuPodcastShellView.java"), StandardCharsets.UTF_8);
        String method = between(source,
                "private void handleExportDocumentStudyTextAudioVideoPrepared(",
                "private static String audioReadinessDetail(");

        int readiness = method.indexOf("inspectDocumentExportReadiness()");
        int repair = method.indexOf("submitAudioGenerationWithPendingExport(readiness");
        assertTrue(readiness >= 0);
        assertTrue(repair > readiness);
        assertFalse(method.contains("confirmRenderAndExport("));
        assertFalse(method.contains("prepareEffectiveNarrationAsync"));
        assertFalse(method.contains("AdaptNarrationLanguageUseCase"));
    }

    @Test
    void passiveReadinessUsesCacheInspectionAndNeverProductionEntryPoints()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/"
                        + "DocuPodcastShellViewModel.java"), StandardCharsets.UTF_8);
        String method = between(source,
                "public DocumentExportReadinessSnapshot inspectDocumentExportReadiness()",
                "private java.util.List<AudioJobSnapshot> audioJobsForExport");

        assertTrue(method.contains("adaptNarrationLanguage.inspectCache("));
        assertTrue(method.contains("audioCoverageSnapshotAssembler.inspect("));
        assertFalse(method.contains("adaptNarrationLanguage.execute("));
        assertFalse(method.contains("submitAudioGeneration"));
        assertFalse(method.contains("reconciledAudioForExport"));
        assertFalse(method.contains("prepare" + "EffectiveNarrationAsync"));
    }

    @Test
    void completeBatchPreparationIsAlsoAnExplicitExportPrerequisite()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/"
                        + "DocuPodcastShellView.java"), StandardCharsets.UTF_8);
        String processing = between(source,
                "private void handleGenerateChunksFromStatusBar()",
                "private void cancelDocumentPreparationForRestart()");
        String export = between(source,
                "public void handleExportDocumentStudyTextAudioVideo(DocumentTextVideoOptions",
                "public void handleExportTheatreWork()");

        assertTrue(processing.contains("DocumentAudioAction.PROCESS_COMPLETE"));
        assertTrue(processing.contains("prepareCompleteReadingThenRun"));
        assertFalse(export.contains("PROCESS_COMPLETE"));
        assertFalse(export.contains("prepareCompleteAudioThenRun"));
        assertTrue(export.contains("prepareDocumentForExportThenRun"));
        assertTrue(export.contains("prepareCompleteReadingThenRun"));
        assertTrue(export.contains("wordSemanticPreparation.prepareThenRun"));
    }

    @Test
    void unattendedExportChoosesDestinationBeforeStartingHeavyPreparation()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/"
                        + "DocuPodcastShellView.java"), StandardCharsets.UTF_8);
        String audio = between(source,
                "private void handleExportPodcastWav(AudioExportFormat requestedFormat,",
                "private void handleExportPodcastWavPrepared(");
        String video = between(source,
                "private void handleExportDocumentStudyTextAudioVideo(",
                "private void handleExportDocumentStudyTextAudioVideoPrepared(");

        assertTrue(audio.indexOf("chooseExportPodcastWavTarget(format)")
                < audio.indexOf("prepareDocumentForExportThenRun"));
        assertTrue(video.indexOf("fileForExport(")
                < video.indexOf("prepareDocumentForExportThenRun"));
        assertTrue(audio.contains("DocumentProcessingScope.FULL_DOCUMENT"));
        assertTrue(video.contains("DocumentProcessingScope.FULL_DOCUMENT"));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0, "missing start marker: " + start);
        assertTrue(to > from, "missing end marker: " + end);
        return source.substring(from, to);
    }
}
