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
import java.util.Map;

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

    @Test
    void silentStageDirectionRemainsSelectableForTheatrePreview() {
        DocumentBlock direction = DocumentBlock.of(
                "B-ACOT-1", DocumentBlockType.PARAGRAPH,
                "Acotación: La plaza queda vacía mientras cae la tarde.", "",
                Map.of("theatreStageDirection", "true", "narratability", "NON_NARRATABLE"));
        ReadableDocument document = new ReadableDocument(
                "Teatro", SourceDocumentFormat.DOCX, Path.of("source.docx"), List.of(direction));
        NarrationSegment segment = new NarrationSegment(
                "SEG-ACOT-1", NarrationSegmentType.PARAGRAPH, "",
                direction.text(), List.of(direction.id()), "CHR-ACOTACION", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of("theatreStageDirection", "true",
                        "theatreGrammarInterventionId", "INTERVENCION-10001"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Teatro", "es", "source.docx", List.of(segment));

        List<IntervencionCatalogo.IntervencionInfo> aliases =
                IntervencionCatalogo.intervenciones(document, script);

        assertEquals(1, aliases.size());
        assertEquals("INTERVENCION-10001", aliases.get(0).alias());
        assertEquals("B-ACOT-1", aliases.get(0).blockId());
        assertEquals(true, aliases.get(0).stageDirection());
        assertEquals("La plaza queda vacía mientras cae la tarde.",
                TheatreFullscreenMapView.stripStageDirectionCue(aliases.get(0).fullText()));
    }

    @Test
    void canonicalBlockIdentityWinsOverStaleNarrationMetadata() {
        DocumentBlock concha = DocumentBlock.of(
                "B-INTERVENCION-6", DocumentBlockType.PARAGRAPH,
                "CONCHA: ¡Yo, yo quiero la palabra!", "",
                Map.of("characterName", "CONCHA",
                        "theatreGrammarInterventionId", "INTERVENCION-11"));
        ReadableDocument document = new ReadableDocument(
                "Teatro", SourceDocumentFormat.DOCX, Path.of("source.md"), List.of(concha));
        NarrationSegment staleSegment = new NarrationSegment(
                "SEG-INTERVENCION-6", NarrationSegmentType.PARAGRAPH, "",
                concha.text(), List.of(concha.id()), "CHR-CONCHA", "VOC-CONCHA",
                "STY-NEUTRAL", Map.of(
                "characterName", "CONCHA",
                "theatreGrammarInterventionId", "INTERVENCION-11"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Teatro", "es", "source.md", List.of(staleSegment));

        List<IntervencionCatalogo.IntervencionInfo> aliases =
                IntervencionCatalogo.intervenciones(document, script);

        assertEquals(1, aliases.size());
        assertEquals("INTERVENCION-6", aliases.getFirst().alias());
        assertEquals("B-INTERVENCION-6", aliases.getFirst().blockId());
        assertEquals("CONCHA: ¡Yo, yo quiero la palabra!", aliases.getFirst().fullText());
    }

    private static DocumentBlock block(String id, String text) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "");
    }

    private static NarrationSegment segment(String id, String blockId, String text) {
        return NarrationSegment.of(id, NarrationSegmentType.PARAGRAPH, "", text, List.of(blockId));
    }
}
