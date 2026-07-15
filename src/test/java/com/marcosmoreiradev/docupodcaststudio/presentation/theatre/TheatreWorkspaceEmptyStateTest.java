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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreWorkspaceEmptyStateTest {
    @Test
    void noActsExplainsMissingSourceBeforeSuggestingTheatreStructure() {
        String message = TheatreWorkspaceEmptyState.noActsMessage(null, null);

        assertTrue(message.contains("fuente primaria"));
        assertTrue(message.contains("puedes crear actos"));
    }

    @Test
    void noActsExplainsNonTheatreSourceWhenNoCuesAreDetected() {
        String message = TheatreWorkspaceEmptyState.noActsMessage(
                document("Parrafo narrativo sin dialogo teatral."),
                null);

        assertTrue(message.contains("No hay intervenciones teatrales detectables"));
    }

    @Test
    void noActsMovesToStructureWhenTheatreCuesExist() {
        String message = TheatreWorkspaceEmptyState.noActsMessage(
                document("NARRADOR: Entra la luz."),
                null);

        assertEquals("Crea el primer acto para ordenar escenas e intervenciones detectadas.", message);
    }

    @Test
    void sequenceMessageAndPreparationNoticeSeparateVisibleMapFromPreparedReading() {
        ReadableDocument document = document("CAPITAN: Revisen la cubierta.");

        assertEquals("Marca texto inicial y final si quieres acotar esta escena.",
                TheatreWorkspaceEmptyState.interventionSequenceMessage(document, null));
        assertTrue(TheatreWorkspaceEmptyState.preparationNotice(document, null).orElse("").contains("Lectura no preparada"));
        assertTrue(TheatreWorkspaceEmptyState.preparationNotice(document, script()).isEmpty());
    }

    private static ReadableDocument document(String text) {
        return new ReadableDocument("Teatro", SourceDocumentFormat.DOCX, Path.of("source.docx"), List.of(
                DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, text, "")));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Teatro", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "", "CAPITAN: Revisen la cubierta.", List.of("B0001"))));
    }
}
