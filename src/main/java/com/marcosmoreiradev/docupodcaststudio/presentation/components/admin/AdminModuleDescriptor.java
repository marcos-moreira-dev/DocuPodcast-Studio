package com.marcosmoreiradev.docupodcaststudio.presentation.components.admin;

import java.util.Objects;

public record AdminModuleDescriptor<M>(M id, String group, String title, String description)
        implements AdminModuleSpec<M> {
    public AdminModuleDescriptor {
        Objects.requireNonNull(id, "id");
        group = require(group, "group");
        title = require(title, "title");
        description = description == null ? "" : description.strip();
    }

    private static String require(String value, String field) {
        String text = value == null ? "" : value.strip();
        if (text.isBlank()) throw new IllegalArgumentException(field + " is required");
        return text;
    }
}
