package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoPanelSourceTest {
    @Test
    void exposesDocumentaryControlsAndConditionalSideDockModule() throws Exception {
        String panel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudyVideoPanel.java"));
        String sideDock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));
        String css = Files.readString(Path.of(
                "src/main/resources/css/document/document-study-video.css"));
        String sketchDialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentParagraphSketchDialog.java"));

        assertTrue(panel.contains("Titulo del video"));
        assertTrue(panel.contains("Duracion de tablas"));
        assertTrue(panel.contains("Contenido del video"));
        assertTrue(panel.contains("Elegir imagen"));
        assertTrue(panel.contains("Dibujar/editar"));
        assertTrue(panel.contains("Elegir mascota/logo"));
        assertTrue(panel.contains("Agregar musica"));
        assertTrue(panel.contains("Agregar nuevo parrafo final"));
        assertTrue(panel.contains("La musica de fondo continua durante toda su duracion"));
        assertTrue(panel.contains("Elegir imagen final"));
        assertTrue(panel.contains("document-study-video-music-name"));
        assertTrue(panel.contains("new Label(\"Volumen\")"));
        assertTrue(panel.contains("Usar imagen principal en el parrafo siguiente"));
        assertTrue(panel.contains("Aplicar mascota a todos los parrafos restantes"));
        assertTrue(panel.contains("Deshabilitar este contenido"));
        assertTrue(panel.contains("Habilitar este contenido"));
        assertTrue(panel.contains("withBlockEnabled"));
        assertTrue(panel.contains("new CollapsibleModuleSplitPane("));
        assertTrue(panel.contains("Acciones del contenido"));
        assertTrue(panel.contains("document-study-video-inspector-scroll"));
        assertTrue(panel.contains("document-study-video-content-scroll"));
        assertTrue(css.contains("document-study-video-card-disabled"));
        assertTrue(css.contains("document-study-video-workspace-split"));
        assertTrue(css.contains("document-study-video-music-volume-label"));
        assertTrue(sketchDialog.contains("surface.inkInputLayer().setMouseTransparent(false)"));
        assertTrue(sketchDialog.contains("new ScrollPane(canvasHost)"));
        assertFalse(panel.contains("Fragmento"));
        assertTrue(sideDock.contains("DOCUMENT_STUDY_VIDEO"));
        assertTrue(sideDock.contains("documentaryVideoConfigurationAvailable"));
    }
}
