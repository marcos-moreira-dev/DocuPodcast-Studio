package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackVisualCueResolverTest {
    @Test
    void keepsTheExactPlayingSentenceWhenSeveralUnitsShareOneParagraph() {
        PlaybackCue firstSentence = cue("SEG-001-U001", 0.0, 1.0, "Primera oración.");
        PlaybackCue secondSentence = cue("SEG-001-U002", 1.0, 2.0, "Segunda oración.");
        PlaybackManifest manifest = manifest(firstSentence, secondSentence);

        // A freshly opened WAV reports a local position near zero. Resolving only
        // that position would incorrectly return the paragraph's first sentence.
        PlaybackCursor cursor = new PlaybackCursor("SEG-001", 0.0, false);

        PlaybackCue resolved = PlaybackVisualCueResolver.resolve(
                cursor, secondSentence, manifest).orElseThrow();

        assertEquals("SEG-001-U002", resolved.unitId());
    }

    @Test
    void fallsBackToManifestPositionWhenNoAcousticCueHasBeenPublished() {
        PlaybackCue firstSentence = cue("SEG-001-U001", 0.0, 1.0, "Primera oración.");
        PlaybackCue secondSentence = cue("SEG-001-U002", 1.0, 2.0, "Segunda oración.");

        PlaybackCue resolved = PlaybackVisualCueResolver.resolve(
                new PlaybackCursor("SEG-001", 1.25, false),
                null,
                manifest(firstSentence, secondSentence)).orElseThrow();

        assertEquals("SEG-001-U002", resolved.unitId());
    }

    @Test
    void doesNotReuseAnAcousticCueFromAnotherSegment() {
        PlaybackCue oldCue = cue("SEG-001-U001", 0.0, 1.0, "Anterior.");
        PlaybackCue newCue = new PlaybackCue(
                "SEG-002", "SEG-002-U001", 1.0, 2.0,
                "CLIP-2", "audio/2.wav", "", "Nueva", "Nueva.");
        PlaybackManifest manifest = new PlaybackManifest(
                "MANIFEST-1", "JOB-1", List.of(oldCue, newCue), "", Instant.EPOCH);

        PlaybackCue resolved = PlaybackVisualCueResolver.resolve(
                new PlaybackCursor("SEG-002", 1.0, false), oldCue, manifest).orElseThrow();

        assertEquals("SEG-002-U001", resolved.unitId());
    }

    @Test
    void clearsVisualCueWhenPlaybackStops() {
        PlaybackCue cue = cue("SEG-001-U001", 0.0, 1.0, "Primera oración.");

        assertTrue(PlaybackVisualCueResolver.resolve(
                PlaybackCursor.stopped(), cue, manifest(cue)).isEmpty());
    }

    @Test
    void autoScrollRequiresLivePlaybackAndTheLatestVisualSequence() {
        PlaybackCursor playing = new PlaybackCursor("SEG-001", 0.5, false);
        PlaybackCursor paused = playing.pause();

        assertTrue(DocumentWorkspaceView.playbackAutoScrollAllowed(playing, 7, 7));
        assertFalse(DocumentWorkspaceView.playbackAutoScrollAllowed(paused, 7, 7));
        assertFalse(DocumentWorkspaceView.playbackAutoScrollAllowed(playing, 6, 7));
    }

    @Test
    void theatreHighlightsTheWholeInterventionWhileDocumentStudyKeepsSentencePrecision() {
        assertTrue(DocumentWorkspaceView.wholeBlockPlaybackHighlight(
                ProjectMode.THEATRE_PRODUCTION));
        assertFalse(DocumentWorkspaceView.wholeBlockPlaybackHighlight(
                ProjectMode.DOCUMENTARY_STUDIO));
        assertFalse(DocumentWorkspaceView.wholeBlockPlaybackHighlight(
                ProjectMode.NARRATIVE_VIDEO));
    }

    @Test
    void theatreSelectsTheWholeInterventionWhileDocumentStudyKeepsSentencePrecision() {
        assertTrue(DocumentWorkspaceView.wholeBlockTextSelection(
                ProjectMode.THEATRE_PRODUCTION));
        assertFalse(DocumentWorkspaceView.wholeBlockTextSelection(
                ProjectMode.DOCUMENTARY_STUDIO));
        assertFalse(DocumentWorkspaceView.wholeBlockTextSelection(
                ProjectMode.NARRATIVE_VIDEO));
    }

    private static PlaybackCue cue(String unitId, double start, double end, String spokenText) {
        return new PlaybackCue(
                "SEG-001", unitId, start, end,
                "CLIP-" + unitId, "audio/" + unitId + ".wav", "",
                spokenText, spokenText);
    }

    private static PlaybackManifest manifest(PlaybackCue... cues) {
        return new PlaybackManifest(
                "MANIFEST-1", "JOB-1", List.of(cues), "", Instant.EPOCH);
    }
}
