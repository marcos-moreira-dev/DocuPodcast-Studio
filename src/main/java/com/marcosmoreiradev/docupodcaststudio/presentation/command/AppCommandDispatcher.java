package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Dispatches command IDs to one registered handler, avoiding duplicated surface logic. */
public final class AppCommandDispatcher {
    private final AppCommandRegistry registry;
    private final Map<AppCommandId, AppCommandHandler> handlers = new EnumMap<>(AppCommandId.class);

    public AppCommandDispatcher(AppCommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public static AppCommandDispatcher emptyOfficial() {
        return new AppCommandDispatcher(AppCommandRegistry.official());
    }

    public AppCommandDispatcher register(AppCommandId id, AppCommandHandler handler) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(handler, "handler");
        registry.descriptor(id);
        handlers.put(id, handler);
        return this;
    }

    public boolean canDispatch(AppCommandId id) {
        return handlers.containsKey(Objects.requireNonNull(id, "id"));
    }

    public AppCommandDispatchResult dispatch(AppCommandId id) {
        Objects.requireNonNull(id, "id");
        registry.descriptor(id);
        AppCommandHandler handler = handlers.get(id);
        if (handler == null) {
            return AppCommandDispatchResult.missingHandler(id);
        }
        handler.handle();
        return AppCommandDispatchResult.dispatched(id);
    }

    public Set<AppCommandId> registeredCommandIds() {
        return Set.copyOf(handlers.keySet());
    }

    public AppCommandRegistry registry() {
        return registry;
    }
}
