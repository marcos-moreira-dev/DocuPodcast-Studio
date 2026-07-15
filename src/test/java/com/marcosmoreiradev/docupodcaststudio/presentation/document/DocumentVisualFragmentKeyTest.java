package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentVisualFragmentKeyTest {
    @Test
    void identifiesOneVisualUnitEvenWhenSegmentsAreShared() {
        DocumentFragmentRailPresentation fragment = new DocumentFragmentRailPresentation(
                "SEG-004-U002", "SEG-004", "B004", 12, 31,
                "Unidad", "Texto", "asset", "file:///image.png");

        DocumentVisualFragmentKey key = DocumentVisualFragmentKey.from(fragment);

        assertTrue(key.matchesUnit("SEG-004-U002"));
        assertFalse(key.matchesUnit("SEG-004-U001"));
        assertFalse(key.emptyKey());
    }
}
