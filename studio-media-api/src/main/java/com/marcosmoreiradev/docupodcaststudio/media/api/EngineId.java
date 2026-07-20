package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Stable provider identifier persisted by settings and project compatibility adapters. */
public record EngineId(String value) {
    public EngineId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("engine id is required");
    }

    @Override public String toString() { return value; }
}
