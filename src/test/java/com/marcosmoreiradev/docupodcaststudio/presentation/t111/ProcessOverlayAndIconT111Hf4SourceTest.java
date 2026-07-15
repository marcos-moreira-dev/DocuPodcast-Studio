package com.marcosmoreiradev.docupodcaststudio.presentation.t111;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProcessOverlayAndIconT111Hf4SourceTest {
    @Test
    void processOverlayCanBeHiddenAndRestoredFromStatusBar() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String overlay = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java");
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String css = read("src/main/resources/css/statusbar.css");

        assertTrue(shell.contains("processOverlayExpanded"));
        assertTrue(overlay.contains("ActionButtonFactory.secondary(\"Ocultar\""));
        assertTrue(overlay.contains("running && expanded.get()"));
        assertTrue(status.contains("Mostrar preparación"));
        assertTrue(status.contains("processOverlayExpanded.set(true)"));
        assertTrue(css.contains("status-process-button"));
    }

    @Test
    void iconSizesAreLargeEnoughForRibbonAndRails() throws Exception {
        String iconView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/IconView.java");
        String ribbonCss = read("src/main/resources/css/components/ribbon.css");
        String actionsCss = read("src/main/resources/css/components/actions.css");

        assertTrue(iconView.contains("new IconView(icon, 34"));
        assertTrue(iconView.contains("new IconView(icon, 30"));
        assertTrue(iconView.contains("new IconView(icon, 24"));
        assertTrue(ribbonCss.contains("T111-HF4 — larger PNG icon polish"));
        assertTrue(actionsCss.contains("T111-HF4 — larger PNG icon polish"));
    }

    @Test
    void oldDocumentSnapshotsRecoverEmbeddedImagesFromSourceDocx() throws Exception {
        String repo = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/ReadableDocumentWorkspaceRepository.java");
        assertTrue(repo.contains("enrichEmbeddedImagesFromSource"));
        assertTrue(repo.contains("word/media/"));
        assertTrue(repo.contains("embeddedImageBase64"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
