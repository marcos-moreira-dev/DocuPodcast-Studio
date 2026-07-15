package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.util.ArrayList;
import java.util.List;

/** Performs user-facing validation for a voice library. Domain invariants already cover hard errors. */
public final class ValidateVoiceLibraryUseCase {
    public List<String> validate(VoiceLibrary library) {
        ArrayList<String> issues = new ArrayList<>();
        if (library == null) {
            issues.add("No hay biblioteca de voces.");
            return List.copyOf(issues);
        }
        if (library.voices().isEmpty()) {
            issues.add("La biblioteca no tiene voces disponibles.");
        }
        if (library.characters().isEmpty()) {
            issues.add("La biblioteca no tiene personajes/narradores.");
        }
        if (library.styles().isEmpty()) {
            issues.add("La biblioteca no tiene estilos de interpretación.");
        }
        library.voices().stream()
                .filter(voice -> !voice.usableForTts())
                .forEach(voice -> issues.add("La voz " + voice.id() + " necesita muestra/modelo antes de usarse como audio humano."));
        library.styles().stream()
                .filter(style -> style.requiresEngineSupport())
                .forEach(style -> issues.add("El estilo " + style.id() + " es una intención: requiere soporte del motor o muestras adecuadas."));
        return List.copyOf(issues);
    }
}
