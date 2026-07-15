package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreStoryboardOverviewDialogSourceTest {
    @Test
    void overviewUsesVisualSequenceMapWithFrameActions() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreStoryboardOverviewDialog.java");
        String map = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreVisualSequenceMap.java");
        String zigzag = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreZigzagLayout.java");
        String css = read("src/main/resources/css/components/theatre-history-board.css");
        String imports = read("src/main/resources/css/docupodcast-light.css");

        assertTrue(dialog.contains("stage.setTitle(\"History board\")"));
        assertTrue(dialog.contains("new TheatreVisualSequenceMap(interventionItems("));
        assertTrue(dialog.contains("ImageFullscreenViewer.show"));
        assertTrue(dialog.contains("toggleTheatreStoryboardFrameVariant"));
        assertTrue(dialog.contains("FileChooser"));
        assertTrue(dialog.contains("Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING)"));
        assertTrue(dialog.contains("Exportar frame"));
        assertTrue(dialog.contains("imagePath"));
        assertFalse(dialog.contains("FlowPane cards"));
        assertFalse(dialog.contains("Alternar variante\", () ->"));

        assertTrue(map.contains("TheatreZigzagLayout.pointsFor("));
        assertTrue(map.contains("AppIcon.FULLSCREEN"));
        assertTrue(map.contains("AppIcon.REFRESH"));
        assertTrue(map.contains("AppIcon.SAVE"));
        assertTrue(map.contains("document-media-thumbnail-corner-action"));
        assertTrue(map.contains("Boceto pendiente"));

        assertTrue(zigzag.contains("pointsFor(int count,"));
        assertTrue(zigzag.contains("heightFor(int aliasCount, double top"));
        assertTrue(css.contains(".theatre-visual-sequence-map"));
        assertTrue(css.contains(".theatre-visual-sequence-line"));
        assertTrue(imports.contains("components/theatre-history-board.css"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
