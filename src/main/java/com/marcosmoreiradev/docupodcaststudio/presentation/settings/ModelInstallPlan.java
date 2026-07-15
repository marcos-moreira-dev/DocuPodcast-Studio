package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import java.util.List;
import java.util.Objects;

/**
 * User-facing plan for installing or importing one local AI model.
 *
 * <p>The plan intentionally does not contain raw URLs. Download sources must come from a future
 * versioned manifest with checksums, while the assistant must always keep a manual import path so
 * the product is not broken when an external host changes.</p>
 */
public record ModelInstallPlan(
        String id,
        String engineId,
        String title,
        String badge,
        String storageFolder,
        String installStrategy,
        String offlineFallback,
        String verification,
        String normalUserFlow,
        String advancedNotes,
        List<ModelInstallStep> steps,
        List<String> actions
) {
    public ModelInstallPlan {
        id = require(id, "id");
        engineId = require(engineId, "engineId");
        title = require(title, "title");
        badge = require(badge, "badge");
        storageFolder = require(storageFolder, "storageFolder");
        installStrategy = require(installStrategy, "installStrategy");
        offlineFallback = require(offlineFallback, "offlineFallback");
        verification = require(verification, "verification");
        normalUserFlow = require(normalUserFlow, "normalUserFlow");
        advancedNotes = require(advancedNotes, "advancedNotes");
        steps = List.copyOf(Objects.requireNonNullElse(steps, List.of()));
        actions = List.copyOf(Objects.requireNonNullElse(actions, List.of()));
    }

    public boolean powerfulVoicePlan() {
        return id.equals("model-advanced-voice");
    }

    private static String require(String value, String name) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
