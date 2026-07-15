package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Effective unit that a TTS audio job must generate.
 *
 * <p>TI2 deliberately keeps the legacy segment language in persisted job files
 * while allowing the job brain to consume RenderUnitPlan units. The identifier
 * may therefore be a classic segment id (SEG-001) or a render unit id
 * (SEG-001-U001). External-audio and visual-silent units do not appear here.</p>
 */
public record AudioGenerationUnit(
        String id,
        String title,
        String text,
        String sourceSegmentId,
        String voiceProfileId,
        String performanceStyleId,
        List<String> appliedLayerIds
) {
    private static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";

    public AudioGenerationUnit {
        id = token(id, "id");
        title = normalize(title);
        text = normalize(text);
        sourceSegmentId = normalize(sourceSegmentId);
        voiceProfileId = normalize(voiceProfileId);
        performanceStyleId = normalize(performanceStyleId);
        appliedLayerIds = appliedLayerIds == null ? List.of() : List.copyOf(appliedLayerIds);
        if (text.isBlank()) {
            throw new IllegalArgumentException("text is required for audio generation units");
        }
    }

    public static AudioGenerationUnit fromSegment(NarrationSegment segment) {
        Objects.requireNonNull(segment, "segment");
        return new AudioGenerationUnit(segment.id(), segment.title(), segment.narrationText(), segment.id(),
                segment.voiceProfileId(), segment.performanceStyleId(), List.of());
    }

    public static AudioGenerationUnit fromRenderUnit(RenderUnit unit) {
        return fromRenderUnit(unit, Optional.empty());
    }

    public static AudioGenerationUnit fromRenderUnit(RenderUnit unit, Optional<NarrationSegment> sourceSegment) {
        Objects.requireNonNull(unit, "unit");
        if (!unit.requiresAudioGeneration()) {
            throw new IllegalArgumentException("RenderUnit does not require TTS generation: " + unit.id());
        }
        String title = unit.title().isBlank() ? unit.segmentId() : unit.title();
        String text = audioTextFor(unit, sourceSegment);
        return new AudioGenerationUnit(unit.id(), title, text, unit.segmentId(),
                unit.voiceProfileId(), unit.performanceStyleId(), unit.appliedLayerIds());
    }

    private static String audioTextFor(RenderUnit unit, Optional<NarrationSegment> sourceSegment) {
        String text = normalize(unit.text());
        if (sourceSegment == null || sourceSegment.isEmpty()) {
            return text;
        }
        String segmentText = normalize(sourceSegment.get().narrationText());
        if (segmentText.isBlank() || segmentText.equals(text)) {
            return text;
        }
        if (segmentText.contains(text)) {
            return text;
        }
        if (text.contains(segmentText)) {
            return segmentText;
        }
        return text;
    }

    public int characterCount() {
        return text.length();
    }

    public String effectiveVoiceProfileId(String fallback) {
        String normalizedFallback = normalize(fallback);
        if (voiceProfileId.isBlank() || DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(voiceProfileId)) {
            return normalizedFallback.isBlank() ? DEFAULT_NARRATOR_VOICE_ID : normalizedFallback;
        }
        return voiceProfileId;
    }

    public String effectiveTitle() {
        if (!title.isBlank()) {
            return title;
        }
        return sourceSegmentId.isBlank() ? id : sourceSegmentId;
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
