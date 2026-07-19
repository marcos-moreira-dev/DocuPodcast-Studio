package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDiscoveryGateway;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;

import java.net.http.HttpClient;

/**
 * Compatibility adapter for the first consumer of the transverse visual engine.
 *
 * @deprecated Inject {@link LocalVisualImageEngineManager} in new code.
 */
@Deprecated(forRemoval = false)
public final class LocalTheatreImageEngineManager extends LocalVisualImageEngineManager {
    public LocalTheatreImageEngineManager() {
        super();
    }

    public LocalTheatreImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                          ExternalProcessRunner processRunner,
                                          HttpClient httpClient) {
        super(readinessUseCase, processRunner, httpClient);
    }

    public LocalTheatreImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                          ExternalProcessRunner processRunner,
                                          ComfyUiVisualEngineClient visualEngineClient) {
        super(readinessUseCase, processRunner, visualEngineClient);
    }

    public LocalTheatreImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                          ExternalProcessRunner processRunner,
                                          ComfyUiVisualEngineClient visualEngineClient,
                                          ComputeDeviceDiscoveryGateway computeDeviceDiscoveryGateway) {
        super(readinessUseCase, processRunner, visualEngineClient, computeDeviceDiscoveryGateway);
    }
}
