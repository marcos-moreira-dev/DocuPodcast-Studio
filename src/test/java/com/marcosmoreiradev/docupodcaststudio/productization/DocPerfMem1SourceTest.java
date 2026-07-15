package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-PERF/MEM1: large document navigation keeps indexes cached instead of rescanning all blocks on UI updates. */
final class DocPerfMem1SourceTest {
    @Test
    void documentWorkspaceCachesBlockAndSentenceIndexesForLargeDocuments() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertTrue(source.contains("blockIndexById"));
        assertTrue(source.contains("sentenceSpanIndex"));
        assertTrue(source.contains("rebuildBlockIndex(document)"));
        assertTrue(source.contains("return sentenceSpanIndex.get(sentenceId);"));
        assertFalse(source.contains("document.blocks().stream()\n                .flatMap(block -> DocumentSentenceSplitter.split(block).stream())"));
    }

    @Test
    void shellViewModelDebtReturnsUnderTransitionalLimit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(source.lines().count() <= 2700, "DocuPodcastShellViewModel debe quedar bajo el límite transitorio antes de RF-TX2.");
    }
}
