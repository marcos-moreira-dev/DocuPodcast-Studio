package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Compact selector of registered reference tones for the voice editor. */
final class VoiceReadyToneMicroCard extends VBox {
    VoiceReadyToneMicroCard() {
        super(6);
        getStyleClass().add("voice-ready-tone-card");
    }

    void refresh(
            VoiceProfile voice,
            Optional<VoiceReferenceSampleSet> samples,
            VoiceReferenceTone selectedTone,
            Consumer<VoiceReferenceTone> toneSelector
    ) {
        getChildren().clear();
        if (voice == null) {
            getChildren().add(note("Sin voz seleccionada."));
            return;
        }
        List<VoiceReferenceTone> tones = readyReferenceTones(voice, samples);
        if (tones.isEmpty()) {
            getChildren().add(note("Sin emociones listas todavía. Registra Neutral para habilitar la voz."));
            return;
        }
        FlowPane chips = new FlowPane(6, 6);
        chips.getStyleClass().add("voice-ready-tone-list");
        for (VoiceReferenceTone tone : tones) {
            chips.getChildren().add(readyToneButton(tone, selectedTone, toneSelector));
        }
        getChildren().addAll(chips, note("Selecciona una emoción lista para escucharla o vuelve a grabarla. Importar o grabar actualiza esa emoción."));
    }

    private static List<VoiceReferenceTone> readyReferenceTones(VoiceProfile voice, Optional<VoiceReferenceSampleSet> samples) {
        List<VoiceReferenceTone> tones = samples
                .map(set -> set.samples().stream()
                        .map(VoiceReferenceSample::tone)
                        .distinct()
                        .sorted(java.util.Comparator.comparingInt(VoiceReferenceTone::ordinal))
                        .toList())
                .orElse(List.of());
        if (tones.isEmpty() && VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice)) {
            return List.of(VoiceReferenceTone.NEUTRAL);
        }
        return tones;
    }

    private static Button readyToneButton(
            VoiceReferenceTone tone,
            VoiceReferenceTone selectedTone,
            Consumer<VoiceReferenceTone> toneSelector
    ) {
        Button chip = ActionButtonFactory.secondary(tone.displayName(), () -> toneSelector.accept(tone));
        chip.getStyleClass().add("voice-ready-tone-chip");
        if (selectedTone == tone) {
            chip.getStyleClass().add("voice-ready-tone-chip-selected");
        }
        chip.setTooltip(new Tooltip("Seleccionar " + tone.displayName()));
        return chip;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }
}
