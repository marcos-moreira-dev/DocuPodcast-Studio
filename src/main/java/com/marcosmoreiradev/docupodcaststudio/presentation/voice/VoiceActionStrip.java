package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

/** Action strip for Voices screens that wraps instead of truncating labels. */
final class VoiceActionStrip extends FlowPane {
    private VoiceActionStrip(Node... actions) {
        super(8, 8);
        getStyleClass().add("voice-action-strip");
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(actions);
    }

    static VoiceActionStrip of(Node... actions) {
        return new VoiceActionStrip(actions);
    }
}
