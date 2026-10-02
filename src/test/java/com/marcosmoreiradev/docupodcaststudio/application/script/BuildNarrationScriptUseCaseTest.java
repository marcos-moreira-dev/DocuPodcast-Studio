package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrationScriptUseCaseTest {
    @Test
    void legacyLogicalOnlyHeadingRemainsNarratableWithoutChangingItsStableId() {
        NarrationSegment legacy = new NarrationSegment(
                "SEG-010", NarrationSegmentType.HEADING,
                "Diccionario", "0.10 Diccionario de árboles de búsqueda",
                List.of("B0010"), "CHR-NARRATOR", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of(
                "logicalOnly", "true",
                "audioNarration", "SKIP_LOGICAL_TITLE"));

        assertTrue(legacy.narratable());
        assertEquals("SEG-010", legacy.id());
        assertEquals("0.10 Diccionario de árboles de búsqueda", legacy.narrationText());
    }

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
        assertEquals("Introducción", script.segments().get(0).narrationText());
        assertTrue(script.segments().get(0).narratable());
        assertEquals("DOCUMENT_TITLE", script.segments().get(0).metadata().get("semanticRole"));
        assertEquals("EXACT_SOURCE_TEXT", script.segments().get(0).metadata().get("audioNarration"));
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
    void readAfterColonNeverTruncatesAHeading() {
        ReadableDocument document = new ReadableDocument("Títulos",
                SourceDocumentFormat.DOCX, Path.of("titulos.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.HEADING,
                        "0.9 Algoritmo como evolución de estados: la película y el mapa",
                        "Heading1")));

        var script = new BuildNarrationScriptUseCase().build(document, "es", true);

        assertEquals("0.9 Algoritmo como evolución de estados: la película y el mapa",
                script.segments().getFirst().narrationText());
        assertTrue(script.segments().getFirst().narratable());
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
    void structuredTableDoesNotDuplicateExistingCellTerminators() {
        ReadableDocument document = new ReadableDocument("Tabla", SourceDocumentFormat.DOCX,
                Path.of("tabla.docx"), List.of(DocumentBlock.of(
                "B001", DocumentBlockType.TABLE_NOTICE, "Tabla", "table", Map.of(
                "table.rowCount", "2",
                "table.columnCount", "2",
                "table.header.0", "Tema",
                "table.header.1", "Pregunta",
                "table.cell.1.0", "Diseño de algoritmos.",
                "table.cell.1.1", "¿Cómo organizo la solución?"))));

        var script = new BuildNarrationScriptUseCase().build(
                document, "es", false, TableNarrationPolicy.READ_STRUCTURED);

        assertEquals("Lectura de cuadro. Encabezados: Tema; Pregunta. Fila 1. "
                        + "Tema: Diseño de algoritmos. Pregunta: ¿Cómo organizo la solución?",
                script.segments().getFirst().narrationText());
    }

    @Test
    void academicProfileReadsOnlyImagesWithNativeDescriptions() {
        ReadableDocument document = new ReadableDocument("Imágenes", SourceDocumentFormat.DOCX,
                Path.of("imagenes.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen del documento fuente: Diagrama de poda", "image",
                        Map.of("description", "Diagrama de poda")),
                DocumentBlock.of("B002", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen detectada sin descripción", "image")
        ));

        var script = new BuildNarrationScriptUseCase()
                .build(document, "es", false, ReadingProfile.academicDefaults());

        assertEquals(1, script.segmentCount());
        assertEquals("Imagen: Diagrama de poda.", script.segments().getFirst().narrationText());
        assertEquals("true", script.segments().getFirst().metadata().get("sourceVisualReadUnit"));
    }

    @Test
    void uncertainWordParagraphNeverReachesNarration() {
        ReadableDocument document = new ReadableDocument("Dudoso", SourceDocumentFormat.DOCX,
                Path.of("dudoso.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH,
                        "a b c d e f g h", "", Map.of("narratability", "UNCERTAIN")),
                DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH,
                        "Este párrafo sí es narrable.", "", Map.of("narratability", "NARRATABLE"))
        ));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals(1, script.segmentCount());
        assertEquals(List.of("B002"), script.segments().getFirst().sourceBlockIds());
    }

}
