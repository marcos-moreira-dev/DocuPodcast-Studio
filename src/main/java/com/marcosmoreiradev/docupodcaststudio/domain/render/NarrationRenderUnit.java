package com.marcosmoreiradev.docupodcaststudio.domain.render;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.util.List;
import java.util.Objects;

/**
 * Minimal render unit for narration audio.
 *
 * <p>T91 introduces this as the bridge between the reading surface, project layers
 * and audio generation. Existing jobs still render by segment, but new code can
 * reason about the finer unit that should eventually become the playback/export
 * cue.</p>
 */
public record NarrationRenderUnit(
        String id,
        String segmentId,
        int index,
        ScriptTextRange scriptRange,
        DocumentTextRange documentRange,
        String text,
        NarrationRenderSourceKind sourceKind,
        String voiceProfileId,
        String performanceStyleId,
        String audioAssetId,
        String imageAssetId,
        List<String> appliedLayerIds
) {
    public NarrationRenderUnit {
        id = token(id, "id");
        segmentId = token(segmentId, "segmentId");
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        scriptRange = Objects.requireNonNull(scriptRange, "scriptRange");
        if (!scriptRange.segmentId().equals(segmentId)) {
            throw new IllegalArgumentException("scriptRange must belong to segmentId");
        }
        text = normalize(text);
        if (text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        sourceKind = sourceKind == null ? NarrationRenderSourceKind.TEXT_TO_SPEECH : sourceKind;
        voiceProfileId = normalize(voiceProfileId);
        performanceStyleId = normalize(performanceStyleId);
        audioAssetId = normalize(audioAssetId);
        imageAssetId = normalize(imageAssetId);
        appliedLayerIds = appliedLayerIds == null ? List.of() : List.copyOf(appliedLayerIds);
        if (sourceKind.tts() && voiceProfileId.isBlank()) {
            throw new IllegalArgumentException("voiceProfileId is required for TEXT_TO_SPEECH units");
        }
        if (sourceKind.externalAudio() && audioAssetId.isBlank()) {
            throw new IllegalArgumentException("audioAssetId is required for AUDIO_CLIP units");
        }
    }

    public boolean usesTts() {
        return sourceKind.tts();
    }

    public boolean usesAudioClip() {
        return sourceKind.externalAudio();
    }

    public boolean hasImage() {
        return !imageAssetId.isBlank();
    }

    public boolean hasDocumentRange() {
        return documentRange != null;
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
