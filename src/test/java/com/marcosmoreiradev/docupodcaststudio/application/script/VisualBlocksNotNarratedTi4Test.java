package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VisualBlocksNotNarratedTi4Test {
    @Test
    void imageTableAndMathBlocksDoNotBecomeNarrationSegmentsByDefault() {
        ReadableDocument document = new ReadableDocument("Visuales", SourceDocumentFormat.DOCX, Path.of("visuales.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto que sí se habla.", ""),
                DocumentBlock.of("B002", DocumentBlockType.IMAGE_NOTICE, "Imagen detectada", "", Map.of("visualBlock", "true")),
                DocumentBlock.of("B003", DocumentBlockType.TABLE_NOTICE, "Tabla detectada", "", Map.of("visualBlock", "true")),
                DocumentBlock.of("B004", DocumentBlockType.MATH_NOTICE, "Fórmula detectada", "", Map.of("visualBlock", "true"))
        ));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals(1, script.segmentCount());
        assertEquals(List.of("B001"), script.segments().getFirst().sourceBlockIds());
    }
}
