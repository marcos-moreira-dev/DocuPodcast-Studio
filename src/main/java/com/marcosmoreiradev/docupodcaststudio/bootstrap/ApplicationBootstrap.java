package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeImageStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.PresentationCompositionRoot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.FileGenerationJobRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.resources.LocalProjectArtifactStore;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.reading.FileNarrationTranslationCache;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.settings.PropertiesOperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeRoots;

import java.time.Duration;
import java.nio.file.Path;

/**
 * Manual composition root for the first onboarding build.
 *
 * <p>No dependency injection framework is used. The project intentionally starts
 * with explicit composition so future features can be wired by family: project,
 * document, script, voice, storyboard, audio, playback, export and observability.</p>
 */
public final class ApplicationBootstrap {
    private final MediaEnginePlatform mediaEngines;
    private final StudioInkPlatform inkPlatform;
    private final ApplicationRuntimeRoots runtimeRoots;

    private ApplicationBootstrap(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform,
                                 ApplicationRuntimeRoots runtimeRoots) {
        this.mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
        this.inkPlatform = java.util.Objects.requireNonNull(inkPlatform, "ink platform");
        this.runtimeRoots = java.util.Objects.requireNonNull(runtimeRoots, "runtime roots");
    }

    public static ApplicationBootstrap createDefault() {
        return createDefault(MediaEnginePlatform.empty(), StudioInkPlatform.local(), Path.of("."));
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines) {
        return createDefault(mediaEngines, StudioInkPlatform.local(), Path.of("."));
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform) {
        return createDefault(mediaEngines, inkPlatform, Path.of("."));
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform,
                                                     Path applicationRoot) {
        return new ApplicationBootstrap(mediaEngines, inkPlatform, ApplicationRuntimeRoots.unified(applicationRoot));
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform,
                                                     ApplicationRuntimeRoots runtimeRoots) {
        return new ApplicationBootstrap(mediaEngines, inkPlatform, runtimeRoots);
    }

    public ApplicationRuntime bootstrap() {
        var operationalSettings = PropertiesOperationalSettingsRepository.defaultRepository();
        MediaCapabilityService mediaCapabilities = new MediaCapabilityService(mediaEngines,
                PriorityResourceScheduler.incrementalReaderDefaults(), () -> {
                    try {
                        var compute = operationalSettings.load().compute();
                        ComputePreference.Mode mode =
                                !compute.allowGpuForContentAnalysis()
                                        ? ComputePreference.Mode.CPU_ONLY
                                        : switch (compute.policy()) {
                                            case CPU_ONLY -> ComputePreference.Mode.CPU_ONLY;
                                            case PREFER_GPU -> ComputePreference.Mode.PREFER_GPU;
                                            case SPECIFIC_DEVICE -> ComputePreference.Mode.SPECIFIC_DEVICE;
                                            case AUTO -> ComputePreference.Mode.AUTO;
                                        };
                        if (mode == ComputePreference.Mode.SPECIFIC_DEVICE
                                && compute.selectedDeviceId().isBlank()) {
                            mode = ComputePreference.Mode.AUTO;
                        }
                        return new ComputePreference(mode, compute.selectedDeviceId(),
                                compute.allowRamOffloadForContentAnalysis());
                    } catch (java.io.IOException failure) {
                        return ComputePreference.automatic();
                    }
                }, () -> {
                    try {
                        var compute = operationalSettings.load().compute();
                        ComputePreference.Mode mode =
                                !compute.allowGpuForTts()
                                        ? ComputePreference.Mode.CPU_ONLY
                                        : switch (compute.policy()) {
                                            case CPU_ONLY -> ComputePreference.Mode.CPU_ONLY;
                                            case PREFER_GPU -> ComputePreference.Mode.PREFER_GPU;
                                            case SPECIFIC_DEVICE -> ComputePreference.Mode.SPECIFIC_DEVICE;
                                            case AUTO -> ComputePreference.Mode.AUTO;
                                        };
                        if (mode == ComputePreference.Mode.SPECIFIC_DEVICE
                                && compute.selectedDeviceId().isBlank()) {
                            mode = ComputePreference.Mode.AUTO;
                        }
                        return new ComputePreference(
                                mode, compute.selectedDeviceId(), true);
                    } catch (java.io.IOException failure) {
                        return ComputePreference.automatic();
                    }
                });
        InfrastructureServices infrastructureServices = new InfrastructureServicesFactory()
                .create(mediaCapabilities, runtimeRoots.runtimeRoot());
        WorkspaceApplicationServices workspaces = new WorkspaceCompositionFactory().create(
                infrastructureServices, mediaCapabilities, runtimeRoots);
        DocuPodcastShellViewModel shellViewModel = new DocuPodcastShellViewModel(
                workspaces, inkPlatform.inputProviders(), mediaEngines, inkPlatform.drawingFeatures(),
                mediaCapabilities, FileGenerationJobRepository::new,
                new LocalProjectArtifactStore(), new FileNarrationTranslationCache());
        DocuPodcastShellView shellView = new PresentationCompositionRoot().createMainShell(shellViewModel);
        ApplicationLifecycleCoordinator lifecycle = new ApplicationLifecycleCoordinator(Duration.ofSeconds(5),
                java.util.List.of(new LifecycleParticipant() {
                    @Override public void requestCancellation() {
                        if (shellViewModel.audioJobRunningProperty().get()) {
                            shellViewModel.cancelActiveAudioJob();
                        }
                        if (shellViewModel.narrativeVisualGenerationRunningProperty().get()) {
                            shellViewModel.cancelNarrativeVisualGeneration();
                        }
                    }

                    @Override public void close() {
                        shellViewModel.stopPlayback();
                    }
                }, new LifecycleParticipant() {
                    @Override public void close() {
                        ImageEngineSmokeImageStore.cleanupAll();
                    }
                }, new LifecycleParticipant() {
                    @Override public void close() {
                        mediaEngines.close();
                    }
                }));

        return new ApplicationRuntime(
                shellView,
                shellViewModel.windowTitleProperty(),
                ApplicationWindowConfig.defaultConfig(),
                java.util.List.of("/css/docupodcast-light.css"),
                event -> {
                    shellView.handleCloseRequest(event);
                    if (!event.isConsumed()) {
                        lifecycle.close();
                    }
                },
                mediaEngines,
                lifecycle
        );
    }
}
