package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrationScriptUseCaseTest {
    @Test
    void buildsStableSegmentsFromNarratableDocumentBlocks() {
        ReadableDocument document = new ReadableDocument("Dinosaurios", SourceDocumentFormat.DOCX, Path.of("dinosaurios.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.HEADING, "Introducción", "Heading1"),
                DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH, "Los dinosaurios vivieron hace millones de años.", ""),
                DocumentBlock.of("B003", DocumentBlockType.IGNORED, "No narrar", ""),
                DocumentBlock.of("B004", DocumentBlockType.IMAGE_NOTICE, "Imagen sin descripción. Se omite.", "")
        ));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals(2, script.segmentCount());
        assertEquals("SEG-001", script.segments().get(0).id());
        assertEquals(NarrationSegmentType.HEADING, script.segments().get(0).type());
        assertTrue(script.segments().get(0).narrationText().contains("Nuevo tema"));
        assertEquals(List.of("B002"), script.segments().get(1).sourceBlockIds());
        assertTrue(script.segments().stream().noneMatch(segment -> segment.sourceBlockIds().contains("B004")),
                "Las imágenes fuente son bloques visuales no narrables por defecto.");
    }

    @Test
    void detectsTheatreSpeakerCharacterIdFromCuePrefix() {
        ReadableDocument document = new ReadableDocument("Teatro", SourceDocumentFormat.DOCX, Path.of("teatro.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "CAPITAN BIGOTE: Teniente, revise el combustible.", ""),
                DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH, "TENIENTE TORNILLO: Combustible hay.", ""),
                DocumentBlock.of("B003", DocumentBlockType.PARAGRAPH, "Escena 1: El hangar.", "")
        ));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals("CHR-CAPITAN-BIGOTE", script.segments().get(0).characterId());
        assertEquals("CHR-TENIENTE-TORNILLO", script.segments().get(1).characterId());
        assertEquals("CHR-NARRATOR", script.segments().get(2).characterId());
    }

    @Test
    void readAfterColonDisabledKeepsTheFullCueText() {
        ReadableDocument document = new ReadableDocument("Dialogo", SourceDocumentFormat.DOCX, Path.of("dialogo.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "TEXTO1: TEXTO2", "")
        ));

        var full = new BuildNarrationScriptUseCase().build(document, "es", false);
        var suffix = new BuildNarrationScriptUseCase().build(document, "es", true);

        assertEquals("TEXTO1: TEXTO2", full.segments().getFirst().narrationText());
        assertEquals("TEXTO2", suffix.segments().getFirst().narrationText());
    }

    @Test
    void tablePolicyControlsSecondaryTableReadUnits() {
        ReadableDocument document = new ReadableDocument("Tabla", SourceDocumentFormat.DOCX, Path.of("tabla.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.TABLE_NOTICE, "Tabla del documento fuente", "table", Map.of(
                        "table.rowCount", "2",
                        "table.columnCount", "2",
                        "table.header.0", "H1",
                        "table.header.1", "H2",
                        "table.cell.1.0", "v1",
                        "table.cell.1.1", "v2"
                ))
        ));

        var ignored = new BuildNarrationScriptUseCase().build(document, "es", false, TableNarrationPolicy.IGNORE_TABLES);
        var summary = new BuildNarrationScriptUseCase().build(document, "es", false, TableNarrationPolicy.ANNOUNCE_SUMMARY);
        var structured = new BuildNarrationScriptUseCase().build(document, "es", false, TableNarrationPolicy.READ_STRUCTURED);

        assertEquals(0, ignored.segmentCount());
        assertEquals("Tabla del documento fuente de 2 filas y 2 columnas.", summary.segments().getFirst().narrationText());
        assertEquals("Lectura de cuadro. Encabezados: H1; H2. Fila 1. H1: v1. H2: v2.",
                structured.segments().getFirst().narrationText());
        assertEquals("true", structured.segments().getFirst().metadata().get("secondaryReadUnit"));
    }

    @Test
    void pdfNarrationUsesOnlyOcrLocalBlocks() {
        ReadableDocument document = new ReadableDocument("PDF", SourceDocumentFormat.PDF, Path.of("book.pdf"), List.of(
                DocumentBlock.of("NATIVE", DocumentBlockType.PARAGRAPH, "Native PDF text must be ignored.", "", Map.of(
                        "sourcePage", "1",
                        "bbox", "10,20,300,40",
                        "bboxUnits", "pdf-points",
                        "nativeText", "true",
                        "ocr", "false",
                        "extractionMode", "pdfbox-text")),
                DocumentBlock.of("OCR", DocumentBlockType.PARAGRAPH, "OCR PDF text is narratable.", "", Map.of(
                        "sourcePage", "1",
                        "bbox", "10,50,300,70",
                        "bboxUnits", "pdf-points",
                        "nativeText", "false",
                        "ocr", "true",
                        "extractionMode", "ocr-local"))
        ));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals(1, script.segmentCount());
        assertEquals(List.of("OCR"), script.segments().getFirst().sourceBlockIds());
    }
}
