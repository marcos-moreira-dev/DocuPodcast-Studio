package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentPrimaryOperationSourceTest {
    @Test
    void documentWorkspaceExposesSingleSmartPrimaryAction() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String ribbonCatalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));
        String strip = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/PrimaryActionStrip.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String provider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document-reader.css"))
                + Files.readString(Path.of("src/main/resources/css/document/document-page.css"))
                + Files.readString(Path.of("src/main/resources/css/components/actions.css"));

        assertTrue(document.contains("StackPane readingStage = new StackPane(documentScroll, readingControls)"));
        assertTrue(document.contains("StackPane.setAlignment(readingControls, Pos.TOP_CENTER)"));
        assertTrue(document.contains("new Insets(6, 24, 0, 24)"));
        assertTrue(document.contains("new FloatingReadingControlBar"));
        assertTrue(document.contains("documentPrimaryActionLabelProperty"));
        assertTrue(document.contains("documentPrimaryActionHintProperty"));
        assertTrue(document.contains("this::runPrimaryActionFromPlaybar"));
        assertTrue(document.contains("viewModel.selectDocumentBlock"));
        assertTrue(viewModel.contains("public void runDocumentPrimaryAction()"));
        assertTrue(viewModel.contains("public void selectDocumentBlock"));
        assertTrue(viewModel.contains("Reproducir desde aquí"));
        assertTrue(viewModel.contains("firstSegmentForDocumentBlock"));
        assertTrue(viewModel.contains("preferredPlaybackStartCue"));
        assertTrue(toolbar.contains("documentPrimaryActionLabelProperty"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.LISTEN_DOCUMENT)"));
        assertFalse(ribbon.contains("documentPrimaryActionLabelProperty"));
        assertFalse(ribbonCatalog.contains("AppCommandId.LISTEN_DOCUMENT"));
        assertFalse(ribbonCatalog.contains("AppCommandId.PLAY_SELECTION"));
        assertTrue(strip.contains("ObservableValue<String> actionLabel"));
        assertTrue(floating.contains("document-operation-strip"));
        assertTrue(floating.contains("PrimaryActionStrip"));
        assertTrue(floating.contains("TransportControls"));
        assertTrue(provider.contains("case DOCUMENT_READER"));
        assertTrue(provider.contains("WorkspaceCapability.LISTEN_DOCUMENT"));
        assertTrue(policy.contains("LISTEN_DOCUMENT -> viewModel.currentDocumentProperty().isNull()"));
        assertTrue(policy.contains("PREPARE_DOCUMENT_READING"));
        assertTrue(css.contains("document-operation-strip"));
        assertTrue(css.contains("ui-floating-reading-control"));
        assertTrue(css.contains("rgba(31, 41, 55, 0.42)"));
    }
}
