package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import java.util.List;
import java.util.Objects;

/**
 * Product-level engine option shown in the guided configuration surface.
 *
 * <p>This is not the runtime gateway itself. It is the user-facing catalog entry that explains
 * what a motor is for, where its files live and which actions are expected from the assistant
 * UI. Keeping this as a small value object avoids hardcoding large setup texts directly inside
 * the settings dialog.</p>
 */
public record EngineSetupOption(
        String id,
        String title,
        String badge,
        String summary,
        String recommendedFor,
        String modelStorage,
        String guidedSetup,
        String supportedControls,
        String availabilityPolicy,
        List<String> nextActions
) {
    public EngineSetupOption {
        id = require(id, "id");
        title = require(title, "title");
        badge = require(badge, "badge");
        summary = require(summary, "summary");
        recommendedFor = require(recommendedFor, "recommendedFor");
        modelStorage = require(modelStorage, "modelStorage");
        guidedSetup = require(guidedSetup, "guidedSetup");
        supportedControls = require(supportedControls, "supportedControls");
        availabilityPolicy = require(availabilityPolicy, "availabilityPolicy");
        nextActions = List.copyOf(Objects.requireNonNullElse(nextActions, List.of()));
    }

    private static String require(String value, String name) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
