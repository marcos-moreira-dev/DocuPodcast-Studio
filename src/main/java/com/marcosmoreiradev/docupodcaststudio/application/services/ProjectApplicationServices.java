package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.project.CreateProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.OpenProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.LoadProjectWorkspaceArtifactsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRoundTripUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.InspectProjectIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.SaveProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ValidateProjectPayloadUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ValidateProjectWorkspaceIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ReconcilePdfOperationAttemptsUseCase;

/** Project-related use cases. */
public record ProjectApplicationServices(
        CreateProjectUseCase createProject,
        SaveProjectUseCase saveProject,
        OpenProjectUseCase openProject,
        ValidateProjectPayloadUseCase validateProjectPayload,
        LoadProjectWorkspaceArtifactsUseCase loadProjectWorkspaceArtifacts,
        ValidateProjectWorkspaceIntegrityUseCase validateProjectWorkspaceIntegrity,
        ReconcilePdfOperationAttemptsUseCase reconcilePdfOperationAttempts,
        InspectProjectIntegrityUseCase inspectProjectIntegrity,
        ProjectRoundTripUseCase projectRoundTrip,
        BatchApplicationServices batch
) {
}
