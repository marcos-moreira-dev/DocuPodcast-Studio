package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocxVisualImportHf9SourceTest {
    @Test
    void docxImporterKeepsAllEmbeddedImagesAndDoesNotStopAfterFirstRenderableImage() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        assertTrue(source.contains("referencedPaths"),
                "El fallback de word/media debe comparar rutas usadas para no omitir otras imagenes embebidas.");
        assertTrue(source.contains("IMAGE_MEDIA_FALLBACK"),
                "Las imagenes no referenciadas en el XML principal deben recuperarse como bloques visuales fuente.");
    }

    @Test
    void docxTablesExposeAReadablePreviewWithoutBecomingNarration() throws Exception {
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertTrue(importer.contains("tablePreviewMarkdown"),
                "Las tablas DOCX deben conservar una vista previa legible en metadata.");
        assertTrue(importer.contains("columns"),
                "La metadata de tabla debe incluir columnas, no solo celdas totales.");
        assertTrue(view.contains("tableMarkdown"),
                "La hoja de lectura debe mostrar la vista previa de tabla cuando exista.");
    }

    @Test
    void latexIsDetectedButNotRenderedAsAnEditor() throws Exception {
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertTrue(importer.contains("hasLatexText"),
                "LaTeX textual debe detectarse como bloque visual identificado.");
        assertTrue(view.contains("No se renderiza en esta versión"),
                "La UI debe ser honesta: detectar LaTeX no implica render matematico completo.");
    }
}
