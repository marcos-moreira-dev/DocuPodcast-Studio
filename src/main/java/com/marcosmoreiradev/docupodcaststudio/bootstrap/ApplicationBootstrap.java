package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeImageStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.PresentationCompositionRoot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;

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

    private ApplicationBootstrap(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform) {
        this.mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
        this.inkPlatform = java.util.Objects.requireNonNull(inkPlatform, "ink platform");
    }

    public static ApplicationBootstrap createDefault() {
        return createDefault(MediaEnginePlatform.empty(), StudioInkPlatform.local());
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines) {
        return createDefault(mediaEngines, StudioInkPlatform.local());
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform) {
        return new ApplicationBootstrap(mediaEngines, inkPlatform);
    }

    public ApplicationRuntime bootstrap() {
        MediaCapabilityService mediaCapabilities = new MediaCapabilityService(mediaEngines,
                LocalResourceScheduler.safeDefaults());
        InfrastructureServices infrastructureServices = new InfrastructureServicesFactory().create(mediaCapabilities);
        WorkspaceApplicationServices workspaces = new WorkspaceCompositionFactory().create(
                infrastructureServices, mediaCapabilities);
        DocuPodcastShellViewModel shellViewModel = new DocuPodcastShellViewModel(
                workspaces, inkPlatform.inputProviders(), mediaEngines, inkPlatform.drawingFeatures(),
                mediaCapabilities);
        DocuPodcastShellView shellView = new PresentationCompositionRoot().createMainShell(shellViewModel);

        return new ApplicationRuntime(
                shellView,
                shellViewModel.windowTitleProperty(),
                ApplicationWindowConfig.defaultConfig(),
                java.util.List.of("/css/docupodcast-light.css"),
                event -> {
                    shellView.handleCloseRequest(event);
                    if (!event.isConsumed()) {
                        ImageEngineSmokeImageStore.cleanupAll();
                    }
                },
                mediaEngines
        );
    }
}
