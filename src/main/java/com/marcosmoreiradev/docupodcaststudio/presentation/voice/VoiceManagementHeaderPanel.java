package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.detachNode;

/** Left column for the Manage Voices workspace. */
final class VoiceManagementHeaderPanel extends VBox {
    VoiceManagementHeaderPanel(ListView<VoiceProfile> voiceBrowser) {
        super(8);
        getStyleClass().add("voice-selection-panel");
        Label title = new Label("Voces registradas");
        title.getStyleClass().add("document-side-section-title");
        detachNode(voiceBrowser);
        voiceBrowser.setPrefHeight(260);
        voiceBrowser.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(voiceBrowser, Priority.ALWAYS);
        getChildren().addAll(title, voiceBrowser);
    }
}
