package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentAnchorsRc1SourceTest {
    @Test
    void newDocumentLayerAssignmentsBuildRichTextAnchorsFromReadableDocument() throws Exception {
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(coordinator.contains("TextAnchor.fromDocumentSelection"));
        assertTrue(coordinator.contains("SourceDocumentSnapshot.from(document).contentHash()"));
        assertTrue(coordinator.contains("ReadableDocument document"));
        assertTrue(vm.contains("currentDocument.get()"));
        assertTrue(!coordinator.contains("new NarrativeLayerAssignment(\n                assignmentId(normalizedKind, scriptRange),\n                normalizedKind,\n                scriptRange,\n                documentRange,\n                target.targetId()"));
    }
}
