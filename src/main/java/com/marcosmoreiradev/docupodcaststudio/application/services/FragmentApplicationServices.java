package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.BuildFragmentWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjectionCoordinator;

/** Fragment projection use cases and coordinators. */
public record FragmentApplicationServices(
        BuildFragmentWorkspaceProjectionUseCase buildFragmentWorkspaceProjection,
        FragmentWorkspaceProjectionCoordinator fragmentWorkspaceProjection
) {
}
