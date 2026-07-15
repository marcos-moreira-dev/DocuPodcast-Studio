package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.InfoBadge;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Product-facing voice engine mode card with friendly labels only. */
public final class VoiceEngineModeCard extends VBox {
    public VoiceEngineModeCard(String title, String details, boolean active) {
        super(5);
        getStyleClass().add("voice-engine-mode-card");
        if (active) {
            getStyleClass().add("voice-engine-mode-card-active");
        }
        Label header = new Label(title == null || title.isBlank() ? "Motor de voz" : title);
        header.getStyleClass().add("voice-engine-card-title");
        Label body = new Label(details == null || details.isBlank() ? "Sin descripción." : details);
        body.setWrapText(true);
        body.getStyleClass().add("voice-engine-card-body");
        getChildren().addAll(header, new InfoBadge(active ? "Activo" : "Disponible", active ? "voice-capability-ready" : "voice-capability-reference"), body);
    }
}
