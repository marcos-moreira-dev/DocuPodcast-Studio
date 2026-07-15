package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeImageStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.PresentationCompositionRoot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;

/**
 * Manual composition root for the first onboarding build.
 *
 * <p>No dependency injection framework is used. The project intentionally starts
 * with explicit composition so future features can be wired by family: project,
 * document, script, voice, storyboard, audio, playback, export and observability.</p>
 */
public final class ApplicationBootstrap {

    private ApplicationBootstrap() {
    }

    public static ApplicationBootstrap createDefault() {
        return new ApplicationBootstrap();
    }

    public ApplicationRuntime bootstrap() {
        InfrastructureServices infrastructureServices = new InfrastructureServicesFactory().create();
        ApplicationServices applicationServices = new ApplicationServicesFactory().create(infrastructureServices);
        DocuPodcastShellViewModel shellViewModel = new DocuPodcastShellViewModel(applicationServices);
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
                }
        );
    }
}
