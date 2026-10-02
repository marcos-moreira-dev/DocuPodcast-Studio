package com.marcosmoreiradev.docupodcaststudio.application.audio;

/** Selectable audio origin, including unavailable engines with an explicit repair reason. */
public record AudioSourceOption(
        String id,
        String label,
        String backend,
        boolean selectable,
        String message,
        String recommendedAction
) {
    public AudioSourceOption {
        id = clean(id, "unknown");
        label = clean(label, id);
        backend = clean(backend, "");
        message = clean(message, "");
        recommendedAction = clean(recommendedAction, "");
    }

    public static AudioSourceOption from(AudioEngineAvailability availability) {
        return new AudioSourceOption(availability.engineId(), availability.displayName(),
                backend(availability.engineId()), availability.usableInDocument(),
                availability.userMessage(), availability.recommendedAction());
    }

    public String accessibleLabel() {
        return backend.isBlank() ? label : label + " — " + backend;
    }

    @Override public String toString() { return label; }

    private static String backend(String id) {
        return switch (id == null ? "" : id) {
            case "piper" -> "Piper";
            case "xtts" -> "Coqui XTTS";
            case "qwen3-tts-local" -> "llama.cpp local";
            case "mock" -> "Diagnóstico";
            case "computer-audio" -> "Archivo manual";
            default -> "";
        };
    }

    private static String clean(String value, String fallback) {
        String current = value == null ? "" : value.strip();
        return current.isBlank() ? fallback : current;
    }
}
