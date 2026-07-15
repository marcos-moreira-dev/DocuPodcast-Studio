package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF10G: memory guardrails for long documents, progress overlay and source tables. */
final class DocUxHf10GMemoryRailOverlayTablesSourceTest {
    @Test
    void rightVisualRailIsVirtualizedForLongDocuments() throws Exception {
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String css = read("src/main/resources/css/components/media-rail.css");

        assertTrue(rail.contains("ListView<DocumentFragmentRailPresentation>"));
        assertTrue(rail.contains("setCellFactory"));
        assertTrue(rail.contains("FragmentRailCell"));
        assertTrue(rail.contains("DocumentRailReconciler.reconcile"));
        assertFalse(rail.contains("storyboardItems.getItems().setAll"));
        assertTrue(rail.contains("pseudoClassStateChanged(VISUAL_SELECTED"));
        assertTrue(rail.contains("pseudoClassStateChanged(PLAYBACK_ACTIVE"));
        assertFalse(rail.contains("new VBox(8)"));
        assertFalse(rail.contains("storyboardItems.getChildren().add(fragmentCard"));
        assertTrue(css.contains("DOC-UX-HF10G"));
        assertTrue(css.contains("document-media-virtual-list"));
    }

    @Test
    void audioProgressIsThrottledBeforeJavaFxUpdates() throws Exception {
        String throttle = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/AudioStatusUiThrottle.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(throttle.contains("docupodcast-audio-status-ui-throttle"));
        assertTrue(throttle.contains("pending = status"));
        assertTrue(throttle.contains("Platform.runLater"));
        assertTrue(viewModel.contains("AudioStatusUiThrottle"));
        assertTrue(viewModel.contains("audioStatusUiThrottle.submit(status)"));
        assertFalse(viewModel.contains("playbackCursor.get() != null"));
        assertTrue(viewModel.contains("playbackTransport.continuationActive()"));
    }

    @Test
    void sourceTablesPreferVerticalGrowthInsteadOfEllipsis() throws Exception {
        String table = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceTableGridView.java");
        String visual = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java");
        String importer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java");
        String css = read("src/main/resources/css/components/source-visual.css");

        assertTrue(table.contains("OverrunStyle.CLIP"));
        assertTrue(table.contains("Region.USE_PREF_SIZE"));
        assertTrue(table.contains("GridPane.setVgrow(cell, Priority.ALWAYS)"));
        assertFalse(table.contains("filas más"));
        assertTrue(visual.contains("new SourceTableGridView(parsed)"));
        assertFalse(importer.contains("abbreviate(textContentOf(cell)"));
        assertFalse(importer.contains("Math.min(6, rows.size())"));
        assertTrue(css.contains("DOC-TABLE-HF11B / DOC-TABLE-HF11C / DOC-UX-HF10G"));
    }

    @Test
    void localRunUsesLargerHeapForLargeDocuments() throws Exception {
        String script = read("scripts/01-ejecutar-app.bat");
        String pom = read("pom.xml");

        assertTrue(script.contains("DOCUPODCAST_APP_HEAP"));
        assertTrue(script.contains("-Xmx2048m"));
        assertTrue(script.contains("-Ddocupodcast.app.heap=%DOCUPODCAST_APP_HEAP%"));
        assertTrue(pom.contains("<docupodcast.app.heap>-Xmx2048m</docupodcast.app.heap>"));
        assertTrue(pom.contains("<option>${docupodcast.app.heap}</option>"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
