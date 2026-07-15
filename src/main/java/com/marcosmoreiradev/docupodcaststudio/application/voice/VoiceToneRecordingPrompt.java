package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceToneCategory;

import java.util.Objects;

/** User-facing recording prompt for one advanced voice reference tone. */
public record VoiceToneRecordingPrompt(
        VoiceReferenceTone tone,
        String displayName,
        VoiceReferenceToneCategory category,
        String suggestedRecordingPrompt,
        boolean required
) {
    public VoiceToneRecordingPrompt {
        tone = Objects.requireNonNullElse(tone, VoiceReferenceTone.NEUTRAL);
        displayName = normalize(displayName).isBlank() ? tone.displayName() : normalize(displayName);
        category = category == null ? tone.category() : category;
        suggestedRecordingPrompt = normalize(suggestedRecordingPrompt).isBlank()
                ? tone.suggestedRecordingPrompt()
                : normalize(suggestedRecordingPrompt);
        if (suggestedRecordingPrompt.isBlank()) {
            throw new IllegalArgumentException("Cada tono debe tener frase guia de grabacion");
        }
    }

    public static VoiceToneRecordingPrompt fromTone(VoiceReferenceTone tone) {
        VoiceReferenceTone target = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        return new VoiceToneRecordingPrompt(
                target,
                target.displayName(),
                target.category(),
                target.suggestedRecordingPrompt(),
                target == VoiceReferenceTone.NEUTRAL
        );
    }

    public boolean neutral() {
        return tone == VoiceReferenceTone.NEUTRAL;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
