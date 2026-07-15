package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfEmbeddedRendererT99SourceTest {
    @Test
    void pdfBoxDependenciesAndModuleAreDeclared() throws Exception {
        String pom = read("pom.xml");
        String module = read("src/main/java/module-info.java");

        assertTrue(pom.contains("<artifactId>pdfbox</artifactId>"));
        assertTrue(pom.contains("<version>3.0.7</version>"));
        assertTrue(pom.contains("<artifactId>jbig2-imageio</artifactId>"));
        assertTrue(pom.contains("<version>3.0.5</version>"));
        assertTrue(module.contains("requires org.apache.pdfbox;"));
    }

    @Test
    void renderContractLivesOutsidePresentation() throws Exception {
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfRenderEngine.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfVisualDocumentUseCase.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxRenderEngine.java")));

        String presentation = allJavaUnder(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"));
        assertFalse(presentation.contains("org.apache.pdfbox"));
        assertFalse(presentation.contains("PdfBoxRenderEngine"));
    }

    @Test
    void sourceCropRendererPrefersEmbeddedEngineBeforePopplerFallback() throws Exception {
        String cropRenderer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfSourceCropRenderer.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxRenderEngine.java");

        int embedded = cropRenderer.indexOf("renderEngine.renderCrop");
        int fallback = cropRenderer.indexOf("runPdftoppm");
        assertTrue(embedded > 0);
        assertTrue(fallback > embedded);
        assertTrue(engine.contains("setAnnotationsFilter"));
    }

    private static String allJavaUnder(Path root) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (var files = Files.walk(root)) {
            for (Path path : files.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                builder.append(Files.readString(path)).append('\n');
            }
        }
        return builder.toString();
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
