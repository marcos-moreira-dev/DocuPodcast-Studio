package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import java.util.List;
import java.util.Objects;

/** Tab definition for the application ribbon. */
public record RibbonTabDefinition(String id, String title, List<RibbonGroupDefinition> groups) {
    public RibbonTabDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
    }
}
