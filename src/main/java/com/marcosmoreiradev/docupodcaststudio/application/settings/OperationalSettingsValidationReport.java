package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.util.List;

/** Human-readable preflight and validation report for the settings warehouse. */
public record OperationalSettingsValidationReport(
        boolean usable,
        List<String> errors,
        List<String> warnings,
        List<String> information
) {
    public OperationalSettingsValidationReport {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        information = information == null ? List.of() : List.copyOf(information);
        usable = usable && errors.isEmpty();
    }

    public static OperationalSettingsValidationReport ok(List<String> information, List<String> warnings) {
        return new OperationalSettingsValidationReport(true, List.of(), warnings, information);
    }
}
