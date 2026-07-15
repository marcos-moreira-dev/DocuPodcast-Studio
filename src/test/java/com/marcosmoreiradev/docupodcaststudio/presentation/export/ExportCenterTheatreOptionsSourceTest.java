package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterTheatreOptionsSourceTest {
    @Test
    void exportCenterEmbedsStyledTargetSpecificOptionsWithoutFollowUpDialogs() throws Exception {
        String dialog = read("presentation/export/ExportCenterDialog.java");
        String shell = read("presentation/shell/DocuPodcastShellView.java");
        String portion = read("presentation/theatre/TheatrePortionExportOptionsPane.java");
        String map = read("presentation/theatre/TheatreMapExportOptionsPane.java");
        String video = read("presentation/video/VideoEncodingOptionsPane.java");

        assertTrue(dialog.contains("TheatreMapExportOptionsPane"));
        assertTrue(dialog.contains("TheatrePortionExportOptionsPane"));
        assertTrue(dialog.contains("VideoEncodingOptionsPane"));
        assertTrue(dialog.contains("ExportCenterContext"));
        assertTrue(shell.contains("handleExportTheatreWork(selection.videoOptions())"));
        assertTrue(shell.contains("handleExportTheatreSpatialView(selection.theatreMapOptions())"));
        assertTrue(shell.contains("handleExportTheatrePortion(selection.theatrePortionOptions())"));
        assertTrue(shell.contains("handleExportSimpleVideo(selection.videoOptions())"));
        assertTrue(portion.contains("Todo el acto"));
        assertTrue(portion.contains("selectedAct.id().equals(scene.actId())"));
        assertTrue(map.contains("TheatreMapCompanionMode"));
        assertTrue(video.contains("StudioFormControls.combo"));
        assertFalse(shell.contains("case EXPORT_THEATRE_WORK -> handleExportTheatreWork();"));
        assertFalse(dialog.contains("TheatreWorkExportOptionsPane"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/" + relative));
    }
}
