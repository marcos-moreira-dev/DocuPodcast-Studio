package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrationRenderUnitsT91SourceTest {
    @Test
    void t91IntroducesExplicitRenderPlanWithoutReplacingSegmentJobsYet() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String unit = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/render/NarrationRenderUnit.java"));
        String useCase = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/render/BuildNarrationRenderPlanUseCase.java"));
        String playbackCue = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/playback/PlaybackCue.java"));
        String doc = Files.readString(root.resolve("docs/productizacion/T91_RENDER_NARRATIVO_UNIDADES.md"));

        assertTrue(unit.contains("NarrationRenderSourceKind"));
        assertTrue(unit.contains("audioAssetId"));
        assertTrue(unit.contains("imageAssetId"));
        assertTrue(useCase.contains("DocumentSentenceSplitter.split"));
        assertTrue(useCase.contains("NarrativeLayerKind.HUMAN_AUDIO"));
        assertTrue(playbackCue.contains("unitId"));
        assertTrue(doc.contains("T91 no reemplaza todavía los jobs TTS por segmento"));
    }
}
