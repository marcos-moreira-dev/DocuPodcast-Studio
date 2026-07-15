package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentActiveReadingFollowSourceTest {
    @Test
    void documentWorkspaceFollowsPlaybackCursorWithoutExposingTechnicalScaffolding() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String anchor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ActiveReadingAnchor.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document-reader.css"))
                + Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String docs = Files.readString(Path.of("docs/56_TANDA_37_SEGUIMIENTO_ORACION_ACTIVA.md"));

        assertTrue(document.contains("playbackCursorProperty()"));
        assertTrue(document.contains("syncActivePlaybackBlock"));
        assertTrue(document.contains("sourceBlockIds"));
        assertTrue(document.contains("Platform.runLater"));
        assertTrue(document.contains("document-block-active-reading"));
        assertTrue(anchor.contains("upperComfortBand"));
        assertTrue(anchor.contains("zona estable de lectura"));
        assertTrue(css.contains("Tanda 37"));
        assertTrue(css.contains("-fx-underline: true"));
        assertTrue(settings.contains("Mantener oración activa cerca del centro")
                || settings.contains("La oración activa deberá mantenerse en una franja estable"));
        assertTrue(docs.contains("tipo MuseScore"));
    }
}
