package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.Objects;

/** Starts or detects the managed local theatre image engine. */
public final class StartLocalTheatreImageEngineUseCase {
    private final LocalVisualImageEngineManager manager;

    public StartLocalTheatreImageEngineUseCase() {
        this(new LocalVisualImageEngineManager());
    }

    public StartLocalTheatreImageEngineUseCase(LocalVisualImageEngineManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public ImageEngineSmokeReport start(OperationalSettings settings, Path applicationRoot) {
        return manager.start(settings, applicationRoot);
    }
}
