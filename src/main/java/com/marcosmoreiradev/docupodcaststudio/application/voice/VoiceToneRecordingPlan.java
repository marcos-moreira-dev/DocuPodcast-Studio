package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Locale;
import java.util.Objects;

/** Recording plan shown before capturing one voice reference tone. */
public record VoiceToneRecordingPlan(
        String voiceProfileId,
        String voiceDisplayName,
        VoiceReferenceTone tone,
        String toneDisplayName,
        String promptText,
        String suggestedFileName,
        String startLabel,
        String cancelLabel,
        String stopLabel,
        String saveLabel,
        boolean cancelKeepsPreviousSample
) {
    public VoiceToneRecordingPlan {
        voiceProfileId = token(voiceProfileId, "voiceProfileId");
        voiceDisplayName = normalize(voiceDisplayName).isBlank() ? voiceProfileId : normalize(voiceDisplayName);
        tone = Objects.requireNonNullElse(tone, VoiceReferenceTone.NEUTRAL);
        toneDisplayName = normalize(toneDisplayName).isBlank() ? tone.displayName() : normalize(toneDisplayName);
        promptText = normalize(promptText).isBlank() ? tone.suggestedRecordingPrompt() : normalize(promptText);
        suggestedFileName = normalize(suggestedFileName).isBlank()
                ? safeFileName(voiceDisplayName + "-" + toneDisplayName + ".wav")
                : safeFileName(suggestedFileName);
        startLabel = normalize(startLabel).isBlank() ? "Grabar muestra" : normalize(startLabel);
        cancelLabel = normalize(cancelLabel).isBlank() ? "Cancelar" : normalize(cancelLabel);
        stopLabel = normalize(stopLabel).isBlank() ? "Detener grabacion" : normalize(stopLabel);
        saveLabel = normalize(saveLabel).isBlank() ? "Guardar muestra" : normalize(saveLabel);
        cancelKeepsPreviousSample = true;
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

    private static String safeFileName(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        String clean = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (clean.isBlank()) {
            clean = "voz-muestra.wav";
        }
        return clean.endsWith(".wav") ? clean : clean + ".wav";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
