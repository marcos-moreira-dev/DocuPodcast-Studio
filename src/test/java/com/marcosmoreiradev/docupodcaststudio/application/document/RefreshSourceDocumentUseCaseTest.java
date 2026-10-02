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
    @Test void theatreProjectionCannotFallBackToGenericMarkdownRefresh() throws IOException {
        Path source = Files.createTempFile("theatre-refresh", ".md");
        ReadableDocument clean = new ReadableDocument("Obra", SourceDocumentFormat.MARKDOWN, source,
                List.of(new DocumentBlock("B-1", DocumentBlockType.PARAGRAPH, "Solo diálogo.", "theatre-dialogue",
                        java.util.Map.of("theatreGrammarInterventionId", "INTERVENCION-1"))));
        var result = new RefreshSourceDocumentUseCase(service(new StaticImporter(document(source, "origen=centro")))).refresh(clean);
        assertFalse(result.refreshedDocumentAvailable());
        assertTrue(result.report().requiresUserReview());
    }
    @Test
    void refreshReimportsReadOnlySourceAndReportsStaleAudioWhenContentChanged() throws IOException {
        Path source = Files.createTempFile("docupodcast-refresh", ".docx");
        ReadableDocument current = document(source, "Contenido anterior.");
        ReadableDocument changed = document(source, "Contenido actualizado desde otra app.");
        RefreshSourceDocumentUseCase useCase =
                new RefreshSourceDocumentUseCase(service(new StaticImporter(changed)));

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
        RefreshSourceDocumentUseCase useCase =
                new RefreshSourceDocumentUseCase(service(new StaticImporter(current)));

        RefreshSourceDocumentResult result = useCase.refresh(current);

        assertFalse(result.refreshedDocumentAvailable());
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

    private static DocumentSourceImportService service(DocumentImporter importer) {
        return new DocumentSourceImportService(new ImportDocumentUseCase(List.of(importer)),
                new CreatePreparedPdfSessionWorkspaceUseCase(
                        new InMemoryPreparedPdfDocumentRepository(),
                        new BuildPdfVisualDocumentUseCase(
                                new com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine())));
    }
}
