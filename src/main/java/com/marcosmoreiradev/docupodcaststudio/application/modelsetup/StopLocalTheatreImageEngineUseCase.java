package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.Objects;

/** Stops the managed local theatre image engine when DocuPodcast launched it. */
public final class StopLocalTheatreImageEngineUseCase {
    private final LocalTheatreImageEngineManager manager;

    public StopLocalTheatreImageEngineUseCase() {
        this(new LocalTheatreImageEngineManager());
    }

    public StopLocalTheatreImageEngineUseCase(LocalTheatreImageEngineManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public ImageEngineSmokeReport stop(OperationalSettings settings) {
        return manager.stop(settings);
    }
}
