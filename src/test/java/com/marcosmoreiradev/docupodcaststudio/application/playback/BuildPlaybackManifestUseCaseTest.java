package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.*;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPlaybackManifestUseCaseTest {
    @Test
    void buildsCuesFromScriptAudioSnapshotAndStoryboard() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola mundo", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "Adiós mundo", List.of("B002"))
        ));
        AudioJobSnapshot snapshot = new AudioJobSnapshot("JOB-001", "Obra", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                2, 2, 0, 1.0, "", "", 0, "OK", "jobs/JOB-001", "jobs/JOB-001/final/podcast.wav", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 2.0, 1, ""),
                new AudioSegmentSnapshot("SEG-002", "Cierre", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-002.wav", 3.0, 1, "")
        ), Instant.EPOCH, Instant.EPOCH);
        StoryboardDocument storyboard = new StoryboardDocument("STORYBOARD-001", "Storyboard", script.id(), List.of(
                StoryboardBinding.of("STB-001", "SEG-002", "IMG-002", "Cierre visual")
        ), Map.of(), Instant.EPOCH, Instant.EPOCH, "");

        PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(script, snapshot, storyboard);

        assertEquals(2, manifest.cueCount());
        assertEquals("IMG-002", manifest.cueForSegment("SEG-002").orElseThrow().imageAssetId());
        assertEquals(2.0, manifest.cueForSegment("SEG-002").orElseThrow().startSeconds(), 0.001);
        assertEquals("jobs/JOB-001/audio/SEG-001.wav", manifest.cueForSegment("SEG-001").orElseThrow().audioRelativePath());
        assertTrue(manifest.totalDurationSeconds() >= 5.0);
    }

    @Test
    void skipsSegmentsWithoutCompletedAudio() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Pendiente", "Texto", List.of("B002"))
        ));
        AudioJobSnapshot snapshot = new AudioJobSnapshot("JOB-001", "Obra", AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                1, 2, 0, 0.5, "", "", 0, "Parcial", "jobs/JOB-001", "", "", List.of(
                new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 1.0, 1, ""),
                new AudioSegmentSnapshot("SEG-002", "Pendiente", AudioSegmentStatus.PENDING, "", 0.0, 0, "")
        ), Instant.EPOCH, Instant.EPOCH);

        PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(script, snapshot, null);

        assertEquals(1, manifest.cueCount());
        assertEquals("SEG-001", manifest.firstCue().orElseThrow().segmentId());
    }

    @Test
    void userAudioClipUnitsBecomeEffectivePlaybackCues() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Ambiente", "Primera oración. Segunda oración.", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "Texto final.", List.of("B002"))
        ));
        AudioJobSnapshot snapshot = new AudioJobSnapshot("JOB-001", "Obra", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                2, 2, 0, 1.0, "", "", 0, "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-001", "Ambiente", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 5.0, 1, ""),
                new AudioSegmentSnapshot("SEG-002", "Cierre", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-002.wav", 2.0, 1, "")
        ), Instant.EPOCH, Instant.EPOCH);
        NarrationRenderPlan plan = new NarrationRenderPlan("RENDER-SCRIPT-001", script.id(), List.of(
                new NarrationRenderUnit("SEG-001-U001", "SEG-001", 0,
                        new ScriptTextRange("SEG-001", 0, 16), null, "Primera oración.",
                        NarrationRenderSourceKind.TEXT_TO_SPEECH, "VOC-NARRATOR", "STY-NEUTRAL", "", "", List.of()),
                new NarrationRenderUnit("SEG-001-U002", "SEG-001", 1,
                        new ScriptTextRange("SEG-001", 17, 33), null, "Segunda oración.",
                        NarrationRenderSourceKind.AUDIO_CLIP, "", "STY-NEUTRAL", "AUDIO-USER-001", "IMG-001", List.of("LAYER-AUDIO")),
                new NarrationRenderUnit("SEG-002-U001", "SEG-002", 0,
                        new ScriptTextRange("SEG-002", 0, 12), null, "Texto final.",
                        NarrationRenderSourceKind.TEXT_TO_SPEECH, "VOC-NARRATOR", "STY-NEUTRAL", "", "", List.of())
        ), Instant.EPOCH);
        DocuPodcastProject project = DocuPodcastProject.empty("Obra")
                .withAsset(new ProjectAssetReference("AUDIO-USER-001", ProjectAssetKind.AUDIO_CLIP,
                        "Pájaros", "media/audio/pajaros.wav", "audio/wav", "Clip genérico", "", ""));

        PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(script, snapshot, null, plan, project);

        assertEquals(2, manifest.cueCount());
        assertEquals("SEG-001-U002", manifest.firstCue().orElseThrow().unitId());
        assertEquals("AUDIO-USER-001", manifest.firstCue().orElseThrow().audioClipId());
        assertEquals("media/audio/pajaros.wav", manifest.firstCue().orElseThrow().audioRelativePath());
        assertEquals("IMG-001", manifest.firstCue().orElseThrow().imageAssetId());
        assertEquals("jobs/JOB-001/audio/SEG-002.wav", manifest.cueForSegment("SEG-002").orElseThrow().audioRelativePath());
    }

}
