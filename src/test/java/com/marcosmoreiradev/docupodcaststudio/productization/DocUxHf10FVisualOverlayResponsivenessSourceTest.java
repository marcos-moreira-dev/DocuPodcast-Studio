package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF10F: cleaner source visuals and non-blocking audio generation overlay. */
final class DocUxHf10FVisualOverlayResponsivenessSourceTest {
    @Test
    void overlayDoesNotScanChunkFilesOnJavaFxRenderPath() throws Exception {
        String overlay = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java");
        assertTrue(overlay.contains("SIZE_PROBE_EXECUTOR"));
        assertTrue(overlay.contains("requestSizeProbe(dto)"));
        assertTrue(overlay.contains("Platform.runLater"));
        assertFalse(overlay.contains("eta.setText(dto.etaLabel() + \" · \" + dto.segmentCounterLabel()\n                + \" · \" + estimatedChunksSizeLabel(dto)"));
    }

    @Test
    void audioStatusDoesNotRebuildPlaybackManifestForPureBackgroundChunkGeneration() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(viewModel.contains("boolean playbackNeedsManifest"));
        assertTrue(viewModel.contains("!playbackNeedsManifest && status.running()"));
        assertTrue(viewModel.lines().count() <= 2700);
    }

    @Test
    void sourceTablesUseFullWidthTransversalGridWithoutExtraVisualCard() throws Exception {
        String visual = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java");
        String table = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceTableGridView.java");
        String css = read("src/main/resources/css/components/source-visual.css");
        assertTrue(visual.contains("new SourceVisualBlockView(title, detail, new SourceTableGridView(parsed), true)"));
        assertTrue(table.contains("ColumnConstraints"));
        assertTrue(table.contains("constraints.setPercentWidth(100.0 / columnCount)"));
        assertTrue(css.contains("ui-source-table-block"));
        assertTrue(css.contains("ui-source-table-header-cell"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
