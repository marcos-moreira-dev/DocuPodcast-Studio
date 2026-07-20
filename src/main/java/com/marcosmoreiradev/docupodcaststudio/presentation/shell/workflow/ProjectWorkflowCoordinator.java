package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocumentResult;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectWorkspaceHydration;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityReport;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityUserDecisionFactory;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSessionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates project/session workflows that were previously embedded directly
 * in {@code DocuPodcastShellViewModel}.
 */
public final class ProjectWorkflowCoordinator {
    private static final ProjectModePolicy MODE_POLICY = new ProjectModePolicy();

    private final WorkspaceApplicationServices applicationServices;
    private final ProjectSessionCoordinator sessions;

    public ProjectWorkflowCoordinator(WorkspaceApplicationServices applicationServices, ProjectSessionCoordinator sessions) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    public ProjectSession createNewProject(String title) {
        DocuPodcastProject project = applicationServices.project().project().createProject().create(title);
        return sessions.startNew(project);
    }

    public ProjectSession createNewProject(String title, ProjectMode mode) {
        DocuPodcastProject project = applicationServices.project().project().createProject().create(title, mode);
        return sessions.startNew(project);
    }

    public OpenedProjectContext openProject(Path sourceFile) throws IOException {
        DocuPodcastProject project = applicationServices.project().project().openProject().open(sourceFile);
        ProjectMode resolvedMode = MODE_POLICY.resolve(project);
        boolean migratedMode = project.metadata().mode() != resolvedMode;
        if (migratedMode) {
            project = project.withMetadata(project.metadata().withMode(resolvedMode));
        }
        applicationServices.project().project().validateProjectPayload().validate(project).throwIfInvalid();
        ProjectWorkspaceHydration hydration = applicationServices.project().project().loadProjectWorkspaceArtifacts().load(sourceFile);
        applicationServices.project().project().validateProjectWorkspaceIntegrity()
                .validate(project, sourceFile, hydration)
                .throwIfInvalid();

        ProjectSession session = sessions.open(project, sourceFile);
        if (migratedMode) {
            session.markDirty();
        }
        hydration.importedDocument().ifPresent(session::hydrateImportedDocument);
        hydration.narrationScript().ifPresent(session::hydrateNarrationScript);
        hydration.storyboard().ifPresent(session::hydrateStoryboard);

        return new OpenedProjectContext(
                project,
                session,
                hydration,
                ProjectStartupWorkspacePolicy.initialWorkspace(resolvedMode),
                firstSegmentId(hydration),
                firstStoryboardImageAssetId(hydration)
        );
    }

    public String resolveProjectModeLabel(Path sourceFile, String fallbackLabel) {
        String fallback = fallbackLabel == null ? "" : fallbackLabel.strip();
        if (sourceFile == null) {
            return fallback;
        }
        try {
            DocuPodcastProject project = applicationServices.project().project().openProject().open(sourceFile);
            return MODE_POLICY.resolve(project).displayName();
        } catch (IOException | RuntimeException ex) {
            return fallback;
        }
    }

    public void saveProject(ProjectSession session, Path targetFile, ReadingProfile readingProfile, VoiceLibrary voiceLibrary) throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(targetFile, "targetFile");
        Objects.requireNonNull(readingProfile, "readingProfile");
        Objects.requireNonNull(voiceLibrary, "voiceLibrary");

        DocuPodcastProject projectToSave = session.project()
                .withReadingProfile(readingProfile)
                .withVoiceLibrary(voiceLibrary);
        if (session.importedDocument().isPresent()) {
            MaterializedImportedDocumentResult materializedDocument = applicationServices.project().document()
                    .materializeImportedDocument()
                    .materializeWithResult(projectToSave, session.importedDocument().get(), targetFile);
            projectToSave = materializedDocument.project();
            session.replaceProject(projectToSave, true);
            session.hydrateImportedDocument(materializedDocument.projectSourceDocument());
        }
        projectToSave = applicationServices.administration().voice()
                .materializeVoiceLibrary()
                .materialize(projectToSave, voiceLibrary, targetFile);
        session.replaceProject(projectToSave, true);
        if (session.narrationScript().isPresent()) {
            projectToSave = applicationServices.project().script()
                    .materializeNarrationScript()
                    .materialize(projectToSave, session.narrationScript().get(), targetFile);
            session.replaceProject(projectToSave, true);
        }
        if (session.storyboard().isPresent()) {
            projectToSave = applicationServices.generation().storyboard()
                    .materializeStoryboard()
                    .materialize(projectToSave, session.storyboard().get(), targetFile);
            session.replaceProject(projectToSave, true);
        }
        applicationServices.project().project().saveProject().save(projectToSave, targetFile);
        session.markClean(targetFile);
    }

    public void closeProject() {
        sessions.closeActive();
    }

    public ProjectIntegrityInspectionOutcome inspectIntegrity(ProjectSession session,
                                                               Path projectFile,
                                                               ProjectWorkspaceHydration hydration,
                                                               List<AudioJobSnapshot> audioJobs) throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(projectFile, "projectFile");
        ProjectWorkspaceHydration safeHydration = hydration == null
                ? new ProjectWorkspaceHydration(session.importedDocument(), session.narrationScript(), session.storyboard())
                : hydration;
        ProjectIntegrityReport report = applicationServices.project().project().inspectProjectIntegrity()
                .inspect(session.project(), projectFile, safeHydration, audioJobs == null ? List.of() : audioJobs);
        String status = "Integridad del proyecto: " + report.status().displayName()
                + " · errores " + report.errorCount()
                + " · advertencias " + report.warningCount() + ".";
        return new ProjectIntegrityInspectionOutcome(status,
                report.ok() ? Optional.empty() : Optional.of(ProjectIntegrityUserDecisionFactory.fromReport(report)));
    }

    private static String firstSegmentId(ProjectWorkspaceHydration hydration) {
        return hydration.narrationScript()
                .flatMap(script -> script.segments().stream().findFirst())
                .map(NarrationSegment::id)
                .orElse("");
    }

    private static String firstStoryboardImageAssetId(ProjectWorkspaceHydration hydration) {
        return hydration.storyboard()
                .flatMap(storyboard -> storyboard.bindings().stream().findFirst())
                .map(StoryboardBinding::imageAssetId)
                .orElse("");
    }

}
