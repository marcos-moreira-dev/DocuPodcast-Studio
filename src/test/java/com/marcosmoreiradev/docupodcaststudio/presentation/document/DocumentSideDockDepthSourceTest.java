package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSideDockDepthSourceTest {
    @Test
    void sideDockChromeBlendsWithDarkDocumentSurfaceWithoutChangingDockGeometry() throws Exception {
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-side-dock-depth.css"));

        assertTrue(imports.contains("compat-legacy.css"));
        assertTrue(imports.indexOf("compat-legacy.css") < imports.indexOf("document/document-side-dock-depth.css"));
        assertTrue(css.contains(".document-workspace .document-split > .split-pane-divider"));
        assertTrue(css.contains(".document-workspace .workspace-side-dock"));
        assertTrue(css.contains(".document-workspace .workspace-side-dock-expanded"));
        assertTrue(css.contains(".document-workspace .theatre-side-dock"));
        assertTrue(css.contains(".document-workspace .side-dock-rail"));
        assertTrue(css.contains(".document-workspace .workspace-side-dock-right .side-dock-module-frame"));
        assertTrue(css.contains("-fx-background-color: -docu-bg-document-edge"));
        assertTrue(css.contains("-fx-border-color: -docu-bg-document-edge"));
    }
}
