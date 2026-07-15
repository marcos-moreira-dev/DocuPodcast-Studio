package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

/** Presentation policy for tone labels: combo boxes show only the human emotion name. */
public final class VoiceToneLabelPolicy {
    private VoiceToneLabelPolicy() {
    }

    public static String comboLabel(VoiceReferenceTone tone) {
        return tone == null ? "Tono" : tone.displayName();
    }

    public static String registeredToneTags(java.util.Collection<VoiceReferenceTone> tones) {
        if (tones == null || tones.isEmpty()) {
            return "Sin tonos registrados";
        }
        return tones.stream()
                .filter(java.util.Objects::nonNull)
                .map(VoiceToneLabelPolicy::comboLabel)
                .distinct()
                .collect(java.util.stream.Collectors.joining(" · "));
    }
}
