package com.marcosmoreiradev.docupodcaststudio.application.guide;

/** A ranked match returned by local guide search. */
public record GuideSearchResult(GuideTopic topic, int score) implements Comparable<GuideSearchResult> {
    public GuideSearchResult {
        if (topic == null) {
            throw new IllegalArgumentException("topic is required");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score cannot be negative");
        }
    }

    @Override
    public int compareTo(GuideSearchResult other) {
        return Integer.compare(other.score, score);
    }
}
