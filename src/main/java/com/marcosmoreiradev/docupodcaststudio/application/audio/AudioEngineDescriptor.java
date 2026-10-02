package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;

import java.util.Set;

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
        String message,
        Set<EngineFeature> features,
        boolean diagnosticOnly,
        String technicalIdentity
) {
    public AudioEngineDescriptor(String engineId, String displayName, String mode,
                                 boolean configured, boolean realTts,
                                 String commandPreview, String message) {
        this(engineId, displayName, mode, configured, realTts,
                commandPreview, message, Set.of(), !realTts,
                normalize(engineId) + "|" + normalize(mode));
    }

    public AudioEngineDescriptor(String engineId, String displayName, String mode,
                                 boolean configured, boolean realTts,
                                 String commandPreview, String message,
                                 Set<EngineFeature> features,
                                 boolean diagnosticOnly) {
        this(engineId, displayName, mode, configured, realTts,
                commandPreview, message, features, diagnosticOnly,
                normalize(engineId) + "|" + normalize(mode));
    }

    public AudioEngineDescriptor {
        engineId = normalize(engineId).isBlank() ? "unknown" : normalize(engineId);
        displayName = normalize(displayName).isBlank() ? engineId : normalize(displayName);
        mode = normalize(mode).isBlank() ? "unknown" : normalize(mode);
        commandPreview = normalize(commandPreview);
        message = normalize(message);
        features = features == null ? Set.of() : Set.copyOf(features);
        technicalIdentity = normalize(technicalIdentity).isBlank()
                ? engineId + "|" + mode : normalize(technicalIdentity);
    }

    public static AudioEngineDescriptor mock() {
        return new AudioEngineDescriptor(
                "mock",
                "Audio mock integrado",
                "mock",
                true,
                false,
                "",
                "Motor mock activo. Genera WAVs silenciosos válidos para probar jobs, ETA y reanudación.",
                Set.of(),
                true,
                "mock|integrated"
        );
    }

    public static AudioEngineDescriptor process(String displayName, boolean configured, String commandPreview, String message) {
        return process(displayName, configured, commandPreview, message, Set.of());
    }

    public static AudioEngineDescriptor process(String displayName, boolean configured,
                                                String commandPreview, String message,
                                                Set<EngineFeature> features) {
        return new AudioEngineDescriptor("local-process", displayName, "process",
                configured, true, commandPreview, message, features, false,
                "local-process|process");
    }

    public static AudioEngineDescriptor unavailable(String engineId, String displayName, String message) {
        return new AudioEngineDescriptor(engineId, displayName, "unavailable",
                false, false, "", message, Set.of(), false,
                engineId + "|unavailable");
    }

    public boolean supports(EngineFeature feature) {
        return feature != null && features.contains(feature);
    }

    public String statusLabel() {
        String real = "unavailable".equals(mode) ? "no disponible" : realTts ? "TTS real" : "mock";
        String state = configured ? "configurado" : "no configurado";
        return displayName + " · " + real + " · " + state;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
