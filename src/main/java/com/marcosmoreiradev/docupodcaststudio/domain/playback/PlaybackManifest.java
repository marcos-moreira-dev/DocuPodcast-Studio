package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Ordered manifest used to synchronize script text, generated audio and storyboard images. */
public record PlaybackManifest(
        String id,
        String sourceJobId,
        List<PlaybackCue> cues,
        String finalAudioRelativePath,
        Instant createdAt
) {
    public PlaybackManifest {
        id = token(id, "id");
        sourceJobId = token(sourceJobId, "sourceJobId");
        cues = cues == null ? List.of() : List.copyOf(cues);
        finalAudioRelativePath = portableOptionalPath(finalAudioRelativePath);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        validate(cues);
    }

    public static PlaybackManifest empty() {
        return new PlaybackManifest("PLAYBACK-EMPTY", "JOB-NONE", List.of(), "", Instant.EPOCH);
    }

    public boolean emptyManifest() {
        return cues.isEmpty();
    }

    public int cueCount() {
        return cues.size();
    }

    public double totalDurationSeconds() {
        return cues.stream().mapToDouble(PlaybackCue::durationSeconds).sum();
    }

    /** Returns the first cue for a segment, preserving segment-level compatibility. */
    public Optional<PlaybackCue> cueForSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return Optional.empty();
        }
        String target = segmentId.strip();
        return cues.stream().filter(cue -> cue.segmentId().equals(target)).findFirst();
    }

    public List<PlaybackCue> cuesForSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return List.of();
        }
        String target = segmentId.strip();
        return cues.stream().filter(cue -> cue.segmentId().equals(target)).toList();
    }

    public PlaybackManifest onlySegment(String segmentId) {
        List<PlaybackCue> segmentCues = cuesForSegment(segmentId);
        if (segmentCues.isEmpty()) {
            return empty();
        }
        return new PlaybackManifest(id + "-SEGMENT", sourceJobId, segmentCues, finalAudioRelativePath, createdAt);
    }

    public Optional<PlaybackCue> cueForUnit(String unitId) {
        if (unitId == null || unitId.isBlank()) {
            return Optional.empty();
        }
        String target = unitId.strip();
        return cues.stream().filter(cue -> cue.unitId().equals(target)).findFirst();
    }

    public Optional<PlaybackCue> cueAt(double absolutePositionSeconds) {
        double position = Math.max(0.0, absolutePositionSeconds);
        Optional<PlaybackCue> openInterval = cues.stream()
                .filter(cue -> position >= cue.startSeconds() && position < cue.endSeconds())
                .findFirst();
        if (openInterval.isPresent()) {
            return openInterval;
        }
        return cues.stream()
                .filter(cue -> Math.abs(position - cue.endSeconds()) < 0.000_001)
                .reduce((first, second) -> second);
    }

    public Optional<PlaybackCue> firstCue() {
        return cues.isEmpty() ? Optional.empty() : Optional.of(cues.getFirst());
    }

    public Optional<PlaybackCue> nextCueAfter(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return firstCue();
        }
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).segmentId().equals(segmentId.strip()) && i + 1 < cues.size()) {
                return Optional.of(cues.get(i + 1));
            }
        }
        return Optional.empty();
    }

    public Optional<PlaybackCue> nextCueAfterUnit(String unitId) {
        if (unitId == null || unitId.isBlank()) {
            return firstCue();
        }
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).unitId().equals(unitId.strip()) && i + 1 < cues.size()) {
                return Optional.of(cues.get(i + 1));
            }
        }
        return Optional.empty();
    }

    private static void validate(List<PlaybackCue> cues) {
        LinkedHashSet<String> units = new LinkedHashSet<>();
        double lastEnd = 0.0;
        for (PlaybackCue cue : cues) {
            if (!units.add(cue.unitId())) {
                throw new IllegalArgumentException("Unidad duplicada en playback manifest: " + cue.unitId());
            }
            if (cue.startSeconds() + 0.0001 < lastEnd) {
                throw new IllegalArgumentException("Playback cues must be ordered and non-overlapping");
            }
            lastEnd = cue.endSeconds();
        }
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String portableOptionalPath(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/').strip();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")) {
            throw new IllegalArgumentException("finalAudioRelativePath must be portable and relative: " + value);
        }
        return normalized;
    }
}
