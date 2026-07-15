package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextAnchorTest {
    @Test
    void createsCurrentAnchorWithSelectedTextHash() {
        TextAnchor anchor = TextAnchor.fromSelection(
                "ANCH-001",
                new DocumentTextRange("B0001", 4, 20),
                "La geología estudia la Tierra.",
                "Antes",
                "Después",
                "sha256:documento"
        );

        assertEquals(TextAnchorStatus.CURRENT, anchor.status());
        assertEquals(TextAnchorConfidence.HIGH, anchor.confidence());
        assertTrue(anchor.selectedTextHash().startsWith("sha256:"));
        assertTrue(anchor.current());
        assertTrue(anchor.hasSelectedText());
    }

    @Test
    void legacyAnchorKeepsRangeButRequiresReview() {
        TextAnchor anchor = TextAnchor.legacy("LYR-001", new DocumentTextRange("B0002", 0, 12));

        assertEquals("ANCH-LYR-001", anchor.id());
        assertEquals("B0002", anchor.range().blockId());
        assertEquals(TextAnchorConfidence.LOW, anchor.confidence());
        assertEquals(TextAnchorStatus.NEEDS_REVIEW, anchor.status());
        assertFalse(anchor.current());
    }
}
