package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IntervencionCatalogoTest {
    @Test
    void sceneHeadingWithHardSpaceDoesNotShiftInterventionLabels() {
        ReadableDocument document = new ReadableDocument("Teatro", SourceDocumentFormat.DOCX, Path.of("source.docx"), List.of(
                block("B0001", "El vuelo del Tornillo Dorado"),
                block("B0004", "Escena\u00A01: El hangar. Un avion antiguo espera al amanecer."),
                block("B0005", "NARRADOR: En el viejo aerodromo de Santa Brisa."),
                block("B0006", "CAPITAN BIGOTE: Teniente, revise el combustible.")));
        NarrationScriptDocument script = NarrationScriptDocument.create("Teatro", "es", "source.docx", List.of(
                segment("SEG-001", "B0001", "Ahora veremos: El vuelo del Tornillo Dorado."),
                segment("SEG-004", "B0004", "Escena\u00A01: El hangar. Un avion antiguo espera al amanecer."),
                segment("SEG-005", "B0005", "NARRADOR: En el viejo aerodromo de Santa Brisa."),
                segment("SEG-006", "B0006", "CAPITAN BIGOTE: Teniente, revise el combustible.")));

        List<IntervencionCatalogo.IntervencionInfo> aliases = IntervencionCatalogo.intervenciones(document, script);

        assertEquals(2, aliases.size());
        assertEquals("INTERVENCION-1", aliases.get(0).alias());
        assertEquals("B0005", aliases.get(0).blockId());
        assertEquals("INTERVENCION-2", aliases.get(1).alias());
        assertEquals("B0006", aliases.get(1).blockId());
    }

    private static DocumentBlock block(String id, String text) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "");
    }

    private static NarrationSegment segment(String id, String blockId, String text) {
        return NarrationSegment.of(id, NarrationSegmentType.PARAGRAPH, "", text, List.of(blockId));
    }
}
