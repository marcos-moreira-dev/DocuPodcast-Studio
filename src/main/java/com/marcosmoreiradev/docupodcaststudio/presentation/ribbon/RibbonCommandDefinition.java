package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.util.Objects;

/** One command entry rendered inside a Ribbon group. */
public record RibbonCommandDefinition(AppCommandId commandId, boolean primary) {
    public RibbonCommandDefinition {
        Objects.requireNonNull(commandId, "commandId");
    }
}
