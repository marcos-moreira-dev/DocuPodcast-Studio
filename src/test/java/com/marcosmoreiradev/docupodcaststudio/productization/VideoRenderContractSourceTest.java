package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoRenderContractSourceTest {
    @Test
    void videoRenderContractIsExplicitAndAuditableBeforeRc() throws Exception {
        String commandPlan = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/VideoRenderCommandPlan.java"));
        String buildUseCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildVideoRenderCommandPlanUseCase.java"));
        String exportUseCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportSimpleVideoPackageUseCase.java"));
        String docs = Files.readString(Path.of("docs/productizacion/VIDEO_RENDER_CONTRACT_T76.md"));

        String buildUseCaseNormalized = buildUseCase.toLowerCase(Locale.ROOT);
        String docsNormalized = docs.toLowerCase(Locale.ROOT);

        assertTrue(commandPlan.contains("docupodcast-simple-video-render-v1"));
        assertTrue(commandPlan.contains("renderableAsMp4"));
        assertTrue(buildUseCaseNormalized.contains("cancelación segura"));
        assertTrue(exportUseCase.contains("RENDER_MANIFEST.json"));
        assertTrue(exportUseCase.contains("render-commands.txt"));
        assertTrue(exportUseCase.contains("RENDER_STATE.md"));
        assertTrue(docsNormalized.contains("bloqueo operativo"));
        assertTrue(docsNormalized.contains("cancelación segura"));
        assertTrue(docs.contains("MP4 final"));
    }
}
