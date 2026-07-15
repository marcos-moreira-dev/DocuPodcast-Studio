package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackSyncStateTest {
    @Test
    void projectsManifestCursorAndStoryboardIntoSharedState() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "demo.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola", List.of("B1")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "Adiós", List.of("B2"))
        ));
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-1", "JOB-1", List.of(
                new PlaybackCue("SEG-001", 0, 2, "AUD-1", "jobs/JOB-1/audio/SEG-001.wav", "IMG-001", "Intro"),
                new PlaybackCue("SEG-002", 2, 4, "AUD-2", "jobs/JOB-1/audio/SEG-002.wav", "", "Cierre")
        ), "", Instant.EPOCH);
        StoryboardDocument storyboard = new StoryboardDocument("STB-1", "Storyboard", script.id(), List.of(
                StoryboardBinding.of("BIND-2", "SEG-002", "IMG-002", "Cierre visual")
        ), Map.of(), Instant.EPOCH, Instant.EPOCH, "");

        PlaybackSyncState state = PlaybackSyncState.from(script, storyboard, manifest, new PlaybackCursor("SEG-002", 2.5, false));

        assertTrue(state.manifestAvailable());
        assertTrue(state.playing());
        assertEquals("SEG-002", state.activeSegmentId());
        assertEquals(2, state.audioReadySegments());
        assertEquals(2, state.storyboardReadySegments());
        assertTrue(state.summaryLabel().contains("SEG-002"));
        assertTrue(state.cueLabels().get(1).contains("IMG-002"));
    }

    @Test
    void explainsMissingManifestWithoutFailing() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "demo.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola", List.of("B1"))
        ));

        PlaybackSyncState state = PlaybackSyncState.from(script, null, PlaybackManifest.empty(), PlaybackCursor.stopped());

        assertTrue(state.scriptAvailable());
        assertEquals(0, state.cueCount());
        assertTrue(state.summaryLabel().contains("sin manifest"));
        assertTrue(state.cueLabels().getFirst().contains("No hay manifest"));
    }
}
