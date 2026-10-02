package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentBlockNarratabilityOverrideTest {
    @Test
    void manualOverrideCanRestoreAnUncertainWordParagraph() {
        DocumentBlock uncertain = DocumentBlock.of("B004", DocumentBlockType.PARAGRAPH,
                "Texto revisado por el usuario.", "",
                Map.of("narratability", "UNCERTAIN"));

        assertFalse(uncertain.narratable());
        assertTrue(uncertain.withType(DocumentBlockType.PARAGRAPH, "revisión manual").narratable());
    }
}
