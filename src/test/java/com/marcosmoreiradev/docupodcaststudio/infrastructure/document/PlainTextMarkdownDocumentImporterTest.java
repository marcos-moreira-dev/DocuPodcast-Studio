package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlainTextMarkdownDocumentImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void importsMarkdownAndTxtAsReadOnlySourceDocuments() throws Exception {
        Path markdown = tempDir.resolve("notas.md");
        Files.writeString(markdown, "# Título\n\n## Sección\n\nTexto narrable.");
        var md = new MarkdownDocumentImporter().importDocument(markdown);
        assertEquals(SourceDocumentFormat.MARKDOWN, md.format());
        assertEquals(DocumentBlockType.TITLE, md.blocks().get(0).type());
        assertEquals("read-only", md.blocks().get(0).metadata().get("sourceMode"));
        assertTrue(md.issues().stream().anyMatch(issue -> issue.message().contains("solo lectura")));

        Path txt = tempDir.resolve("notas.txt");
        Files.writeString(txt, "Título TXT\n\nPrimer párrafo narrable.");
        var text = new PlainTextDocumentImporter().importDocument(txt);
        assertEquals(SourceDocumentFormat.TXT, text.format());
        assertEquals(2, text.blocks().size());
        assertEquals("read-only", text.blocks().get(1).metadata().get("sourceMode"));
    }
}
