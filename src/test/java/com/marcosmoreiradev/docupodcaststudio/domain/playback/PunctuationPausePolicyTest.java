package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PunctuationPausePolicyTest {
    private final PunctuationPausePolicy policy = new PunctuationPausePolicy();

    @Test
    void keepsExistingOneXPauseInsteadOfApplyingNewDurations() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "Texto final.");
        PlaybackCue next = cue("SEG-002", "SEG-002-U001", "Siguiente texto.");

        long pause = policy.interCuePauseMillis(completed, next, 1.0, 1000L);

        assertEquals(1000L, pause);
    }

    @Test
    void usesQuarterSecondForCommaWhenAccelerated() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "Capitan,");
        PlaybackCue next = cue("SEG-001", "SEG-001-U002", "revise el combustible.");

        long pause = policy.interCuePauseMillis(completed, next, 1.5, 1000L);

        assertEquals(250L, pause);
    }

    @Test
    void usesQuarterSecondForPointFollowedInsideSameSegmentWhenAccelerated() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "Combustible hay.");
        PlaybackCue next = cue("SEG-001", "SEG-001-U002", "Viento hay.");

        long pause = policy.interCuePauseMillis(completed, next, 1.75, 1000L);

        assertEquals(250L, pause);
    }

    @Test
    void usesHalfSecondForParagraphFinalPointWhenAnotherTextFollows() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "La dignidad esta en mantenimiento.");
        PlaybackCue next = cue("SEG-002", "SEG-002-U001", "Excelente.");

        long pause = policy.interCuePauseMillis(completed, next, 1.75, 1000L);

        assertEquals(500L, pause);
    }

    @Test
    void usesQuarterSecondForStructuralTitlePointAcrossSegmentsWhenAccelerated() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "Capitulo uno.", PlaybackCueKind.TITLE);
        PlaybackCue next = cue("SEG-002", "SEG-002-U001", "Excelente.");

        long pause = policy.interCuePauseMillis(completed, next, 1.5, 1000L);

        assertEquals(250L, pause);
    }

    @Test
    void doesNotAddPauseAfterLastText() {
        PlaybackCue completed = cue("SEG-001", "SEG-001-U001", "Final.");

        long pause = policy.interCuePauseMillis(completed, null, 1.75, 1000L);

        assertEquals(0L, pause);
    }

    private static PlaybackCue cue(String segmentId, String unitId, String text) {
        return new PlaybackCue(segmentId, unitId, 0.0, 1.0, "AUD-" + unitId,
                "jobs/JOB/audio/" + unitId + ".wav", "", unitId, text);
    }

    private static PlaybackCue cue(String segmentId, String unitId, String text, PlaybackCueKind kind) {
        return new PlaybackCue(segmentId, unitId, 0.0, 1.0, "AUD-" + unitId,
                "jobs/JOB/audio/" + unitId + ".wav", "", unitId, text, kind);
    }
}
