package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterT95SourceTest {
    @Test
    void exportCenterDetailsScrollAndPreviewHasNoTranslucentInnerPanel() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/export-center.css"));

        assertTrue(dialog.contains("ScrollPane detailScroll"));
        assertTrue(dialog.contains("detailScroll.setFitToWidth(true)"));
        assertTrue(dialog.contains("detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED)"));
        assertTrue(css.contains(".export-center-detail-scroll"));
        assertTrue(css.contains(".export-center-frame-preview-copy"));
        assertTrue(css.contains("-fx-background-color: transparent"));
        assertTrue(css.contains("-fx-padding: 0"));
    }
}
