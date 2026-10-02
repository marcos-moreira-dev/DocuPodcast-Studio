package com.marcosmoreiradev.docupodcaststudio.domain.document;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.DocumentAudioPreparationExtent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentProcessingIntervalTest {
    @Test
    void acceptsStrictInclusiveOneBasedPageIntervals() {
        assertEquals(java.util.List.of(1),
                DocumentProcessingInterval.pages(1, 1, 10).inclusiveIndexes());
        assertEquals(java.util.List.of(1, 2, 3),
                DocumentProcessingInterval.pages(1, 3, 10).inclusiveIndexes());
        assertEquals(java.util.List.of(3, 4, 5, 6, 7, 8),
                DocumentProcessingInterval.pages(3, 8, 10).inclusiveIndexes());
    }

    @Test
    void acceptsStrictInclusiveOneBasedWordBlockIntervals() {
        DocumentProcessingInterval interval =
                DocumentProcessingInterval.blocks(2, 4, 7);

        assertEquals(DocumentProcessingIntervalUnit.BLOCK, interval.unit());
        assertEquals(java.util.List.of(2, 3, 4), interval.inclusiveIndexes());
    }

    @Test
    void rejectsInvalidIntervalsWithoutSwappingOrClamping() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProcessingInterval.pages(0, 1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProcessingInterval.pages(-1, 3, 10));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProcessingInterval.pages(8, 3, 10));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProcessingInterval.pages(3, 999, 10));
    }

    @Test
    void scopeKeepsAnExplicitCompatibilityMapping() {
        assertEquals(DocumentAudioPreparationExtent.SINGLE_FRAGMENT,
                DocumentProcessingScope.SINGLE_FRAGMENT.legacyExtent());
        assertTrue(DocumentProcessingScope.FROM_SELECTION.legacyPortionEnabled());
        assertTrue(DocumentProcessingScope.INTERVAL.legacyPortionEnabled());
        assertFalse(DocumentProcessingScope.FULL_DOCUMENT.legacyPortionEnabled());
        assertEquals(4, DocumentProcessingScope.values().length);
    }
}
