package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Extensible administrative action identifier. */
public record EngineActionId(String value) {
    public static final EngineActionId INSTALL = new EngineActionId("install");
    public static final EngineActionId IMPORT = new EngineActionId("import");
    public static final EngineActionId START = new EngineActionId("start");
    public static final EngineActionId STOP = new EngineActionId("stop");
    public static final EngineActionId REPAIR = new EngineActionId("repair");
    public static final EngineActionId SMOKE_TEST = new EngineActionId("smoke-test");

    public EngineActionId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("engine action id is required");
    }
}
