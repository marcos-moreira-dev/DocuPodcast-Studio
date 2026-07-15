package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Reusable row that shows one stored voice sample and its reference tone. */
public final class VoiceSampleRow extends HBox {
    public VoiceSampleRow(VoiceReferenceSample sample) {
        super(10);
        getStyleClass().add("voice-sample-row");
        VoiceToneBadge tone = new VoiceToneBadge(sample == null ? null : sample.tone(), sample != null && sample.isNeutral());
        tone.setMinWidth(Region.USE_PREF_SIZE);
        tone.setMaxWidth(Region.USE_PREF_SIZE);

        Label origin = new Label(originFor(sample));
        origin.setWrapText(true);
        origin.getStyleClass().add("voice-tone-sample-title");
        Label meta = new Label(detailFor(sample));
        meta.setWrapText(true);
        meta.getStyleClass().add("voice-tone-sample-meta");

        VBox detail = new VBox(2, origin, meta);
        detail.getStyleClass().add("voice-tone-sample-detail");
        getChildren().addAll(tone, detail);
        HBox.setHgrow(detail, Priority.ALWAYS);
    }

    private static String originFor(VoiceReferenceSample sample) {
        if (sample == null) {
            return "Muestra no disponible.";
        }
        return sample.origin().displayName();
    }

    private static String detailFor(VoiceReferenceSample sample) {
        if (sample == null) {
            return "Sin archivo asociado.";
        }
        return sample.ownership().displayName() + " · " + sample.fileUri();
    }
}
