package com.marcosmoreiradev.docupodcaststudio.presentation.status;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StatusBarReadingZoomT102SourceTest {
    @Test
    void shellMountsStatusBarWithReadingZoomDependencies() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        assertTrue(shell.contains("new StatusBarView("));
        assertTrue(shell.contains("viewModel.readingFontSizeProperty()"));
        assertTrue(shell.contains("viewModel::decreaseReadingFontSize"));
        assertTrue(shell.contains("viewModel::resetReadingFontSize"));
        assertTrue(shell.contains("viewModel::increaseReadingFontSize"));
        assertTrue(shell.contains("viewModel::setReadingFontSize"));
    }

    @Test
    void statusBarOwnsCompactReadingZoomControl() throws Exception {
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String zoom = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/ReadingZoomControl.java");
        assertTrue(status.contains("ReadingZoomControl"));
        assertTrue(status.contains("HBox.setHgrow(spacer, Priority.ALWAYS)"));
        assertTrue(status.contains("private static void keepReadable(Label label)"));
        assertTrue(status.contains("label.setTextOverrun(OverrunStyle.CLIP)"));
        assertTrue(status.contains("keepReadable(prefix)"));
        assertTrue(status.contains("keepReadable(documentProgress)"));
        assertTrue(zoom.contains("new Slider("));
        assertTrue(zoom.contains("MIN_READING_FONT_SIZE"));
        assertTrue(zoom.contains("MAX_READING_FONT_SIZE"));
        assertTrue(zoom.contains("100%"));
        assertTrue(zoom.contains("Tooltip.install"));
        assertTrue(zoom.contains("no es zoom de lienzo"));
        assertTrue(zoom.contains("setMinWidth(Region.USE_PREF_SIZE)"));
        assertTrue(zoom.contains("setMaxWidth(Region.USE_PREF_SIZE)"));
        assertTrue(zoom.contains("button.setTextOverrun(OverrunStyle.CLIP)"));
        assertTrue(zoom.contains("keepReadable(caption)"));
        assertTrue(zoom.contains("keepReadable(percent)"));
    }

    @Test
    void viewModelDelegatesReadingFontSizePersistenceToCoordinator() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ReadingComfortCoordinator.java");
        assertTrue(viewModel.contains("MIN_READING_FONT_SIZE = 14"));
        assertTrue(viewModel.contains("DEFAULT_READING_FONT_SIZE = 18"));
        assertTrue(viewModel.contains("MAX_READING_FONT_SIZE = 28"));
        assertTrue(viewModel.contains("IntegerProperty readingFontSize"));
        assertTrue(viewModel.contains("readingFontSizeProperty()"));
        assertTrue(viewModel.contains("decreaseReadingFontSize()"));
        assertTrue(viewModel.contains("increaseReadingFontSize()"));
        assertTrue(viewModel.contains("resetReadingFontSize()"));
        assertTrue(viewModel.contains("setReadingFontSize(int requestedSize)"));
        assertTrue(viewModel.contains("ReadingComfortCoordinator"));
        assertTrue(viewModel.contains("readingComfortWorkflow.persistFontSize(next)"));
        assertTrue(coordinator.contains("loadOperationalSettings().load()"));
        assertTrue(coordinator.contains("saveOperationalSettings().save(updated)"));
        assertTrue(coordinator.contains("new OperationalSettings.ReadingDocumentSettings"));
    }

    @Test
    void documentReaderReflowsTextWithCssSizeClassesNotInlineStyles() throws Exception {
        String documentView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String css = read("src/main/resources/css/document/document-page.css");
        assertTrue(documentView.contains("READING_SIZE_CLASS_PREFIX"));
        assertTrue(documentView.contains("document-reader-size-"));
        assertTrue(documentView.contains("viewModel.readingFontSizeProperty().addListener"));
        assertFalse(documentView.contains("setStyle("));
        assertTrue(css.contains(".document-reader-size-14 .document-block-text"));
        assertTrue(css.contains(".document-reader-size-18 .document-block-text"));
        assertTrue(css.contains(".document-reader-size-28 .document-block-text"));
    }

    @Test
    void cssStylesZoomControlAsStatusBarElement() throws Exception {
        String css = read("src/main/resources/css/statusbar.css");
        assertTrue(css.contains(".reading-zoom-control"));
        assertTrue(css.contains(".reading-zoom-slider"));
        assertTrue(css.contains(".reading-zoom-percent"));
        assertTrue(css.contains(".reading-zoom-button"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
