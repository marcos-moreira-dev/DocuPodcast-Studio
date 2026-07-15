package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNativeTextLayerT108SourceTest {
    @Test
    void nativePdfTextLayerIsApplicationBoundaryOnly() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfNativeTextLayerUseCase.java");
        String presentation = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"))
                + Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java"));

        assertTrue(services.contains("BuildPdfNativeTextLayerUseCase buildPdfNativeTextLayer"));
        assertTrue(factory.contains("new BuildPdfNativeTextLayerUseCase()"));
        assertTrue(useCase.contains("PdfTextLayerOrigin.NATIVE_BBOX"));
        assertTrue(useCase.contains("PdfTextLayerOrigin.UNAVAILABLE"));
        assertFalse(presentation.contains("BuildPdfNativeTextLayerUseCase"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
