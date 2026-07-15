package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.Objects;

/**
 * User-facing description of the currently wired audio engine.
 *
 * <p>The descriptor lets the UI/documentation distinguish between the always-available mock engine
 * and a configured process-based TTS engine without exposing implementation internals to the shell.</p>
 */
public record AudioEngineDescriptor(
        String engineId,
        String displayName,
        String mode,
        boolean configured,
        boolean realTts,
        String commandPreview,
        String message
) {
    public AudioEngineDescriptor {
        engineId = normalize(engineId).isBlank() ? "unknown" : normalize(engineId);
        displayName = normalize(displayName).isBlank() ? engineId : normalize(displayName);
        mode = normalize(mode).isBlank() ? "unknown" : normalize(mode);
        commandPreview = normalize(commandPreview);
        message = normalize(message);
    }

    public static AudioEngineDescriptor mock() {
        return new AudioEngineDescriptor(
                "mock",
                "Audio mock integrado",
                "mock",
                true,
                false,
                "",
                "Motor mock activo. Genera WAVs silenciosos válidos para probar jobs, ETA y reanudación."
        );
    }

    public static AudioEngineDescriptor process(String displayName, boolean configured, String commandPreview, String message) {
        return new AudioEngineDescriptor("local-process", displayName, "process", configured, true, commandPreview, message);
    }

    public String statusLabel() {
        String real = realTts ? "TTS real" : "mock";
        String state = configured ? "configurado" : "no configurado";
        return displayName + " · " + real + " · " + state;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
