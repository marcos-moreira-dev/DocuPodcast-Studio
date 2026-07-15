package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** VIDEO-SMOKE-RC1 protects final MP4 smoke with readiness before any long FFmpeg render. */
final class VideoSmokeRc1SourceTest {
    @Test
    void finalVideoSmokeUsesReadinessAndFfmpegEvidenceBeforeRendering() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/InspectFinalVideoSmokeReadinessUseCase.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ExportApplicationServices.java"));
        String script = Files.readString(Path.of("scripts/38-smoke-video-final.bat"));

        assertTrue(useCase.contains("FINAL_VIDEO_MP4"));
        assertTrue(useCase.contains("readyForFinalVideo()"));
        assertTrue(useCase.contains("Smoke de video final no listo"));
        assertTrue(services.contains("inspectFinalVideoSmokeReadiness"));
        assertTrue(script.contains("VideoSmokeRc1SourceTest"));
        assertTrue(!useCase.contains("javafx"));
    }
}
