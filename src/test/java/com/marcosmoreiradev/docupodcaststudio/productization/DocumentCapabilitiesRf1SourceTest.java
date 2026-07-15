package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentCapabilitiesRf1SourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void documentCapabilitiesExistAsApplicationContract() throws Exception {
        Path capabilities = ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/DocumentSourceCapabilities.java");
        Path useCase = ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/InspectDocumentSourceCapabilitiesUseCase.java");
        String source = Files.readString(capabilities);
        assertTrue(Files.exists(useCase));
        assertTrue(source.contains("HIERARCHICAL_OUTLINE"));
        assertTrue(source.contains("EMBEDDED_SOURCE_VISUALS"));
        assertTrue(source.contains("VISUAL_PAGE_RENDERING"));
        assertTrue(source.contains("NATIVE_TEXT_REQUIRED"));
        assertTrue(source.contains("LINEAR_TEXT_ONLY"));
    }

    @Test
    void wordIsRichButPdfAndTxtDoNotPretendToHaveSameCapabilities() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/DocumentSourceCapabilities.java"));
        assertTrue(source.contains("case DOCX"));
        assertTrue(source.contains("case PDF"));
        assertTrue(source.contains("case TXT"));
        assertTrue(source.contains("render visual embebido por pagina"));
        assertTrue(source.contains("Texto plano ofrece lectura lineal"));
    }
}
