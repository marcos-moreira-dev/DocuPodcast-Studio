package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import java.util.List;
import java.util.Objects;

/** Intent group in the ribbon catalog. */
public record RibbonGroupDefinition(String title, List<RibbonCommandDefinition> commands) {
    public RibbonGroupDefinition {
        Objects.requireNonNull(title, "title");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
    }
}
