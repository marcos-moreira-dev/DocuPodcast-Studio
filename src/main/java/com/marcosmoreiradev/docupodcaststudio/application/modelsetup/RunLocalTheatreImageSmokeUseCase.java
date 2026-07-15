package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Runs the unified readiness/start/smoke flow for the local image engine. */
public final class RunLocalTheatreImageSmokeUseCase {
    private final LocalTheatreImageEngineManager manager;

    public RunLocalTheatreImageSmokeUseCase() {
        this(new LocalTheatreImageEngineManager());
    }

    public RunLocalTheatreImageSmokeUseCase(InspectLocalTheatreImageSetupReadinessUseCase readiness, HttpClient httpClient) {
        this(new LocalTheatreImageEngineManager(
                readiness == null ? new InspectLocalTheatreImageSetupReadinessUseCase() : readiness,
                ExternalProcessRunner.unavailable("Imagen IA teatral"),
                httpClient == null ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build() : httpClient));
    }

    public RunLocalTheatreImageSmokeUseCase(LocalTheatreImageEngineManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public LocalTheatreImageSmokeReport run(OperationalSettings settings, Path applicationRoot) {
        ImageEngineSmokeReport report = runDetailed(settings, applicationRoot);
        return new LocalTheatreImageSmokeReport(report.success(), report.userMessage());
    }

    public ImageEngineSmokeReport runDetailed(OperationalSettings settings, Path applicationRoot) {
        return manager.smoke(settings, applicationRoot);
    }

    public ImageEngineSmokeReport runDetailed(OperationalSettings settings,
                                              Path applicationRoot,
                                              ImageEngineSmokeRequest request) {
        return manager.smoke(settings, applicationRoot, request);
    }

    public ImageEngineSmokeReport runDetailed(OperationalSettings settings,
                                              Path applicationRoot,
                                              String promptText,
                                              int steps) {
        return manager.smoke(settings, applicationRoot, promptText, steps);
    }
}
