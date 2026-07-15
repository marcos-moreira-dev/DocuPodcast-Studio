package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentMiniStoryboardRailSourceTest {
    @Test
    void theatreRailShowsRealStoryboardThumbnailsAndHighlightsAssignedText() throws Exception {
        String documentWorkspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String mediaCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/MediaThumbnailCard.java"));
        String imageProjection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRailImagePresentation.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));
        String appStyles = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java"));

        assertFalse(documentWorkspace.contains("new DocumentMediaRailView(viewModel)"),
                "Documento simple no debe montar el rail visual derecho.");
        assertTrue(theatreDock.contains("new DocumentMediaRailView(viewModel, () ->"));
        assertTrue(documentWorkspace.contains("scrollToDocumentBlock(normalized)"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertFalse(mediaRail.contains("railTitle(\"Im"));
        assertTrue(mediaRail.contains("MediaThumbnailCard"));
        assertTrue(mediaRail.contains("Borrar im"));
        assertTrue(mediaRail.contains("document-media-actions-vertical"));
        assertTrue(mediaRail.contains("fullWidthAction(editFrameButton)"));
        assertTrue(mediaRail.contains("fullWidthAction(storyboardOverviewButton)"));
        assertTrue(mediaRail.contains("Ocultar acciones visuales"));
        assertTrue(mediaRail.contains("selectDocumentFragmentRailItem"));
        assertTrue(mediaRail.contains("variantImageUri"));
        assertTrue(mediaRail.contains("TheatreVisualVariant.values()"));
        assertFalse(mediaRail.contains("selectLooseStoryboardImage"));
        assertTrue(mediaCard.contains("ImageView"));
        assertTrue(appStyles.contains("UI_MEDIA_THUMBNAIL_CARD"));
        assertTrue(imageProjection.contains("Sin texto asignado"));
        assertTrue(imageProjection.contains("assignedBlockId"));
        assertTrue(viewModel.contains("documentRailImagePresentations"));
        assertTrue(viewModel.contains("assetUri"));
        assertTrue(viewModel.contains("selectDocumentBlockForStoryboardSegment"));
        assertTrue(css.contains("ui-media-thumbnail"));
        assertTrue(css.contains("document-media-image-unassigned"));
    }
}
