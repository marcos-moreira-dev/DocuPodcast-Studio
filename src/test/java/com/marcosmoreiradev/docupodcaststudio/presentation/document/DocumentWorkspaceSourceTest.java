package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentWorkspaceSourceTest {
    @Test
    void documentWorkspaceExposesContextualInspectorReadingBarTheatreRailAndSelection() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));

        assertTrue(source.contains("SplitPane"), "El workspace Documento debe reservar zonas laterales y centro.");
        assertTrue(source.contains("FloatingReadingControlBar"), "Debe montar la barra flotante de lectura global sobre la hoja.");
        assertTrue(source.contains("WorkspaceSideDock"), "Debe montar el inspector contextual izquierdo.");
        assertTrue(source.contains("DocumentWorkspaceMode.READING"), "Documento normal debe tener modo de lectura simple.");
        assertTrue(source.contains("DocumentWorkspaceMode.THEATRE_SCRIPT"), "Guion teatral reutiliza el documento con capa teatral.");
        assertTrue(source.contains("DocumentContextDetailsPanel"), "Debe existir modulo contextual de detalles del fragmento.");
        assertTrue(source.contains("DocumentAudioNarrationPanel"), "Debe existir modulo contextual de audio y narracion.");
        assertTrue(theatreDock.contains("DocumentImageContextPanel"), "El modulo Imagen se mueve al sidebar teatral.");
        assertTrue(theatreDock.contains("DocumentMediaRailView"), "El rail visual derecho vive en Teatro/Guion.");
        assertTrue(source.contains("selectedBlockId"), "Debe manejar seleccion de bloque/oracion para el inspector contextual.");
    }
}
