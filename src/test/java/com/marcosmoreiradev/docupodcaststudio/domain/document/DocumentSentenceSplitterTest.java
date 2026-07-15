package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSentenceSplitterTest {
    @Test
    void splitsSentencesKeepingOffsetsInsideSourceBlock() {
        List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split("BLK-001", "Hola señora. ¿Cómo está? Bien.");

        assertEquals(3, spans.size());
        assertEquals("Hola señora.", spans.get(0).text());
        assertEquals(new DocumentTextRange("BLK-001", 0, 12), spans.get(0).range());
        assertEquals("¿Cómo está?", spans.get(1).text());
        assertEquals("Bien.", spans.get(2).text());
        assertFalse(spans.get(1).range().overlaps(spans.get(2).range()));
    }

    @Test
    void fallsBackToWholeBlockWhenThereIsNoTerminator() {
        List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split("BLK-002", "Una oración sin punto final");

        assertEquals(1, spans.size());
        assertEquals("Una oración sin punto final", spans.get(0).text());
        assertTrue(spans.get(0).range().length() > 0);
    }
}
