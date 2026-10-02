package com.marcosmoreiradev.docupodcaststudio.media.api;

public record EngineConfigurationField(
        String key,
        String label,
        String description,
        ConfigurationFieldType type,
        boolean required,
        String defaultValue) {
    public EngineConfigurationField {
        key = require(key, "configuration key");
        label = require(label, "configuration label");
        description = description == null ? "" : description.strip();
        type = type == null ? ConfigurationFieldType.TEXT : type;
        defaultValue = defaultValue == null ? "" : defaultValue;
    }

    private static String require(String value, String field) {
        String text = value == null ? "" : value.strip();
        if (text.isBlank()) throw new IllegalArgumentException(field + " is required");
        return text;
    }
}
