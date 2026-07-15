package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentWorkspaceSideDockSourceTest {
    @Test
    void documentWorkspaceUsesReusableSideDockAsContextInspector() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String ids = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockStatePolicy.java"));

        assertTrue(source.contains("WorkspaceSideDock"));
        assertTrue(source.contains("DOCUMENT_CONTEXT_DETAILS"));
        assertTrue(source.contains("DOCUMENT_INDEX"));
        assertTrue(source.contains("DocumentIndexPanel"));
        assertTrue(source.contains("DOCUMENT_AUDIO_NARRATION"));
        assertFalse(source.contains("SideDockModuleId.DOCUMENT_IMAGE"),
                "Documento simple no debe exponer el modulo de Imagen en el sidebar izquierdo.");
        assertTrue(source.contains("WorkspaceSideDock.RailPlacement.LEFT"));
        assertTrue(source.contains("this::railReadingControl"));
        assertTrue(theatreDock.contains("THEATRE_FRAGMENT_IMAGES"),
                "Teatro/Guion es el dueño del modulo de imagenes por fragmento.");
        assertTrue(theatreDock.contains("DocumentImageContextPanel"));
        assertTrue(theatreDock.contains("DocumentMediaRailView"));
        assertTrue(ids.contains("DOCUMENT_CONTEXT_DETAILS"));
        assertTrue(ids.contains("DOCUMENT_INDEX"));
        assertTrue(ids.contains("THEATRE_FRAGMENT_IMAGES"));
        assertTrue(policy.contains("DOCUMENT_CONTEXT_DETAILS"));
        assertTrue(policy.contains("DOCUMENT_INDEX"));
    }
}
