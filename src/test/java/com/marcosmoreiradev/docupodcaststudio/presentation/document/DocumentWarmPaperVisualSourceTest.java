package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentWarmPaperVisualSourceTest {
    @Test
    void documentAreaUsesDarkViewportWhitePageSoftShadowAndScopedScrollbar() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String page = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String scrollbar = Files.readString(Path.of("src/main/resources/css/document/document-scrollbar.css"));
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String view = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(view.contains("documentSurface.getStyleClass().add(\"document-surface\")"));
        assertTrue(view.contains("documentScroll.getStyleClass().add(\"document-scroll\")"));
        assertTrue(view.contains("pageHost.getStyleClass().add(\"document-page-host\")"));
        assertTrue(view.contains("content.getStyleClass().add(\"document-page\")"));

        assertTrue(tokens.contains("-docu-bg-document: #FFFFFF"));
        assertTrue(tokens.contains("-docu-bg-document-edge: #30415D"));
        assertTrue(tokens.contains("-docu-border-document: #B8B5AE"));
        assertTrue(tokens.contains("-docu-shadow-document: dropshadow(gaussian, rgba(5, 12, 24, 0.36), 24, 0.18, 0, 7)"));
        assertTrue(tokens.contains("-docu-scroll-document-track: #3A4D69"));
        assertTrue(tokens.contains("-docu-scroll-document-thumb: #AFC3DA"));
        assertTrue(tokens.contains("-docu-scroll-document-thumb-hover: #C7D6E6"));
        assertTrue(tokens.contains("-docu-scroll-document-thumb-active: #91ABC7"));

        assertTrue(page.contains(".document-scroll > .viewport"));
        assertTrue(page.contains(".document-scroll > .corner"));
        assertTrue(page.contains(".document-workspace .document-page"));
        assertTrue(page.contains("-fx-background-color: -docu-bg-document-edge"));
        assertTrue(page.contains("-fx-background-color: -docu-bg-document"));
        assertTrue(page.contains("-fx-border-color: -docu-border-document"));
        assertTrue(page.contains("-fx-effect: -docu-shadow-document"));
        assertTrue(imports.contains("document/document-scrollbar.css"));
        assertTrue(scrollbar.contains(".document-workspace .document-scroll .scroll-bar:vertical"));
        assertTrue(scrollbar.contains("-fx-pref-width: 9px"));
        assertTrue(scrollbar.contains("-docu-scroll-document-track"));
        assertTrue(scrollbar.contains("-docu-scroll-document-thumb-hover"));
        assertTrue(scrollbar.contains("-docu-scroll-document-thumb-active"));
        assertTrue(scrollbar.contains(".thumb:pressed"));
    }
}
