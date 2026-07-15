package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Optional bridge from Settings to Shell-owned support commands. */
public final class SettingsSupportActions {
    private static final SettingsSupportActions UNAVAILABLE = new SettingsSupportActions(id -> false, id -> { });

    private final Predicate<AppCommandId> availability;
    private final Consumer<AppCommandId> dispatcher;

    private SettingsSupportActions(Predicate<AppCommandId> availability, Consumer<AppCommandId> dispatcher) {
        this.availability = Objects.requireNonNull(availability, "availability");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    public static SettingsSupportActions unavailable() {
        return UNAVAILABLE;
    }

    public static SettingsSupportActions of(Predicate<AppCommandId> availability, Consumer<AppCommandId> dispatcher) {
        return new SettingsSupportActions(availability, dispatcher);
    }

    public boolean available(AppCommandId commandId) {
        return commandId != null && availability.test(commandId);
    }

    public boolean anyAvailable(AppCommandId... commandIds) {
        if (commandIds == null) {
            return false;
        }
        for (AppCommandId commandId : commandIds) {
            if (available(commandId)) {
                return true;
            }
        }
        return false;
    }

    public void run(AppCommandId commandId) {
        if (available(commandId)) {
            dispatcher.accept(commandId);
        }
    }
}
