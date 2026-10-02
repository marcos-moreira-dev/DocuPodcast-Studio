package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CreatePreparedPdfSessionWorkspaceUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void closesOnlyTheManagedUnsavedWorkspaceAndNeverTheExternalSource() throws Exception {
        Path external = tempDir.resolve("fuente con ñ.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(external.toFile());
        }
        CreatePreparedPdfSessionWorkspaceUseCase useCase =
                new CreatePreparedPdfSessionWorkspaceUseCase(
                        new JsonPreparedPdfDocumentRepository(),
                        new BuildPdfVisualDocumentUseCase(new PdfBoxRenderEngine()));

        PreparedPdfSource staged = useCase.create(external, "Fuente con tildes");
        Path managedRoot = staged.workspace().projectRoot();

        assertTrue(useCase.isSessionWorkspace(staged));
        assertTrue(Files.isRegularFile(managedRoot.resolve("document/manifest.json")));
        useCase.closeIfSessionWorkspace(staged);

        assertFalse(Files.exists(managedRoot));
        assertTrue(Files.isRegularFile(external));
    }
}
