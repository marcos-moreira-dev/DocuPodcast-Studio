package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportedDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocument;
import com.marcosmoreiradev.docupodcaststudio.application.script.MaterializedNarrationScript;
import com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.MaterializedStoryboard;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.StoryboardWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Saves, reopens and verifies the user-level project round-trip.
 *
 * <p>This is deliberately a brain-side use case, not a UI smoke script. The root
 * object remains the narrated document project. The narration projection, layers,
 * storyboard and audio jobs are derived project artifacts that must survive closing
 * and reopening the project without modifying the source Word/PDF/Markdown/TXT.</p>
 */
public final class ProjectRoundTripUseCase {
    private final ProjectRepository projectRepository;
    private final ImportedDocumentWorkspaceRepository importedDocumentRepository;
    private final NarrationScriptWorkspaceRepository narrationScriptRepository;
    private final StoryboardWorkspaceRepository storyboardRepository;
    private final AudioJobRepository audioJobRepository;

    public ProjectRoundTripUseCase(
            ProjectRepository projectRepository,
            ImportedDocumentWorkspaceRepository importedDocumentRepository,
            NarrationScriptWorkspaceRepository narrationScriptRepository,
            StoryboardWorkspaceRepository storyboardRepository,
            AudioJobRepository audioJobRepository
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.importedDocumentRepository = Objects.requireNonNull(importedDocumentRepository, "importedDocumentRepository");
        this.narrationScriptRepository = Objects.requireNonNull(narrationScriptRepository, "narrationScriptRepository");
        this.storyboardRepository = Objects.requireNonNull(storyboardRepository, "storyboardRepository");
        this.audioJobRepository = Objects.requireNonNull(audioJobRepository, "audioJobRepository");
    }

    public ProjectRoundTripResult execute(ProjectRoundTripRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        Path projectFile = request.projectFile().toAbsolutePath().normalize();
        Path projectDirectory = projectFile.getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }

        ArrayList<String> messages = new ArrayList<>();
        DocuPodcastProject projectToSave = request.project();
        int initialAssetCount = projectToSave.assets().size();

        if (request.importedDocumentOptional().isPresent()) {
            MaterializedImportedDocument materialized = importedDocumentRepository.materialize(
                    request.importedDocumentOptional().get(),
                    projectToSave.readingProfile(),
                    projectFile
            );
            projectToSave = withAsset(projectToSave, materialized.sourceDocumentAsset());
            projectToSave = withAsset(projectToSave, materialized.importedDocumentAsset());
            messages.add("Documento narrable materializado para reapertura");
        }

        if (request.narrationProjectionOptional().isPresent()) {
            MaterializedNarrationScript materialized = narrationScriptRepository.materialize(
                    request.narrationProjectionOptional().get(),
                    projectFile
            );
            projectToSave = withAsset(projectToSave, materialized.narrationScriptAsset());
            messages.add("Proyección interna de narración materializada");
        }

        if (request.storyboardOptional().isPresent()) {
            MaterializedStoryboard materialized = storyboardRepository.materialize(projectFile, request.storyboardOptional().get());
            projectToSave = withAsset(projectToSave, materialized.asset());
            messages.add("Storyboard materializado para reapertura");
        }

        for (AudioJobSnapshot job : request.audioJobs()) {
            audioJobRepository.save(projectDirectory, job);
        }
        if (!request.audioJobs().isEmpty()) {
            messages.add("Jobs de audio persistidos para recuperación");
        }

        projectRepository.save(projectToSave, projectFile);
        DocuPodcastProject restoredProject = projectRepository.open(projectFile);
        ProjectWorkspaceHydration hydration = new ProjectWorkspaceHydration(
                importedDocumentRepository.load(projectFile),
                narrationScriptRepository.load(projectFile),
                storyboardRepository.load(projectFile)
        );
        List<AudioJobSnapshot> restoredJobs = audioJobRepository.list(projectDirectory);
        messages.add(hydration.statusLabel());

        return new ProjectRoundTripResult(
                restoredProject,
                hydration,
                restoredJobs,
                request.importedDocumentOptional().isPresent(),
                request.narrationProjectionOptional().isPresent(),
                request.storyboardOptional().isPresent(),
                !request.audioJobs().isEmpty(),
                projectToSave.narrativeLayerAssignments().size(),
                Math.max(initialAssetCount, projectToSave.assets().size()),
                messages
        );
    }

    private static DocuPodcastProject withAsset(DocuPodcastProject project, ProjectAssetReference reference) {
        return project.withAsset(reference);
    }
}
