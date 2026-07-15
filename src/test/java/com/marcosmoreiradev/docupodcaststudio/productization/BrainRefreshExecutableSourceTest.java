package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrainRefreshExecutableSourceTest {
    @Test
    void refreshSourceDocumentIsModeledInDomainAndApplicationNotOnlyInUi() throws IOException {
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/SourceDocumentSnapshot.java", "SHA-256");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/SourceDocumentChangeReport.java", "audio queda obsoleto");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/RefreshSourceDocumentUseCase.java", "DocumentSourceImportService");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/RefreshSourceDocumentUseCase.java", "UNSUPPORTED");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/RefreshSourceDocumentUseCase.java", "no edita ni sobrescribe");

        String contract = read("docs/productizacion/CONTRATO_CEREBRO_REFRESCO_FUENTE.md");
        assertTrue(contract.contains("UI → SourceDocumentRefreshCoordinator → RefreshSourceDocumentUseCase"));
        assertTrue(contract.contains("CHANGED → audio obsoleto"));
        assertTrue(contract.contains("V1 no necesita resolver automáticamente todos los conflictos de rangos"));
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
