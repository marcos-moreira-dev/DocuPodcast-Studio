package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocUxHf9FImportProgressPlaybackSourceTest {
    @Test
    void sourceImportRunsWithProgressAndOutsideDirectFileChooserMutation() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("DocumentImportProgressDialog"));
        assertTrue(shell.contains("Task<ReadableDocument>"));
        assertTrue(shell.contains("docupodcast-source-import"));
        assertTrue(viewModel.contains("importAndClassifySourceDocument"));
        assertTrue(viewModel.contains("attachImportedDocument"));
        assertFalse(shell.contains("viewModel.importWordDocument(file.toPath());"));
    }

    @Test
    void playbackCursorResolvesSentenceCueInsteadOfFirstSegmentCue() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java"));

        assertTrue(controller.contains("cuesForSegment(cursor.segmentId())"));
        assertTrue(controller.contains("Comparator.comparingDouble"));
        assertFalse(controller.contains("return byPosition.isPresent() ? byPosition : manifest.cueForSegment(cursor.segmentId());"));
    }
}
