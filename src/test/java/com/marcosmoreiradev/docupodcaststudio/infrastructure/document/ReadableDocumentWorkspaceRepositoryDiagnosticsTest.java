package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadableDocumentWorkspaceRepositoryDiagnosticsTest {
    @TempDir
    Path tempDir;

    @Test
    void documentJsonIncludesSummaryMetadataAndIssues() throws Exception {
        Path source = tempDir.resolve("notas.docx");
        Files.writeString(source, "fake docx bytes");
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                source,
                List.of(DocumentBlock.of("B0001", DocumentBlockType.IMAGE_NOTICE, "Imagen detectada", "", java.util.Map.of("source", "docx:image"))),
                List.of(DocumentImportIssue.warning("B0001", "Imagen sin descripción"))
        );

        new ReadableDocumentWorkspaceRepository().materialize(document, ReadingProfile.academicDefaults(), tempDir.resolve("Proyecto.docupodcast.json"));

        String json = Files.readString(tempDir.resolve("document/document.json"));
        assertTrue(json.contains("\"summary\""));
        assertTrue(json.contains("\"metadata\""));
        assertTrue(json.contains("Imagen sin descripción"));
        assertTrue(json.contains("readingProfile"));
    }
}
