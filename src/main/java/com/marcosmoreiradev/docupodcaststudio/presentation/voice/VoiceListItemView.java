package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Compact, human-readable list row for voices created in the project. */
public final class VoiceListItemView extends VBox {
    public VoiceListItemView(VoiceProfile voice, VoiceLibrary library) {
        this(voice, library, null);
    }

    public VoiceListItemView(VoiceProfile voice, VoiceLibrary library, Button action) {
        super(4);
        getStyleClass().add("voice-list-item");
        if (voice == null) {
            getChildren().add(new Label("Voz"));
            return;
        }
        Label name = new Label(VoiceProfilePresentationPolicy.displayName(voice));
        name.setGraphic(SemanticActionIcons.graphicFor("Voz", 17));
        name.getStyleClass().add("voice-browser-name");

        String status = humanStatus(voice, library);
        Label state = new Label(status);
        state.getStyleClass().addAll("voice-list-status", statusClass(status));

        Label type = new Label(humanType(voice));
        type.getStyleClass().add("voice-browser-meta");
        type.setWrapText(true);

        HBox top = new HBox(6, name, state);
        top.getStyleClass().add("voice-list-heading");
        if (action != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            action.setMinWidth(90);
            top.getChildren().addAll(spacer, action);
        }
        getChildren().addAll(top, type);

        FlowPane toneTags = registeredToneTags(voice, library);
        if (!toneTags.getChildren().isEmpty()) {
            getChildren().add(toneTags);
        }
    }

    public static String humanStatus(VoiceProfile voice, VoiceLibrary library) {
        return VoiceProfilePresentationPolicy.status(voice, library);
    }

    static boolean simpleVoice(VoiceProfile voice) {
        return VoiceProfilePresentationPolicy.simpleVoice(voice);
    }

    private static String humanType(VoiceProfile voice) {
        return VoiceProfilePresentationPolicy.typeLabel(voice);
    }

    private static FlowPane registeredToneTags(VoiceProfile voice, VoiceLibrary library) {
        FlowPane tags = new FlowPane(6, 6);
        tags.getStyleClass().add("voice-list-tone-tags");
        tags.setMaxWidth(Double.MAX_VALUE);
        tags.setPrefWrapLength(360);
        var sampleSet = library == null ? java.util.Optional.<com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet>empty()
                : library.referenceSampleSetByVoiceId(voice.id());
        sampleSet.ifPresent(set -> set.registeredTones().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(tone -> {
                    Label tag = new Label(VoiceToneLabelPolicy.comboLabel(tone));
                    tag.getStyleClass().add("voice-list-tone-tag");
                    tag.setMinWidth(58);
                    tag.setAlignment(Pos.CENTER);
                    tags.getChildren().add(tag);
                }));
        return tags;
    }

    private static String statusClass(String status) {
        return switch (status) {
            case "Lista" -> "voice-list-status-ready";
            case "Neutral" -> "voice-list-status-ready";
            case "Incompleta" -> "voice-list-status-pending";
            default -> "voice-list-status-neutral";
        };
    }
}
