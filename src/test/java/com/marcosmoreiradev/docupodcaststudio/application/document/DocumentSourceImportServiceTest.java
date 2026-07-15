package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.MarkdownDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PlainTextDocumentImporter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSourceImportServiceTest {
    @Test
    void unifiedImportServiceAcceptsRealV1SourceTypes() throws Exception {
        DocumentSourceImportService service = new DocumentSourceImportService(new ImportDocumentUseCase(List.of(
                new MarkdownDocumentImporter(), new PlainTextDocumentImporter(), new PdfDocumentImporter())));
        Path txt = Files.createTempFile("docupodcast", ".txt");
        Files.writeString(txt, "Titulo\n\nParrafo narrable desde TXT", StandardCharsets.UTF_8);

        ReadableDocument document = service.importSource(txt);

        assertEquals(SourceDocumentFormat.TXT, document.format());
        assertTrue(DocumentSourceDescriptor.from(txt).readOnly());
        assertEquals(DocumentSourceType.TXT, DocumentSourceDescriptor.from(txt).type());
    }

    @Test
    void pdfDescriptorDoesNotRequireNativeTextForOcrOnlyPolicy() throws Exception {
        Path pdf = Files.createTempFile("docupodcast", ".pdf");
        DocumentSourceDescriptor descriptor = DocumentSourceDescriptor.from(pdf);

        assertEquals(DocumentSourceType.PDF_TEXT, descriptor.type());
        assertTrue(descriptor.readOnly());
        assertFalse(descriptor.nativeTextRequired());
    }
}
