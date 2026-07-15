package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.List;

/** Result of auditing visible commands against registered handlers. */
public record CommandAuditReport(
        List<AppCommandId> visibleCommandsWithoutHandler,
        List<AppCommandId> hiddenCommandsWithHandler
) {
    public CommandAuditReport {
        visibleCommandsWithoutHandler = List.copyOf(visibleCommandsWithoutHandler == null ? List.of() : visibleCommandsWithoutHandler);
        hiddenCommandsWithHandler = List.copyOf(hiddenCommandsWithHandler == null ? List.of() : hiddenCommandsWithHandler);
    }

    public boolean clean() {
        return visibleCommandsWithoutHandler.isEmpty();
    }

    public String summary() {
        if (clean()) {
            return "Todos los comandos visibles tienen handler real.";
        }
        return "Comandos visibles sin handler: " + visibleCommandsWithoutHandler;
    }
}
