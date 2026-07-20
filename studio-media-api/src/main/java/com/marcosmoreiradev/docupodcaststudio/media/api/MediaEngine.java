package com.marcosmoreiradev.docupodcaststudio.media.api;

public interface MediaEngine {
    EngineDescriptor descriptor();
    EngineConfigurationSchema configurationSchema();
    EngineReadiness inspectReadiness(EngineConfiguration configuration);
}
