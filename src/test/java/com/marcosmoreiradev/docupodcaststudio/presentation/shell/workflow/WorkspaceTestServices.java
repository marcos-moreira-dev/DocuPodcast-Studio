package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

/** Explicit test fixture for workflows that only exercise a small dependency slice. */
final class WorkspaceTestServices {
    private WorkspaceTestServices() { }

    static WorkspaceApplicationServices empty() {
        return withStoryboard(null);
    }

    static WorkspaceApplicationServices withStoryboard(StoryboardApplicationServices storyboard) {
        return new WorkspaceApplicationServices(
                new WorkspaceApplicationServices.ProjectWorkspace(null, null, null, null, null, null, null, null, null),
                new WorkspaceApplicationServices.PlaybackWorkspace(null, null, null),
                new WorkspaceApplicationServices.GenerationWorkspace(null, storyboard, null, null),
                new WorkspaceApplicationServices.ExportWorkspace(null, null),
                new WorkspaceApplicationServices.AdministrationWorkspace(null, null, null, null, null,
                        MediaEnginePlatform.empty()));
    }
}
