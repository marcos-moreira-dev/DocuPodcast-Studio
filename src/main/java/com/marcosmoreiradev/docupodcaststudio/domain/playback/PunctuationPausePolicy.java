package com.marcosmoreiradev.docupodcaststudio.domain.playback;

/** Decides explicit silence between playback cues from text punctuation and playback speed. */
public final class PunctuationPausePolicy {
    public static final long CONTINUING_PUNCTUATION_PAUSE_MILLIS = 250L;
    public static final long PARAGRAPH_FINAL_PAUSE_MILLIS = 500L;
    private static final double ACCELERATED_RATE_THRESHOLD = 1.49;

    public long interCuePauseMillis(PlaybackCue completedCue,
                                    PlaybackCue nextCue,
                                    double playbackRate,
                                    long oneXFallbackMillis) {
        if (nextCue == null) {
            return 0L;
        }
        if (playbackRate < ACCELERATED_RATE_THRESHOLD) {
            return Math.max(0L, oneXFallbackMillis);
        }
        String text = completedCue == null ? "" : completedCue.spokenText().strip();
        if (text.isBlank()) {
            return 0L;
        }
        char last = text.charAt(text.length() - 1);
        if (last == ',') {
            return CONTINUING_PUNCTUATION_PAUSE_MILLIS;
        }
        if (last == '.') {
            return sameSegment(completedCue, nextCue) || structuralText(completedCue)
                    ? CONTINUING_PUNCTUATION_PAUSE_MILLIS
                    : PARAGRAPH_FINAL_PAUSE_MILLIS;
        }
        return 0L;
    }

    private static boolean sameSegment(PlaybackCue left, PlaybackCue right) {
        return left != null && right != null && left.segmentId().equals(right.segmentId());
    }

    private static boolean structuralText(PlaybackCue cue) {
        return cue != null && cue.structuralText();
    }
}
