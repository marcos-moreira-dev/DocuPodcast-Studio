package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.InfoBadge;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Reusable card for voice profile, mode and readiness summaries. */
public final class VoiceProfileCard extends VBox {
    public VoiceProfileCard(String title, String subtitle, String details, String chip, String chipClass) {
        super(6);
        getStyleClass().add("voice-profile-card");
        Label header = new Label(title == null || title.isBlank() ? "Voz" : title);
        header.getStyleClass().add("voice-card-title");
        Label sub = new Label(subtitle == null || subtitle.isBlank() ? "Biblioteca de voces" : subtitle);
        sub.getStyleClass().add("voice-card-subtitle");
        sub.setWrapText(true);
        HBox chips = new HBox(6);
        chips.getChildren().add(new InfoBadge(chip == null || chip.isBlank() ? "Sin estado" : chip, chipClass));
        Label body = new Label(details == null || details.isBlank() ? "Sin descripción." : details);
        body.setWrapText(true);
        body.getStyleClass().add("voice-library-body");
        getChildren().addAll(header, sub, chips, body);
    }
}
