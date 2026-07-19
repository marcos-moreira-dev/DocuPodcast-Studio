package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeDocumentContextCompilerTest {
    private final NarrativeDocumentContextCompiler compiler = new NarrativeDocumentContextCompiler();

    @Test
    void compilesOnlyWordTextAndUsesOneNarratableParagraphAsTheTakeAction() {
        ReadableDocument document = new ReadableDocument(
                "Viaje",
                SourceDocumentFormat.DOCX,
                Path.of("viaje.docx"),
                List.of(
                        DocumentBlock.of("T001", DocumentBlockType.TITLE,
                                "La expedicion de Aurora", "Title"),
                        DocumentBlock.of("H001", DocumentBlockType.HEADING,
                                "Cruce de la montana", "Heading 1"),
                        DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH,
                                "Aurora avanza por la nieve y levanta la linterna.", "Normal"),
                        DocumentBlock.of("IMG1", DocumentBlockType.IMAGE_NOTICE,
                                "Texto alternativo ajeno al guion", "image"),
                        DocumentBlock.of("TAB1", DocumentBlockType.TABLE_NOTICE,
                                "Datos de produccion no narrables", "table"),
                        DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH,
                                "Aurora observa la nieve desde el refugio.", "Normal")));

        NarrativeDocumentContext context = compiler.compile(document, "B001", 2_000);

        assertEquals("B001", context.blockId());
        assertEquals("Aurora avanza por la nieve y levanta la linterna.", context.paragraphText());
        assertTrue(context.promptContext().contains("La expedicion de Aurora"));
        assertTrue(context.promptContext().contains("Cruce de la montana"));
        assertTrue(context.promptContext().contains("Aurora observa la nieve"));
        assertFalse(context.normalizedDocumentText().contains("Texto alternativo ajeno"));
        assertFalse(context.normalizedDocumentText().contains("Datos de produccion"));
        assertEquals(List.of("B001", "T001", "H001", "B002"), context.sourceBlockIds());
    }

    @Test
    void rejectsImagesAndOtherNonNarratableBlocksAsTakes() {
        ReadableDocument document = new ReadableDocument(
                "Documento",
                SourceDocumentFormat.DOCX,
                Path.of("source.docx"),
                List.of(DocumentBlock.of("IMG1", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen incrustada", "image")));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(document, "IMG1"));

        assertTrue(failure.getMessage().contains("no es un parrafo narrable"));
    }
}
