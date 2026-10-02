package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;

public interface MediaEngine {
    EngineDescriptor descriptor();
    EngineConfigurationSchema configurationSchema();
    EngineReadiness inspectReadiness(EngineConfiguration configuration);

    /** Presets are opaque to product code and interpreted only by the adapter. */
    default List<EnginePresetDescriptor> presets() { return List.of(); }
}
