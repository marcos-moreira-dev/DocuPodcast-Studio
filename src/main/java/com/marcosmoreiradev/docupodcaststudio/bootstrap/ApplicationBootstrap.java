package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeImageStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.PresentationCompositionRoot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputProviderRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

/**
 * Manual composition root for the first onboarding build.
 *
 * <p>No dependency injection framework is used. The project intentionally starts
 * with explicit composition so future features can be wired by family: project,
 * document, script, voice, storyboard, audio, playback, export and observability.</p>
 */
public final class ApplicationBootstrap {
    private final MediaEnginePlatform mediaEngines;

    private ApplicationBootstrap(MediaEnginePlatform mediaEngines) {
        this.mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
    }

    public static ApplicationBootstrap createDefault() {
        return createDefault(MediaEnginePlatform.empty());
    }

    public static ApplicationBootstrap createDefault(MediaEnginePlatform mediaEngines) {
        return new ApplicationBootstrap(mediaEngines);
    }

    public ApplicationRuntime bootstrap() {
        InfrastructureServices infrastructureServices = new InfrastructureServicesFactory().create();
        ApplicationServices applicationServices = new ApplicationServicesFactory().create(infrastructureServices);
        DocuPodcastShellViewModel shellViewModel = new DocuPodcastShellViewModel(
                applicationServices, InkInputProviderRegistry.localDefaults(), mediaEngines);
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
