package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.Objects;

/** Result of trying to dispatch a command. */
public record AppCommandDispatchResult(AppCommandId commandId, boolean dispatched, String message) {
    public AppCommandDispatchResult {
        Objects.requireNonNull(commandId, "commandId");
        message = message == null ? "" : message.strip();
    }

    public static AppCommandDispatchResult dispatched(AppCommandId id) {
        return new AppCommandDispatchResult(id, true, "Ejecutado.");
    }

    public static AppCommandDispatchResult missingHandler(AppCommandId id) {
        return new AppCommandDispatchResult(id, false, "El comando no tiene handler registrado.");
    }
}
