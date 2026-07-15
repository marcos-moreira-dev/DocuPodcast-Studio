package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.List;
import java.util.Objects;

/** Audits that visible commands exposed by menu/ribbon/workspaces can actually be dispatched. */
public final class CommandAuditInspector {
    private CommandAuditInspector() {
    }

    public static CommandAuditReport inspect(AppCommandRegistry registry, AppCommandDispatcher dispatcher) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(dispatcher, "dispatcher");
        List<AppCommandId> visibleWithoutHandler = registry.all().stream()
                .filter(AppCommandDescriptor::visibleByDefault)
                .map(AppCommandDescriptor::id)
                .filter(id -> !dispatcher.canDispatch(id))
                .toList();
        List<AppCommandId> hiddenWithHandler = registry.all().stream()
                .filter(descriptor -> !descriptor.visibleByDefault())
                .map(AppCommandDescriptor::id)
                .filter(dispatcher::canDispatch)
                .toList();
        return new CommandAuditReport(visibleWithoutHandler, hiddenWithHandler);
    }

    public static void requireNoVisibleCommandGaps(AppCommandRegistry registry, AppCommandDispatcher dispatcher) {
        CommandAuditReport report = inspect(registry, dispatcher);
        if (!report.clean()) {
            throw new IllegalStateException(report.summary());
        }
    }
}
