package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.Objects;

/** Inspects product-level readiness for the managed local theatre image engine. */
public final class InspectLocalTheatreImageEngineUseCase {
    private final LocalTheatreImageEngineManager manager;

    public InspectLocalTheatreImageEngineUseCase() {
        this(new LocalTheatreImageEngineManager());
    }

    public InspectLocalTheatreImageEngineUseCase(LocalTheatreImageEngineManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public ImageEngineReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        return manager.inspect(settings, applicationRoot);
    }
}
