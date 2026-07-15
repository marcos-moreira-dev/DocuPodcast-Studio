package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.io.IOException;

/** Loads persisted operational settings or safe defaults. */
public final class LoadOperationalSettingsUseCase {
    private final OperationalSettingsRepository repository;

    public LoadOperationalSettingsUseCase(OperationalSettingsRepository repository) {
        this.repository = repository;
    }

    public OperationalSettings load() throws IOException {
        return OperationalSettingsMigrationPolicy.repair(repository.load());
    }
}
