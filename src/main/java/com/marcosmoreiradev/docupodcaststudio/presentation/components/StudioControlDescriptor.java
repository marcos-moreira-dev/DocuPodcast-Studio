package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import java.util.Objects;

/** Metadata proving that a product control belongs to the shared visual system. */
public record StudioControlDescriptor(
        StudioControlFamily family,
        StudioControlVariant variant,
        StudioControlDensity density,
        String accessibleName,
        String helpText
) {
    public StudioControlDescriptor {
        family = Objects.requireNonNull(family, "control family");
        variant = Objects.requireNonNullElse(variant, StudioControlVariant.REGULAR);
        density = Objects.requireNonNullElse(density, StudioControlDensity.REGULAR);
        accessibleName = Objects.requireNonNullElse(accessibleName, "").strip();
        helpText = Objects.requireNonNullElse(helpText, "").strip();
    }

    public StudioControlDescriptor withHelp(String accessibleName, String helpText) {
        return new StudioControlDescriptor(family, variant, density,
                accessibleName == null || accessibleName.isBlank() ? this.accessibleName : accessibleName,
                helpText == null || helpText.isBlank() ? this.helpText : helpText);
    }
}
