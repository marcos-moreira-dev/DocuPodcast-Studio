package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioJobsFromRenderPlanTi2SourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void audioGenerationRequestCarriesRenderUnitPlanAndEffectiveGenerationUnits() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioGenerationRequest.java"));
        assertTrue(source.contains("RenderUnitPlan renderUnitPlan"));
        assertTrue(source.contains("usesRenderUnitPlan"));
        assertTrue(source.contains("generationUnits()"));
        assertTrue(source.contains("requiresAudioGeneration"));
        assertTrue(source.contains("No hay unidades de voz sintetizable"));
    }

    @Test
    void gatewaysConsumeGenerationUnitsInsteadOfRawSegments() throws Exception {
        String mock = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/MockAudioGenerationGateway.java"));
        String local = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        assertTrue(mock.contains("request.generationUnits()"));
        assertTrue(local.contains("request.generationUnits()"));
        assertTrue(local.contains("segment.effectiveVoiceProfileId(request.voiceProfileId())"));
    }

    @Test
    void roadmapDocumentsTi2BeforeVideoMigration() throws Exception {
        String roadmap = Files.readString(ROOT.resolve("docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md"));
        String ti2 = Files.readString(ROOT.resolve("docs/productizacion/roadmap_restante_post_ti1/TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md"));
        assertTrue(roadmap.contains("TI2"));
        assertTrue(ti2.contains("Audio jobs desde RenderPlan"));
    }
}
