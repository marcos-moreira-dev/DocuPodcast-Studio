package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RefreshSourceDocumentUseCaseTest {
    @Test
    void refreshReimportsReadOnlySourceAndReportsStaleAudioWhenContentChanged() throws IOException {
        Path source = Files.createTempFile("docupodcast-refresh", ".docx");
        ReadableDocument current = document(source, "Contenido anterior.");
        ReadableDocument changed = document(source, "Contenido actualizado desde otra app.");
        RefreshSourceDocumentUseCase useCase = new RefreshSourceDocumentUseCase(new DocumentSourceImportService(new ImportDocumentUseCase(List.of(new StaticImporter(changed)))));

        RefreshSourceDocumentResult result = useCase.refresh(current);

        assertTrue(result.refreshedDocumentAvailable());
        assertTrue(result.report().hasContentChanges());
        assertTrue(result.report().audioShouldBeRegenerated());
        assertTrue(result.report().requiresUserReview());
    }

    @Test
    void missingExternalSourceDoesNotReplaceCurrentDocument() throws IOException {
        Path source = Files.createTempFile("docupodcast-missing-refresh", ".docx");
        Files.delete(source);
        ReadableDocument current = document(source, "Contenido anterior.");
        RefreshSourceDocumentUseCase useCase = new RefreshSourceDocumentUseCase(new DocumentSourceImportService(new ImportDocumentUseCase(List.of(new StaticImporter(current)))));

        RefreshSourceDocumentResult result = useCase.refresh(current);

        assertFalse(result.refreshedDocumentAvailable());
        assertTrue(result.report().requiresUserReview());
    }


    @Test
    void unsupportedNativeTextPdfRefreshDoesNotReplaceCurrentDocument() throws IOException {
        Path source = Files.createTempFile("docupodcast-refresh-scanned", ".pdf");
        Files.writeString(source, "%PDF-1.4\n%%EOF");
        ReadableDocument current = new ReadableDocument(
                "PDF previo",
                SourceDocumentFormat.PDF,
                source,
                List.of(DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto previo extraído.", "PDF")));
        RefreshSourceDocumentUseCase useCase = new RefreshSourceDocumentUseCase(
                new DocumentSourceImportService(new ImportDocumentUseCase(List.of(new RejectingPdfImporter()))));

        RefreshSourceDocumentResult result = useCase.refresh(current);

        assertFalse(result.refreshedDocumentAvailable());
        assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeStatus.UNSUPPORTED, result.report().status());
        assertTrue(result.report().requiresUserReview());
    }

    private static ReadableDocument document(Path source, String text) {
        return new ReadableDocument(
                "Documento refrescable",
                SourceDocumentFormat.DOCX,
                source,
                List.of(DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, text, "Normal")));
    }

    private record StaticImporter(ReadableDocument document) implements DocumentImporter {
        @Override
        public boolean supports(Path sourceFile) {
            return true;
        }

        @Override
        public ReadableDocument importDocument(Path sourceFile) {
            return document;
        }
    }

    private record RejectingPdfImporter() implements DocumentImporter {
        @Override
        public boolean supports(Path sourceFile) {
            return sourceFile.toString().toLowerCase().endsWith(".pdf");
        }

        @Override
        public ReadableDocument importDocument(Path sourceFile) throws IOException {
            throw new SourceDocumentRequirementException("PDF no compatible: no contiene texto nativo extraíble suficiente.");
        }
    }
}
