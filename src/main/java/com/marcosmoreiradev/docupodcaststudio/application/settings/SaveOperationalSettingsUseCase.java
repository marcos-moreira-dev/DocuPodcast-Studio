package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.io.IOException;

/** Saves operational settings after normalization and validation. */
public final class SaveOperationalSettingsUseCase {
    private final OperationalSettingsRepository repository;
    private final ValidateOperationalSettingsUseCase validator;

    public SaveOperationalSettingsUseCase(OperationalSettingsRepository repository, ValidateOperationalSettingsUseCase validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public OperationalSettingsValidationReport save(OperationalSettings settings) throws IOException {
        OperationalSettings normalized = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        OperationalSettingsValidationReport report = validator.validate(normalized);
        if (!report.errors().isEmpty()) {
            return report;
        }
        repository.save(normalized);
        return report;
    }
}
