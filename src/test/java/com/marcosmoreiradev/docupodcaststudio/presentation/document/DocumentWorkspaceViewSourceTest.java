package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentWorkspaceViewSourceTest {
    @Test
    void exposesContextualDocumentSideDockModules() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String studyDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));

        assertTrue(source.contains("Fragmento"));
        assertTrue(source.contains("Texto"));
        assertTrue(source.contains("Audio"));
        assertTrue(source.contains("DocumentContextDetailsPanel"));
        assertTrue(source.contains("DocumentAudioNarrationPanel"));
        assertTrue(source.contains("DocumentStudySideDock"));
        assertTrue(studyDock.contains("DocumentTechnicalProblemPanel"));
        assertTrue(source.contains("problemCheckBox"));
        assertTrue(source.contains("prepareStudySourceCrops()"));
        assertTrue(source.contains("sourceCropPaths()"));
        assertTrue(source.contains("WorkspaceSideDock"));
        assertFalse(source.contains("SideDockModuleId.DOCUMENT_IMAGE"),
                "La lectura documental no debe mostrar el modulo Imagen.");
        assertTrue(theatreDock.contains("Capas multimedia"));
        assertTrue(theatreDock.contains("DocumentImageContextPanel"));
        assertFalse(source.contains("Estructura documental"));
        assertFalse(source.contains("Perfil de lectura"));
    }
}
