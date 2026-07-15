package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPlaybackManifestFromRenderUnitPlanTest {
    @Test
    void buildsUnitLevelCuesFromGeneratedAndExternalAudioButSkipsSilentVisuals() {
        RenderUnit generated = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 15), new DocumentTextRange("B0001", 0, 15),
                "Primera oración", "Texto generado.", RenderUnitKind.SPOKEN_ONLY,
                "VOC-NARRATOR", "STY-NEUTRAL", "", "", 5.0, List.of());
        RenderUnit external = new RenderUnit("SEG-001-U002", "SEG-001-U002", "SEG-001", 1,
                new ScriptTextRange("SEG-001", 16, 35), new DocumentTextRange("B0001", 16, 35),
                "Segunda oración", "Audio humano.", RenderUnitKind.SPOKEN_WITH_VISUAL,
                "", "", "AUD-USER-001", "IMG-001", 5.0, List.of("LAYER-AUDIO", "LAYER-IMAGE"));
        RenderUnit silent = RenderUnit.visualSilent("VISUAL-B0002", 2,
                new DocumentTextRange("B0002", 0, 0), "Tabla", "IMG-TABLE", 5.0, List.of("LAYER-TABLE"));
        RenderUnitPlan plan = new RenderUnitPlan("UNITPLAN-SCRIPT-001", "SCRIPT-001",
                List.of(generated, external, silent), 5.0, Instant.EPOCH);
        AudioJobSnapshot snapshot = snapshot();
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("AUD-USER-001", ProjectAssetKind.AUDIO_CLIP,
                        "Voz humana", "media/audio/voz-humana.wav", "audio/wav", "Audio elegido por el usuario", "", ""));

        PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(plan, snapshot, project);

        assertEquals(2, manifest.cueCount());
        assertEquals("jobs/JOB-001/audio/SEG-001-U001.wav", manifest.cueForUnit("SEG-001-U001").orElseThrow().audioRelativePath());
        assertEquals("media/audio/voz-humana.wav", manifest.cueForUnit("SEG-001-U002").orElseThrow().audioRelativePath());
        assertEquals("IMG-001", manifest.cueForUnit("SEG-001-U002").orElseThrow().imageAssetId());
        assertTrue(manifest.cueForUnit("VISUAL-B0002").isEmpty());
        assertEquals("SEG-001-U002", manifest.nextCueAfterUnit("SEG-001-U001").orElseThrow().unitId());
    }

    @Test
    void seekCanTargetAConcreteUnitWhileKeepingSegmentCompatibleCursor() {
        PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(planForSeek(), snapshot(), DocuPodcastProject.empty("Demo"));

        PlaybackCursor cursor = new SeekPlaybackUseCase().seekUnit(PlaybackCursor.stopped(), manifest, "SEG-001-U002");

        assertEquals("SEG-001", cursor.segmentId());
        assertEquals(2.0, cursor.positionSeconds(), 0.001);
        assertTrue(cursor.paused());
    }

    private static RenderUnitPlan planForSeek() {
        return new RenderUnitPlan("UNITPLAN-SEEK", "SCRIPT-001", List.of(
                new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                        new ScriptTextRange("SEG-001", 0, 10), null,
                        "Uno", "Texto uno.", RenderUnitKind.SPOKEN_ONLY,
                        "", "", "", "", 5.0, List.of()),
                new RenderUnit("SEG-001-U002", "SEG-001-U002", "SEG-001", 1,
                        new ScriptTextRange("SEG-001", 11, 20), null,
                        "Dos", "Texto dos.", RenderUnitKind.SPOKEN_ONLY,
                        "", "", "", "", 5.0, List.of())
        ), 5.0, Instant.EPOCH);
    }

    private static AudioJobSnapshot snapshot() {
        return new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                2, 2, 0, 1.0, "", "", 0, "OK", "jobs/JOB-001",
                "jobs/JOB-001/final/podcast.wav", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-001-U001", "Primera oración", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-001-U001.wav", 2.0, 1, ""),
                new AudioSegmentSnapshot("SEG-001-U002", "Segunda oración", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-001-U002.wav", 2.0, 1, "")
        ), Instant.EPOCH, Instant.EPOCH);
    }
}
