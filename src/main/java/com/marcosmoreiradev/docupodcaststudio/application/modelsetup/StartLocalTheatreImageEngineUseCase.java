package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.Objects;

/** Starts or detects the managed local theatre image engine. */
public final class StartLocalTheatreImageEngineUseCase {
    private final LocalTheatreImageEngineManager manager;

    public StartLocalTheatreImageEngineUseCase() {
        this(new LocalTheatreImageEngineManager());
    }

    public StartLocalTheatreImageEngineUseCase(LocalTheatreImageEngineManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public ImageEngineSmokeReport start(OperationalSettings settings, Path applicationRoot) {
        return manager.start(settings, applicationRoot);
    }
}
