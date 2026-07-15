package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;
import java.util.Objects;

/** Builds human readiness rows for Settings, Voces and Documento without duplicating engine labels. */
public final class InspectAudioEngineReadinessUiUseCase {
    private final ListAudioEngineAvailabilityUseCase availabilityUseCase;

    public InspectAudioEngineReadinessUiUseCase(ListAudioEngineAvailabilityUseCase availabilityUseCase) {
        this.availabilityUseCase = Objects.requireNonNull(availabilityUseCase, "availabilityUseCase");
    }

    public List<AudioEngineReadinessUiItem> inspect() {
        return availabilityUseCase.list().stream()
                .map(AudioEngineReadinessUiItem::fromAvailability)
                .toList();
    }

    public List<String> compactLines() {
        return inspect().stream()
                .map(AudioEngineReadinessUiItem::compactLine)
                .toList();
    }
}
