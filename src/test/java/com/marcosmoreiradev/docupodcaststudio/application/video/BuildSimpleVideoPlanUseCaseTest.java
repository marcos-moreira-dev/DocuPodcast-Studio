package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildSimpleVideoPlanUseCaseTest {
    @Test
    void buildsFramesFromScriptStoryboardAndCompletedAudio() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script)
                .withBinding(StoryboardBinding.of("BND-001", "SEG-001", "IMG-001", "Bosque"));
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(
                new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Bosque", "assets/images/bosque.png", "image/png", "frame", "", "")
        ));
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 2.5, 1, ""),
                new AudioSegmentSnapshot("SEG-002", "Cierre", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-002.wav", 1.5, 1, "")
        ), Instant.now(), Instant.now());

        SimpleVideoPlan plan = new BuildSimpleVideoPlanUseCase().build(script, storyboard, assets, List.of(job));

        assertEquals(2, plan.frameCount());
        assertEquals(4.0 + 2.0, plan.totalDurationSeconds(), 0.001);
        assertEquals(1, plan.framesWithImage());
        assertEquals(2, plan.framesWithAudio());
        assertFalse(plan.exportableAsRenderedVideo());
        assertEquals(1, plan.framesMissingImage());
        assertEquals("assets/images/bosque.png", plan.frames().get(0).imageRelativePath());
    }

    @Test
    void keepsFullNarrationTextForVideoFramesInsteadOfPreviewEllipsis() {
        String longText = ("NARRADOR: "
                + "El avion conserva todo el texto del fragmento sin recortarlo en la exportacion. ".repeat(5)).strip();
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Cierre", longText, List.of("BLK-001"))
        ));

        SimpleVideoPlan plan = new BuildSimpleVideoPlanUseCase().build(script, StoryboardDocument.createForScript(script),
                ProjectAssetCatalog.empty(), List.of());

        assertEquals(longText, plan.frames().get(0).narrationPreview());
        assertFalse(plan.frames().get(0).narrationPreview().endsWith("..."));
        assertFalse(plan.frames().get(0).narrationPreview().endsWith("…"));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Demo", "es", "", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Los árboles respiran con el viento.", List.of("BLK-001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "El bosque queda en silencio.", List.of("BLK-002"))
        ));
    }
}
