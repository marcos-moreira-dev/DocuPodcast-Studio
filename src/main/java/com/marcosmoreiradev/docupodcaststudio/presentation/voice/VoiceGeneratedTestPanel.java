package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/** Reusable generated-voice-test panel shared by advanced and local-simple voice modes. */
public final class VoiceGeneratedTestPanel extends VBox {
    public VoiceGeneratedTestPanel(String title, String summary, TextArea phraseInput, Label status, Node... actions) {
        this(title, "Prueba temporal de voz", summary, phraseInput, status, actions);
    }

    public VoiceGeneratedTestPanel(String title, String subtitle, String summary, TextArea phraseInput, Label status, Node... actions) {
        super(8);
        getStyleClass().add("voice-generated-test-panel");
        Label explanation = new Label(summary == null || summary.isBlank()
                ? "Escribe una frase corta y genera una prueba nueva."
                : summary);
        explanation.setWrapText(true);
        explanation.getStyleClass().add("voice-library-body");
        getChildren().addAll(new SectionHeader(title, subtitle), explanation, phraseInput, VoiceActionStrip.of(actions), status);
    }
}
