package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReadableDocumentWorkspaceRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void materializesSourceAndDocumentJsonUsingRelativeAssetsAndDiagnostics() throws Exception {
        Path source = tempDir.resolve("notas.docx");
        Files.writeString(source, "fake docx bytes");
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                source,
                List.of(DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Hola mundo", "", Map.of("styleId", "Normal"))),
                new DocumentImportReport(List.of(DocumentImportIssue.warning("DOCX_NO_HEADINGS", "Sin títulos detectados")))
        );
        Path projectFile = tempDir.resolve("Proyecto.docupodcast.json");

        ReadableDocumentWorkspaceRepository repository = new ReadableDocumentWorkspaceRepository();
        MaterializedImportedDocument materialized = repository.materialize(document, ReadingProfile.academicDefaults(), projectFile);

        assertEquals(ProjectAssetKind.SOURCE_DOCUMENT, materialized.sourceDocumentAsset().kind());
        assertEquals("source/notas.docx", materialized.sourceDocumentAsset().relativePath());
        assertEquals(ProjectAssetKind.IMPORTED_DOCUMENT, materialized.importedDocumentAsset().kind());
        assertEquals("document/document.json", materialized.importedDocumentAsset().relativePath());
        assertTrue(Files.exists(tempDir.resolve("source/notas.docx")));
        assertEquals(tempDir.resolve("source/notas.docx").toAbsolutePath().normalize(), materialized.projectSourceDocument().sourcePath());
        String json = Files.readString(tempDir.resolve("document/document.json"));
        assertTrue(json.contains("Hola mundo"));
        assertTrue(json.contains("importReport"));
        assertTrue(json.contains("DOCX_NO_HEADINGS"));
        assertTrue(json.contains("metadata"));
        assertTrue(json.contains("readingProfile"));
        assertTrue(json.contains("Documento académico Word"));
    }
}
