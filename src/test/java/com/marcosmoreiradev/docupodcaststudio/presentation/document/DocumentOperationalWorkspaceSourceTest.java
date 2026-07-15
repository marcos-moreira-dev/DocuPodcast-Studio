package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentOperationalWorkspaceSourceTest {
    @Test
    void documentWorkspaceStaysPrimaryOperationalSurfaceWithContextInspector() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String sideDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java"));

        assertTrue(document.contains("FloatingReadingControlBar"));
        assertTrue(document.contains("DocumentAudioNarrationPanel"));
        assertFalse(document.contains("SideDockModuleId.DOCUMENT_IMAGE"));
        assertTrue(theatreDock.contains("DocumentImageContextPanel"));
        assertTrue(document.contains("WorkspaceSideDock.RailPlacement.LEFT"));
        assertTrue(document.contains("this::railReadingControl"));
        assertTrue(sideDock.contains("workspace-side-dock-expanded"));
        assertFalse(document.contains("Estructura documental"));
    }
}
