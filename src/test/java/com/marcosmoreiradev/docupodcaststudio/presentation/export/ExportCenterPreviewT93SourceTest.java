package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterPreviewT93SourceTest {
    @Test
    void documentTextVideoOptionsHaveStyledControlsAndLiveFramePreview() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/export-center.css"));

        assertTrue(dialog.contains("StudioFormControls.combo(backgroundMode"));
        assertTrue(dialog.contains("ActionButtonFactory.secondary("));
        assertTrue(dialog.contains("export-center-frame-preview"));
        assertTrue(dialog.contains("La luz escribe despacio sobre la pagina"));
        assertTrue(dialog.contains("updatePreview()"));
        assertTrue(css.contains(".export-center-frame-preview"));
    }
}
