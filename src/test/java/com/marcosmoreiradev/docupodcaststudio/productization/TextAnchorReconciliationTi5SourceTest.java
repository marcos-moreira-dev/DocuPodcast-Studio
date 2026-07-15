package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextAnchorReconciliationTi5SourceTest {
    @Test
    void ti5DocumentsStrongTextAnchorReconciliation() throws Exception {
        String anchor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/TextAnchor.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/ReconcileTextAnchorsUseCase.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java"));
        String doc = Files.readString(Path.of("docs/productizacion/TI5_TEXTANCHOR_RECONCILIACION.md"));

        assertTrue(anchor.contains("fromDocumentSelection"));
        assertTrue(anchor.contains("selectedTextHash"));
        assertTrue(anchor.contains("contextBefore"));
        assertTrue(useCase.contains("TextAnchorStatus.RELOCATED"));
        assertTrue(useCase.contains("ORPHANED"));
        assertTrue(services.contains("reconcileTextAnchors"));
        assertTrue(doc.contains("CURRENT"));
        assertTrue(doc.contains("RELOCATED"));
        assertTrue(doc.contains("ORPHANED"));
    }
}
