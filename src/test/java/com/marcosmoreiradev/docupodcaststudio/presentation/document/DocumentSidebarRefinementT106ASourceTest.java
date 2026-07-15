package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSidebarRefinementT106ASourceTest {
    @Test
    void fragmentPanelDoesNotDuplicateAudioAndImageActions() throws Exception {
        String fragment = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java"));
        assertTrue(fragment.contains("Reproducir desde aquí"));
        assertTrue(fragment.contains("DocuPodcast lo preparará con la voz base"));
        assertFalse(fragment.contains("Button voice"));
        assertFalse(fragment.contains("Button image"));
        assertFalse(fragment.contains("Button emotion"));
        assertFalse(fragment.contains("new RailActionRow(voice"));
    }

    @Test
    void audioPanelUsesCombosForAiVoiceAndStyleWithoutPrepareAudioButton() throws Exception {
        String audio = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        assertTrue(audio.contains("ComboBox<VoiceProfile>"));
        assertTrue(audio.contains("ComboBox<VoiceReferenceTone>"));
        assertTrue(audio.contains("assignVoiceToSelectedDocumentRange"));
        assertTrue(audio.contains("assignVoiceToneToSelectedDocumentRange"));
        assertTrue(audio.contains("Elegir audio"));
        assertTrue(audio.contains("Extraer audio de video"));
        assertFalse(audio.contains("Preparar audio"));
        assertFalse(audio.contains("Quitar emoción"));
        assertFalse(audio.contains("Quitar emocion"));
    }

    @Test
    void imagePanelImportsAssignsAndPreviewsWithoutReplaceButton() throws Exception {
        String image = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String viewer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ImageFullscreenViewer.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(image.contains("ImageView"));
        assertTrue(image.contains("document-image-preview-box"));
        assertTrue(image.contains("importImageForSelectedDocumentRange"));
        assertTrue(image.contains("Quitar imagen"));
        assertTrue(image.contains("Pantalla completa (se pausa la reproduccion)"));
        assertTrue(image.contains("ImageFullscreenViewer.show"));
        assertTrue(viewer.contains("Escape para cerrar imagen"));
        assertTrue(viewer.contains("document-image-fullscreen-root"));
        assertTrue(image.contains("bindToSelectedImage(viewFull)"));
        assertTrue(image.contains("bindToSelectedImage(remove)"));
        assertFalse(image.contains("Reemplazar imagen"));
        assertTrue(viewModel.contains("importImageForSelectedDocumentRange(Path imageFile, NarrativeLayerKind imageKind)"));
        assertTrue(viewModel.contains("selectedDocumentImageUri()"));
    }

    @Test
    void sidebarsUseFullHeightScrollPanes() throws Exception {
        String fragment = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java"));
        String audio = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String image = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        assertTrue(fragment.contains("VBox.setVgrow(scroll, Priority.ALWAYS)"));
        assertTrue(audio.contains("VBox.setVgrow(scroll, Priority.ALWAYS)"));
        assertTrue(image.contains("VBox.setVgrow(scroll, Priority.ALWAYS)"));
        assertTrue(rail.contains("VBox.setVgrow(storyboardItems, Priority.ALWAYS)"));
    }
}
