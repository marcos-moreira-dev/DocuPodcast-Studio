package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppStyles;
import javafx.scene.control.Label;

/** Compact badge for a reference tone in the Voice Library. */
public final class VoiceToneBadge extends Label {
    public VoiceToneBadge(VoiceReferenceTone tone, boolean required) {
        super(labelFor(tone));
        getStyleClass().add(AppStyles.UI_INFO_BADGE);
        getStyleClass().add("voice-tone-badge");
        getStyleClass().add(required ? "voice-tone-badge-required" : "voice-tone-badge-optional");
    }

    private static String labelFor(VoiceReferenceTone tone) {
        return tone == null ? "Tono" : tone.displayName();
    }
}
