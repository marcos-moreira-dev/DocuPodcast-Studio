package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSidebarImageAudioT106BSourceTest {
    @Test
    void fragmentModuleOffersContinuationAndSingleFragmentPlaybackWithoutRedundantLayerButtons() throws Exception {
        String context = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(context.contains("Reproducir desde aquí"));
        assertTrue(context.contains("Reproducir fragmento"));
        assertTrue(context.contains("viewModel::playSelectedFragmentOnly"));
        assertTrue(viewModel.contains("playSelectedFragmentOnly"));
        assertTrue(viewModel.contains("playSingleCueOnly"));
        assertFalse(context.contains("Voz"));
        assertFalse(context.contains("Emoción"));
        assertFalse(context.contains("Imagen"));
    }

    @Test
    void imageModuleGuidesUnsavedProjectAndRefreshesPreviewAndRailAfterImport() throws Exception {
        String image = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java");
        String viewer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ImageFullscreenViewer.java");
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(image.contains("ExceptionAlertPresenter"));
        assertTrue(image.contains("Guarda el proyecto"));
        assertTrue(image.contains("documentMediaRevisionProperty"));
        assertTrue(image.contains("StableImageLoader.shared().load"));
        assertFalse(image.contains("new Image(normalized, false)"));
        assertTrue(image.contains("selectedImageUri"));
        assertTrue(image.contains("ImageFullscreenViewer.show"));
        assertTrue(viewer.contains("KeyCode.ESCAPE"));
        assertTrue(rail.contains("documentMediaRevisionProperty"));
        assertTrue(viewModel.contains("bumpDocumentMediaRevision"));
        assertTrue(viewModel.contains("refreshStoryboardFromImageLayers"));
    }

    @Test
    void removingImageRemovesProjectAssetWhenNoOtherLayerUsesIt() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(viewModel.contains("deleteProjectAssetFileIfPresent"));
        assertTrue(viewModel.contains("removeProjectAsset().remove"));
        assertTrue(viewModel.contains("bindingsForImage"));
        assertTrue(viewModel.contains("Imagen quitada y eliminada del proyecto"));
    }

    @Test
    void voiceToneComboHasExpandedReferenceToneCatalog() throws Exception {
        String tone = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceTone.java");
        String audio = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(audio.contains("ComboBox<VoiceReferenceTone>"));
        assertTrue(audio.contains("assignVoiceToneToSelectedDocumentRange"));
        assertTrue(tone.contains("HAPPY"));
        assertTrue(tone.contains("ENTHUSIASTIC"));
        assertTrue(tone.contains("SAD"));
        assertTrue(tone.contains("CALM"));
        assertTrue(tone.contains("layerTargetId()"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
