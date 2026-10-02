package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

final class BuildTheatreAudioTrackTimelineUseCaseTest {
    private final BuildTheatreAudioTrackTimelineUseCase useCase = new BuildTheatreAudioTrackTimelineUseCase();

    @Test
    void trackStartedAtOneInterventionCoversFollowingInterventions() {
        DocuPodcastProject project = project(List.of(track("TRACK-1", "INTERVENCION-1", 0, 5)));

        TheatreAudioTrackTimeline timeline = useCase.execute(project, script(), manifest());

        TheatreAudioTrackTimelineEntry entry = timeline.entries().getFirst();
        assertTrue(entry.valid());
        assertEquals(0.0, entry.timelineStartSeconds(), 0.001);
        assertEquals(5.0, entry.timelineEndSeconds(), 0.001);
        assertEquals(List.of("INTERVENCION-1", "INTERVENCION-2", "INTERVENCION-3"), entry.affectedInterventionIds());
        assertEquals("TRACK-1", timeline.affectingIntervention("INTERVENCION-2").orElseThrow().track().id());
    }

    @Test
    void upsertRequiresExplicitReplacementWhenIntervalsOverlap() {
        DocuPodcastProject project = project(List.of(track("TRACK-1", "INTERVENCION-1", 0, 5)));
        UpsertTheatreAudioTrackUseCase upsert = new UpsertTheatreAudioTrackUseCase(useCase);
        TheatreProjectLayer.TheatreAudioTrack candidate = track("TRACK-2", "INTERVENCION-2", 0, 3);

        var rejected = upsert.execute(project, script(), manifest(), candidate, false);
        assertFalse(rejected.saved());
        assertEquals(List.of("TRACK-1"), rejected.conflicts().stream().map(item -> item.track().id()).toList());

        var replaced = upsert.execute(project, script(), manifest(), candidate, true);
        assertTrue(replaced.saved());
        assertEquals(List.of("TRACK-2"), replaced.project().theatre().audioTracks().stream()
                .map(TheatreProjectLayer.TheatreAudioTrack::id).toList());
    }

    @Test
    void anyNarratableSegmentCanAnchorTrackWithoutTheatreIntervention() {
        TheatreProjectLayer.TheatreAudioTrack track = new TheatreProjectLayer.TheatreAudioTrack(
                "TRACK-INTRO", "AUDIO-1", "", "SEG-INTRO", 0.0, 1.5,
                TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.30, 8.0, true);
        DocuPodcastProject project = project(List.of(track));
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-INTRO", NarrationSegmentType.PARAGRAPH, "Intro", "Personajes de la obra", List.of("B000")),
                NarrationSegment.of("SEG-1", NarrationSegmentType.PARAGRAPH, "Uno", "Primera intervencion", List.of("B001"))));
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-2", "JOB-2", List.of(
                cue("SEG-INTRO", 0, 2), cue("SEG-1", 2, 4)), "", Instant.now());

        TheatreAudioTrackTimeline timeline = useCase.execute(project, script, manifest);

        TheatreAudioTrackTimelineEntry entry = timeline.startingAtSegment("SEG-INTRO").orElseThrow();
        assertTrue(entry.valid());
        assertEquals("SEG-INTRO", entry.startSegmentId());
        assertEquals(0.375, entry.track().fadeDurationSeconds(), 0.001);
        assertEquals("TRACK-INTRO", timeline.affectingSegment("SEG-INTRO").orElseThrow().track().id());
    }

    @Test
    void indexesLargePlaybackManifestInsteadOfScanningItForEverySegment() {
        ArrayList<NarrationSegment> segments = new ArrayList<>();
        ArrayList<PlaybackCue> cues = new ArrayList<>();
        for (int index = 0; index < 2_000; index++) {
            String id = "SEG-" + index;
            segments.add(NarrationSegment.of(id, NarrationSegmentType.PARAGRAPH,
                    id, "Texto narrable " + index, List.of("B-" + index)));
            cues.add(cue(id, index, index + 1));
        }
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Documento grande", "es", "source.docx", segments);
        PlaybackManifest manifest = new PlaybackManifest(
                "PLAYBACK-LARGE", "JOB-LARGE", cues, "", Instant.now());

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                useCase.execute(project(List.of()), script, manifest));
    }

    private static DocuPodcastProject project(List<TheatreProjectLayer.TheatreAudioTrack> tracks) {
        TheatreProjectLayer theatre = TheatreProjectLayer.empty().withAudioTracks(tracks);
        theatre = new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B002"),
                        TheatreProjectLayer.Intervencion.ofSequence(3, "B003")),
                theatre.characters(), theatre.voiceRoleAliases(), theatre.characterImages(), theatre.intervencionesVisuales(),
                theatre.acts(), theatre.scenes(), theatre.positions(), theatre.actions(), theatre.textActionPlacements(),
                theatre.objectImages(), theatre.objects(), tracks);
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION)
                .withAsset(new ProjectAssetReference("AUDIO-1", ProjectAssetKind.AUDIO_CLIP, "Ambiente",
                        "assets/audio/ambiente.wav", "audio/wav", "pista", "", ""))
                .withTheatre(theatre);
    }

    private static TheatreProjectLayer.TheatreAudioTrack track(String id, String intervention, double from, double to) {
        return new TheatreProjectLayer.TheatreAudioTrack(id, "AUDIO-1", intervention, from, to,
                TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.30, 8.0);
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-1", NarrationSegmentType.PARAGRAPH, "Uno", "Primera intervencion", List.of("B001")),
                NarrationSegment.of("SEG-2", NarrationSegmentType.PARAGRAPH, "Dos", "Segunda intervencion", List.of("B002")),
                NarrationSegment.of("SEG-3", NarrationSegmentType.PARAGRAPH, "Tres", "Tercera intervencion", List.of("B003"))));
    }

    private static PlaybackManifest manifest() {
        return new PlaybackManifest("PLAYBACK-1", "JOB-1", List.of(
                cue("SEG-1", 0, 2), cue("SEG-2", 2, 4), cue("SEG-3", 4, 6)), "", Instant.now());
    }

    private static PlaybackCue cue(String id, double start, double end) {
        return new PlaybackCue(id, id, start, end, "AUDIO-" + id, "audio/" + id + ".wav", "", id, id);
    }
}
