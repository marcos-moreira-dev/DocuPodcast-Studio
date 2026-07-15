package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

/** Manifest connecting narration segments to generated audio clips. */
public record AudioManifest(
        String jobId,
        List<AudioClipReference> clips,
        String finalAudioRelativePath,
        Instant createdAt
) {
    public AudioManifest {
        jobId = token(jobId, "jobId");
        clips = clips == null ? List.of() : List.copyOf(clips);
        finalAudioRelativePath = finalAudioRelativePath == null ? "" : finalAudioRelativePath.replace('\\', '/').strip();
        createdAt = createdAt == null ? Instant.now() : createdAt;
        validateUniqueClips(clips);
    }

    public int clipCount() {
        return clips.size();
    }

    public double totalDurationSeconds() {
        return clips.stream().mapToDouble(AudioClipReference::durationSeconds).sum();
    }

    private static void validateUniqueClips(List<AudioClipReference> clips) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        for (AudioClipReference clip : clips) {
            if (!ids.add(clip.id())) {
                throw new IllegalArgumentException("Audio clip duplicado: " + clip.id());
            }
            if (!segments.add(clip.segmentId())) {
                throw new IllegalArgumentException("Segmento duplicado en manifest: " + clip.segmentId());
            }
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
}
