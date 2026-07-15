package com.marcosmoreiradev.docupodcaststudio.presentation;

import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;

/**
 * Presentation composition root for the onboarding build.
 *
 * <p>This class must stay small. As workspaces are implemented, creation should
 * move into dedicated factories such as DocumentWorkspaceFactory,
 * ScriptWorkspaceFactory and StoryboardWorkspaceFactory.</p>
 */
public final class PresentationCompositionRoot {

    public DocuPodcastShellView createMainShell(DocuPodcastShellViewModel viewModel) {
        return new DocuPodcastShellView(viewModel);
    }
}
