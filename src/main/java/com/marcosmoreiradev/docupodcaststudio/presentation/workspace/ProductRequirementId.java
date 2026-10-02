package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import java.util.Locale;

/** Extensible product-complexity requirement; it is not a closed project-type switch. */
public record ProductRequirementId(String value) {
    public ProductRequirementId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*")) {
            throw new IllegalArgumentException("invalid product requirement id: " + value);
        }
    }
}
