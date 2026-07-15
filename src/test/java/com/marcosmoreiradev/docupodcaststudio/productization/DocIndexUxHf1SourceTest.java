package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-INDEX-UX-HF1 keeps the index module understandable as a document tree, not a UML-style graph. */
final class DocIndexUxHf1SourceTest {
    @Test
    void indexModuleCommunicatesTreeNavigationPurpose() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java"));
        String dock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java"));
        String icons = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppIcon.java"));

        assertTrue(workspace.contains("Árbol del documento: raíz, secciones y hojas")
                || workspace.contains("Ãrbol del documento: raÃ­z, secciones y hojas"));
        assertTrue(workspace.contains("\"Árbol\"") || workspace.contains("\"Ãrbol\""));
        assertTrue(panel.contains("Origen del indice"));
        assertTrue(panel.contains("DocumentOutlineProjection"));
        assertTrue(dock.contains("AppIcon.INDEX_TREE"));
        assertTrue(icons.contains("INDEX_TREE("));
    }
}
