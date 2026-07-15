package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF10H: readable audio progress, real generated size and honest video setup copy. */
final class DocUxHf10HProgressVideoRailSourceTest {
    @Test
    void audioOverlayShowsReadableGreenProgressAndGeneratedSize() throws Exception {
        String overlay = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java");
        String css = read("src/main/resources/css/components/process-overlay.css");

        assertTrue(overlay.contains("audio generado en disco"));
        assertTrue(overlay.contains("generatedChunksSizeLabel"));
        assertFalse(overlay.contains("tamaño estimado"));
        assertTrue(css.contains("-fx-pref-height: 13px"));
        assertTrue(css.contains(".process-overlay-progress > .bar"));
        assertTrue(css.contains("#22C55E"));
    }

    @Test
    void etaUsesHoursWhenLongJobsTakeMoreThanOneHour() throws Exception {
        String dto = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobStatusDto.java");
        String row = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioJobRow.java");

        assertTrue(dto.contains("hours = seconds / 3600L"));
        assertTrue(dto.contains("return hours + \" h \" + minutes + \" min \" + rest + \" s\""));
        assertTrue(row.contains("hours = value / 3600L"));
        assertTrue(row.contains("return hours + \" h \" + minutes + \" min \" + rest + \" s\""));
    }

    @Test
    void visualRailExplainsVirtualizedCardsAndAvoidsHorizontalScroll() throws Exception {
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String card = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/MediaThumbnailCard.java");
        String css = read("src/main/resources/css/components/media-rail.css");

        assertTrue(rail.contains("fragmentCountLabel"));
        assertTrue(rail.contains("fragmentos listados"));
        assertTrue(rail.contains("card.setPrefWidth(Math.max(160.0, getListView().getWidth() - 22.0))"));
        assertTrue(card.contains("setMinWidth(0)"));
        assertTrue(css.contains("DOC-UX-HF10H"));
        assertTrue(css.contains(".document-media-virtual-list .scroll-bar:horizontal"));
    }

    @Test
    void videoSetupCopyMakesUrlAndFolderImportResponsibilitiesExplicit() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String videoLocal = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java");
        String settingsSurface = dialog + videoLocal;

        assertTrue(dialog.contains("URL para preparar video local"));
        assertTrue(dialog.contains("descarga automática queda centralizada aquí"));
        assertTrue(dialog.contains("Importar carpeta"));
        assertFalse(settingsSurface.contains("todavía no descarga desde internet"));
        assertTrue(settingsSurface.contains("ffmpeg.exe y ffprobe.exe"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
