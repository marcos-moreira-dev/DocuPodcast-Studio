package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildSimpleVideoPlanFromRenderUnitPlanTest {
    @Test
    void buildsOnlyVisualFramesFromRenderUnitsAndAllowsSilentVisualFrames() {
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(
                new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Hangar", "assets/images/hangar.png", "image/png", "storyboard", "", ""),
                new ProjectAssetReference("IMG-002", ProjectAssetKind.IMAGE, "Mapa", "assets/images/mapa.png", "image/png", "storyboard", "", "")
        ));
        RenderUnit spokenWithoutVisual = new RenderUnit(
                "SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 20), new DocumentTextRange("BLK-001", 0, 20),
                "Intro", "Esto solo se escucha.", RenderUnitKind.SPOKEN_ONLY,
                "VOC-NARRATOR", "", "", "", 5.0, List.of());
        RenderUnit spokenWithVisual = new RenderUnit(
                "SEG-002-U001", "SEG-002-U001", "SEG-002", 1,
                new ScriptTextRange("SEG-002", 0, 24), new DocumentTextRange("BLK-002", 0, 24),
                "Despegue", "El avión tiembla de emoción.", RenderUnitKind.SPOKEN_WITH_VISUAL,
                "VOC-NARRATOR", "", "", "IMG-001", 5.0, List.of("LAYER-IMG-001"));
        RenderUnit visualSilent = RenderUnit.visualSilent(
                "VISUAL-BLK-003", 2, new DocumentTextRange("BLK-003", 0, 1),
                "Mapa al revés", "IMG-002", 5.0, List.of("LAYER-IMG-002"));
        RenderUnitPlan renderPlan = new RenderUnitPlan("UNITPLAN-DEMO", "SCRIPT-DEMO",
                List.of(spokenWithoutVisual, spokenWithVisual, visualSilent), 5.0, Instant.now());
        AudioJobSnapshot job = jobWithCompletedUnit("SEG-002-U001", "jobs/JOB-001/audio/SEG-002-U001.wav", 2.5);

        SimpleVideoPlan videoPlan = new BuildSimpleVideoPlanUseCase().build(renderPlan, assets, List.of(job));

        assertEquals(2, videoPlan.frameCount());
        assertEquals(2, videoPlan.framesWithImage());
        assertEquals(1, videoPlan.framesWithAudio());
        assertEquals(1, videoPlan.framesSilentVisual());
        assertEquals(0, videoPlan.framesMissingAudio());
        assertTrue(videoPlan.exportableAsRenderedVideo());
        assertEquals("assets/images/hangar.png", videoPlan.frames().get(0).imageRelativePath());
        assertEquals("visual-silencioso", videoPlan.frames().get(1).frameModeLabel());
        assertEquals(5.0, videoPlan.frames().get(1).frameDurationSeconds(), 0.001);
    }

    @Test
    void commandPlanGeneratesSyntheticSilenceForSilentVisualFrame() {
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(
                new ProjectAssetReference("IMG-002", ProjectAssetKind.IMAGE, "Mapa", "assets/images/mapa.png", "image/png", "storyboard", "", "")
        ));
        RenderUnit visualSilent = RenderUnit.visualSilent(
                "VISUAL-BLK-003", 0, new DocumentTextRange("BLK-003", 0, 1),
                "Mapa al revés", "IMG-002", 5.0, List.of("LAYER-IMG-002"));
        RenderUnitPlan renderPlan = new RenderUnitPlan("UNITPLAN-DEMO", "SCRIPT-DEMO", List.of(visualSilent), 5.0, Instant.now());
        SimpleVideoPlan videoPlan = new BuildSimpleVideoPlanUseCase().build(renderPlan, assets, List.of());

        VideoRenderCommandPlan commandPlan = new BuildVideoRenderCommandPlanUseCase()
                .build(videoPlan, SimpleVideoExportSettings.defaults(), new FfmpegToolDiscovery(java.nio.file.Path.of("tools/ffmpeg/bin/ffmpeg.exe"), java.nio.file.Path.of("tools/ffmpeg/bin/ffprobe.exe"), true, true, "OK"));

        assertTrue(commandPlan.commandsText().contains("anullsrc=channel_layout=stereo:sample_rate=44100"));
        assertTrue(commandPlan.commandsText().contains("assets/images/mapa.png"));
    }

    private static AudioJobSnapshot jobWithCompletedUnit(String unitId, String relativePath, double duration) {
        return new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot(unitId, unitId, AudioSegmentStatus.COMPLETED, relativePath, duration, 1, "")
        ), Instant.now(), Instant.now());
    }
}
