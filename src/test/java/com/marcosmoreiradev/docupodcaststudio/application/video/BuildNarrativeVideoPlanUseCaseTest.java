package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeGeneratedClip;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeKeyframeSource;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrativeVideoPlanUseCaseTest {
    @TempDir
    Path projectDirectory;

    private final BuildNarrativeVideoPlanUseCase useCase = new BuildNarrativeVideoPlanUseCase();

    @Test
    void partitionsChainedVideoClipsAcrossTheParagraphVoice() throws Exception {
        prepareFile("generated/narrative/keyframes/B001.png");
        prepareFile("generated/narrative/clips/B001-01.mp4");
        prepareFile("generated/narrative/clips/B001-02.mp4");
        prepareFile("jobs/JOB/audio/SEG-001.wav");

        NarrativeParagraphTake take = take("B001", true, false, "KF-B001",
                List.of(
                        clip("C1", "VC1", 0, 2.0),
                        clip("C2", "VC2", 1, 2.0)));
        DocuPodcastProject project = project(take)
                .withAsset(asset("KF-B001", ProjectAssetKind.IMAGE,
                        "generated/narrative/keyframes/B001.png"))
                .withAsset(asset("VC1", ProjectAssetKind.VIDEO_SOURCE,
                        "generated/narrative/clips/B001-01.mp4"))
                .withAsset(asset("VC2", ProjectAssetKind.VIDEO_SOURCE,
                        "generated/narrative/clips/B001-02.mp4"));

        SimpleVideoPlan plan = useCase.build(
                project,
                script(segment("SEG-001", "B001")),
                List.of(job(audio("SEG-001", 3.0))),
                projectDirectory);

        assertEquals(1, plan.frameCount());
        List<SimpleVideoFrame.VisualPart> parts = plan.frames().getFirst().visualParts();
        assertEquals(2, parts.size());
        assertTrue(parts.stream().allMatch(SimpleVideoFrame.VisualPart::videoClip));
        assertEquals(2.0, parts.get(0).durationSeconds(), 0.001);
        assertEquals(0.0, parts.get(0).sourceStartSeconds(), 0.001);
        assertEquals(1.0, parts.get(1).durationSeconds(), 0.001);
        assertEquals(0.0, parts.get(1).sourceStartSeconds(), 0.001);
    }

    @Test
    void excludesDisabledParagraphFromNarrationAndVideo() throws Exception {
        prepareFile("generated/narrative/keyframes/B002.png");
        prepareFile("generated/narrative/clips/B002.mp4");
        prepareFile("jobs/JOB/audio/SEG-001.wav");
        prepareFile("jobs/JOB/audio/SEG-002.wav");

        NarrativeParagraphTake disabled = NarrativeParagraphTake.empty("B001").withEnabled(false);
        NarrativeParagraphTake enabled = take("B002", true, false, "KF-B002",
                List.of(clip("C2", "VC2", 0, 2.0)));
        DocuPodcastProject project = project(disabled, enabled)
                .withAsset(asset("KF-B002", ProjectAssetKind.IMAGE,
                        "generated/narrative/keyframes/B002.png"))
                .withAsset(asset("VC2", ProjectAssetKind.VIDEO_SOURCE,
                        "generated/narrative/clips/B002.mp4"));

        SimpleVideoPlan plan = useCase.build(
                project,
                script(segment("SEG-001", "B001"), segment("SEG-002", "B002")),
                List.of(job(audio("SEG-001", 1.0), audio("SEG-002", 1.5))),
                projectDirectory);

        assertEquals(1, plan.frameCount());
        assertEquals("SEG-002", plan.frames().getFirst().segmentId());
    }

    @Test
    void blocksStaleEnabledTakeWithoutDeletingItsPreviousFiles() throws Exception {
        prepareFile("generated/narrative/keyframes/B001.png");
        prepareFile("generated/narrative/clips/B001.mp4");
        prepareFile("jobs/JOB/audio/SEG-001.wav");
        NarrativeParagraphTake stale = take("B001", true, true, "KF-B001",
                List.of(clip("C1", "VC1", 0, 2.0)));
        DocuPodcastProject project = project(stale)
                .withAsset(asset("KF-B001", ProjectAssetKind.IMAGE,
                        "generated/narrative/keyframes/B001.png"))
                .withAsset(asset("VC1", ProjectAssetKind.VIDEO_SOURCE,
                        "generated/narrative/clips/B001.mp4"));

        IOException failure = assertThrows(IOException.class, () -> useCase.build(
                project,
                script(segment("SEG-001", "B001")),
                List.of(job(audio("SEG-001", 1.0))),
                projectDirectory));

        assertTrue(failure.getMessage().contains("desactualizada"));
        assertTrue(Files.isRegularFile(projectDirectory.resolve("generated/narrative/clips/B001.mp4")));
    }

    private void prepareFile(String relativePath) throws Exception {
        Path file = projectDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] { 1 });
    }

    private static NarrativeGeneratedClip clip(String id, String assetId, int order, double duration) {
        return new NarrativeGeneratedClip(id, assetId, order, duration, "", "model", "workflow",
                42L + order, "fingerprint", Map.of());
    }

    private static NarrativeParagraphTake take(String blockId,
                                               boolean enabled,
                                               boolean stale,
                                               String keyframeAssetId,
                                               List<NarrativeGeneratedClip> clips) {
        return new NarrativeParagraphTake(blockId, enabled, keyframeAssetId,
                NarrativeKeyframeSource.GENERATED, clips, "prompt", "", 42L,
                "fingerprint", stale, "");
    }

    private static DocuPodcastProject project(NarrativeParagraphTake... takes) {
        NarrativeProjectLayer layer = NarrativeProjectLayer.empty();
        for (NarrativeParagraphTake take : takes) {
            layer = layer.withTake(take);
        }
        return DocuPodcastProject.createNew("Narrativo", ProjectMode.NARRATIVE_VIDEO)
                .withNarrative(layer);
    }

    private static ProjectAssetReference asset(String id, ProjectAssetKind kind, String relativePath) {
        return new ProjectAssetReference(id, kind, id, relativePath, "", "", "", "");
    }

    private static NarrationSegment segment(String id, String blockId) {
        return NarrationSegment.of(id, NarrationSegmentType.PARAGRAPH, blockId,
                "Texto narrable de " + blockId, List.of(blockId));
    }

    private static NarrationScriptDocument script(NarrationSegment... segments) {
        return new NarrationScriptDocument("SCRIPT", "Documento", "es", "source.docx",
                List.of(segments), Instant.EPOCH, Instant.EPOCH, "");
    }

    private static AudioSegmentSnapshot audio(String segmentId, double durationSeconds) {
        return AudioSegmentSnapshot.pending(segmentId, segmentId)
                .completed("jobs/JOB/audio/" + segmentId + ".wav", durationSeconds);
    }

    private static AudioJobSnapshot job(AudioSegmentSnapshot... segments) {
        ArrayList<AudioSegmentSnapshot> items = new ArrayList<>(List.of(segments));
        return new AudioJobSnapshot(
                "JOB", "Documento", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                items.size(), items.size(), 0, 1.0, "", "", 0, "OK",
                "jobs/JOB", "", "jobs/JOB/playback-manifest.json",
                items, Instant.EPOCH, Instant.EPOCH);
    }
}
