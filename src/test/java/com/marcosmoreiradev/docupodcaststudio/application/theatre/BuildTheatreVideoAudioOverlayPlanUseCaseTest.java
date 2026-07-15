package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreVideoAudioOverlayPlanUseCaseTest {
    @TempDir Path projectDirectory;

    @Test
    void resolvesParentTrackAnchorAgainstRenderedAudioUnitIds() throws Exception {
        VideoAudioOverlayPlan plan = new BuildTheatreVideoAudioOverlayPlanUseCase()
                .build(projectWithTrack("SEG-001"), script("SEG-001"), video("SEG-001-U001"), projectDirectory);

        assertEquals(1, plan.inputs().size());
        assertEquals("THEATRE-TRACK-001", plan.inputs().getFirst().overlayId());
        assertEquals(0.0, plan.inputs().getFirst().timelineStartSeconds(), 0.001);
    }

    @Test
    void omitsTrackThatDoesNotBelongToPartialExportScope() throws Exception {
        VideoAudioOverlayPlan plan = new BuildTheatreVideoAudioOverlayPlanUseCase()
                .build(projectWithTrack("SEG-OUTSIDE"), script("SEG-001"), video("SEG-001-U001"), projectDirectory);

        assertTrue(plan.inputs().isEmpty());
    }

    private DocuPodcastProject projectWithTrack(String startSegmentId) throws Exception {
        Path audio = projectDirectory.resolve("media/audio/track.wav");
        Files.createDirectories(audio.getParent());
        Files.write(audio, new byte[] {1});
        TheatreProjectLayer.TheatreAudioTrack track = new TheatreProjectLayer.TheatreAudioTrack(
                "THEATRE-TRACK-001", "AUDIO-001", "", startSegmentId,
                0.0, 4.0, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.30, 10.0, false);
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION)
                .withAsset(new ProjectAssetReference("AUDIO-001", ProjectAssetKind.AUDIO_CLIP, "Pista",
                        "media/audio/track.wav", "audio/wav", "pista teatral", "", ""))
                .withTheatre(TheatreProjectLayer.empty().withAudioTracks(List.of(track)));
    }

    private static NarrationScriptDocument script(String segmentId) {
        return NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                NarrationSegment.of(segmentId, NarrationSegmentType.PARAGRAPH,
                        "Escena", "Texto narrable", List.of("B0001"))));
    }

    private static SimpleVideoPlan video(String unitId) {
        SimpleVideoFrame frame = new SimpleVideoFrame("FRAME-001", unitId, "Escena", "Texto",
                "IMG-001", "media/images/frame.png", "jobs/audio/" + unitId + ".wav",
                3.0, 0.0, true, true);
        return new SimpleVideoPlan("Obra", List.of(frame), 0.0, Instant.now());
    }
}
