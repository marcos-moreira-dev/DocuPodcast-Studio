package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

/**
 * Future-ready assignment over a text range: AI voice or human recording, optional style and optional image binding.
 */
public record PerformanceSpan(
        String id,
        ScriptTextRange textRange,
        VoiceSourceKind voiceSourceKind,
        String voiceProfileId,
        String humanAudioAssetId,
        String performanceStyleId,
        String imageAssetId,
        String notes
) {
    public PerformanceSpan {
        id = token(id, "id");
        if (textRange == null) {
            throw new IllegalArgumentException("textRange is required");
        }
        voiceSourceKind = voiceSourceKind == null ? VoiceSourceKind.UNASSIGNED : voiceSourceKind;
        voiceProfileId = normalize(voiceProfileId);
        humanAudioAssetId = normalize(humanAudioAssetId);
        performanceStyleId = normalize(performanceStyleId);
        imageAssetId = normalize(imageAssetId);
        notes = normalize(notes);
        validateSource(voiceSourceKind, voiceProfileId, humanAudioAssetId);
    }

    public boolean usesAiVoice() {
        return voiceSourceKind == VoiceSourceKind.AI_TTS;
    }

    public boolean usesHumanAudio() {
        return voiceSourceKind == VoiceSourceKind.HUMAN_RECORDING;
    }

    private static void validateSource(VoiceSourceKind kind, String voiceProfileId, String humanAudioAssetId) {
        if (kind == VoiceSourceKind.AI_TTS && voiceProfileId.isBlank()) {
            throw new IllegalArgumentException("AI_TTS performance spans require voiceProfileId");
        }
        if (kind == VoiceSourceKind.HUMAN_RECORDING && humanAudioAssetId.isBlank()) {
            throw new IllegalArgumentException("Human recording performance spans require humanAudioAssetId");
        }
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
