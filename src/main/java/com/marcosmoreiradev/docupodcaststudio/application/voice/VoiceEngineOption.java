package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Guided, user-facing engine option for synthesis.
 *
 * <p>This is not a raw command line profile. It describes what the product can present safely in
 * Configuration while keeping the main document reader simple.</p>
 */
public record VoiceEngineOption(
        String id,
        String displayName,
        String tier,
        String recommendedUse,
        String modelFolder,
        boolean primary,
        boolean requiresModel,
        boolean diagnosticOnly,
        Set<VoiceEngineControl> controls
) {
    public VoiceEngineOption {
        id = normalize(id);
        displayName = normalize(displayName);
        tier = normalize(tier);
        recommendedUse = normalize(recommendedUse);
        modelFolder = normalize(modelFolder);
        controls = controls == null || controls.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(controls));
        if (id.isBlank()) {
            throw new IllegalArgumentException("engine id is required");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("display name is required");
        }
    }

    public boolean supports(VoiceEngineControl control) {
        return control != null && controls.contains(control);
    }

    public boolean supportsExpressiveReferenceVoice() {
        return supports(VoiceEngineControl.REFERENCE_VOICE) && supports(VoiceEngineControl.EMOTION_INTENT);
    }

    public String stateSummary() {
        if (diagnosticOnly) {
            return displayName + " · diagnóstico";
        }
        return displayName + " · " + (requiresModel ? "requiere modelo" : "listo sin modelo externo");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
