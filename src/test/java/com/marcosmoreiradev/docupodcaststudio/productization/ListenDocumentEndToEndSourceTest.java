package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenDocumentEndToEndSourceTest {
    @Test
    void documentListeningIsModeledAsEndToEndBrainNotOnlyAsButtonHandler() throws IOException {
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PrepareDocumentListeningUseCase.java", "Escuchar documento");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/DocumentListenPlan.java", "open source → narrated document");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java", "prepareDocumentListening");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentNarrationCoordinator.java", "listeningPlan");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java", "plan.requiresNarrationProjection()");
        assertFileContains("docs/productizacion/ESCUCHAR_DOCUMENTO_END_TO_END_T72.md", "Documento narrable → narración interna → audio/buffer → playback");
    }

    private static void assertFileContains(String path, String expected) throws IOException {
        assertTrue(read(path).contains(expected), path + " debe contener: " + expected);
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
