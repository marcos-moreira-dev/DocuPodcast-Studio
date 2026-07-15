package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class BuildPreparedReadingProjectionUseCaseTest {
    @Test
    void buildsPreparedReadingProjectionFromSourceDocument() {
        ReadableDocument document = new ReadableDocument(
                "Documento de estudio",
                SourceDocumentFormat.DOCX,
                Path.of("documento.docx"),
                List.of(DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Texto para escuchar.", "Normal"))
        );

        PreparedReadingProjection projection = new BuildPreparedReadingProjectionUseCase(new BuildNarrationScriptUseCase())
                .build(document, "es");

        assertEquals(document, projection.sourceDocument());
        assertEquals(1, projection.fragmentCount());
        assertEquals(1L, projection.narratableFragmentCount());
        assertFalse(projection.empty());
        assertEquals("Lectura preparada", projection.label());
    }
}
