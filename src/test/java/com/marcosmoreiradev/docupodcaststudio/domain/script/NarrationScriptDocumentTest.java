package com.marcosmoreiradev.docupodcaststudio.domain.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrationScriptDocumentTest {
    @Test
    void rejectsDuplicatedSegmentIds() {
        NarrationSegment one = NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno", List.of("B001"));
        NarrationSegment duplicated = NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Dos", "Texto dos", List.of("B002"));

        assertThrows(IllegalArgumentException.class, () -> NarrationScriptDocument.create("Guion", "es", "Doc", List.of(one, duplicated)));
    }

    @Test
    void countsSegmentsAndWords() {
        NarrationSegment one = NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Hola mundo", List.of("B001"));
        NarrationSegment two = NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Texto de prueba", List.of("B002"));

        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Doc", List.of(one, two));

        assertEquals(2, script.segmentCount());
        assertEquals(5, script.wordCount());
        assertTrue(script.segmentById("SEG-002").isPresent());
    }
}
