package com.marcosmoreiradev.docupodcaststudio.domain.narrative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Persistent production state for one narratable Word paragraph. */
public record NarrativeParagraphTake(
        String blockId,
        boolean enabled,
        String keyframeAssetId,
        NarrativeKeyframeSource keyframeSource,
        List<NarrativeGeneratedClip> clips,
        String prompt,
        String negativePrompt,
        long seed,
        String sourceFingerprint,
        boolean stale,
        String notes
) {
    public NarrativeParagraphTake {
        blockId = token(blockId, "blockId");
        keyframeAssetId = optional(keyframeAssetId);
        keyframeSource = Objects.requireNonNullElse(keyframeSource, NarrativeKeyframeSource.NONE);
        if (keyframeAssetId.isBlank()) {
            keyframeSource = NarrativeKeyframeSource.NONE;
        }
        clips = clips == null ? List.of() : clips.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(NarrativeGeneratedClip::order))
                .toList();
        prompt = optional(prompt);
        negativePrompt = optional(negativePrompt);
        sourceFingerprint = optional(sourceFingerprint);
        notes = optional(notes);
    }

    public static NarrativeParagraphTake empty(String blockId) {
        return new NarrativeParagraphTake(blockId, true, "", NarrativeKeyframeSource.NONE,
                List.of(), "", "", 0L, "", true, "");
    }

    public NarrativeParagraphTake withEnabled(boolean value) {
        return new NarrativeParagraphTake(blockId, value, keyframeAssetId, keyframeSource,
                clips, prompt, negativePrompt, seed, sourceFingerprint, stale, notes);
    }

    public NarrativeParagraphTake withKeyframe(String assetId,
                                               NarrativeKeyframeSource source,
                                               String nextPrompt,
                                               String nextNegativePrompt,
                                               long nextSeed,
                                               String fingerprint) {
        return new NarrativeParagraphTake(blockId, enabled, assetId, source, List.of(),
                nextPrompt, nextNegativePrompt, nextSeed, fingerprint, false, notes);
    }

    public NarrativeParagraphTake withClips(List<NarrativeGeneratedClip> nextClips,
                                            String fingerprint) {
        return new NarrativeParagraphTake(blockId, enabled, keyframeAssetId, keyframeSource,
                nextClips, prompt, negativePrompt, seed, fingerprint, false, notes);
    }

    public NarrativeParagraphTake markStale() {
        if (stale) {
            return this;
        }
        return new NarrativeParagraphTake(blockId, enabled, keyframeAssetId, keyframeSource,
                clips, prompt, negativePrompt, seed, sourceFingerprint, true, notes);
    }

    public double generatedDurationSeconds() {
        return clips.stream().mapToDouble(NarrativeGeneratedClip::durationSeconds).sum();
    }

    public boolean keyframeReady() {
        return !keyframeAssetId.isBlank();
    }

    public boolean clipsReady() {
        return !clips.isEmpty() && !stale;
    }

    private static String token(String value, String field) {
        String normalized = optional(value);
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}
