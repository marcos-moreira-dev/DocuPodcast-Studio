package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentComfortReadingSourceTest {
    @Test
    void documentReaderUsesComfortableTextAndKeepsTechnicalSettingsOutside() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/css/document-reader.css"))
                + Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(css.contains("Tanda 36 — lectura cómoda"));
        assertTrue(css.contains(".document-block-text"));
        assertTrue(css.contains("-fx-font-size: 18px"));
        assertTrue(css.contains("-fx-line-spacing: 7px"));
        assertTrue(css.contains(".document-page"));
        assertTrue(document.contains("document-page"));
        assertTrue(document.contains("FloatingReadingControlBar"));
        assertTrue(floating.contains("PrimaryActionStrip"));
        assertTrue(floating.contains("TransportControls"));
        assertTrue(document.contains("documentPrimaryActionLabelProperty"));
        assertTrue(viewModel.contains("Escuchar documento"));
        assertTrue(settings.contains("Tamaño base del documento"));
        assertTrue(settings.contains("18 px por defecto"));
    }
}
