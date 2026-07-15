package com.marcosmoreiradev.docupodcaststudio.domain.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignmentPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Root aggregate for the early DocuPodcast project format. */
public record DocuPodcastProject(
        ProjectMetadata metadata,
        ProjectAssetCatalog assets,
        ReadingProfile readingProfile,
        VoiceLibrary voiceLibrary,
        List<NarrativeLayerAssignment> narrativeLayerAssignments,
        TheatreProjectLayer theatre,
        StudyProjectLayer study,
        Map<String, String> viewState
) {
    public DocuPodcastProject {
        metadata = Objects.requireNonNull(metadata, "metadata");
        assets = Objects.requireNonNullElseGet(assets, ProjectAssetCatalog::empty);
        readingProfile = Objects.requireNonNullElseGet(readingProfile, ReadingProfile::academicDefaults);
        voiceLibrary = Objects.requireNonNullElseGet(voiceLibrary, VoiceLibrary::defaults);
        narrativeLayerAssignments = normalizeAssignments(narrativeLayerAssignments);
        theatre = Objects.requireNonNullElseGet(theatre, TheatreProjectLayer::empty);
        study = Objects.requireNonNullElseGet(study, StudyProjectLayer::empty);
        viewState = viewState == null ? Map.of() : Map.copyOf(viewState);
    }

    public DocuPodcastProject(ProjectMetadata metadata, ProjectAssetCatalog assets, Map<String, String> viewState) {
        this(metadata, assets, ReadingProfile.academicDefaults(), VoiceLibrary.defaults(), List.of(), TheatreProjectLayer.empty(), StudyProjectLayer.empty(), viewState);
    }

    public DocuPodcastProject(ProjectMetadata metadata, ProjectAssetCatalog assets, ReadingProfile readingProfile, Map<String, String> viewState) {
        this(metadata, assets, readingProfile, VoiceLibrary.defaults(), List.of(), TheatreProjectLayer.empty(), StudyProjectLayer.empty(), viewState);
    }

    public DocuPodcastProject(
            ProjectMetadata metadata,
            ProjectAssetCatalog assets,
            ReadingProfile readingProfile,
            VoiceLibrary voiceLibrary,
            Map<String, String> viewState) {
        this(metadata, assets, readingProfile, voiceLibrary, List.of(), TheatreProjectLayer.empty(), StudyProjectLayer.empty(), viewState);
    }

    public DocuPodcastProject(
            ProjectMetadata metadata,
            ProjectAssetCatalog assets,
            ReadingProfile readingProfile,
            VoiceLibrary voiceLibrary,
            List<NarrativeLayerAssignment> narrativeLayerAssignments,
            Map<String, String> viewState) {
        this(metadata, assets, readingProfile, voiceLibrary, narrativeLayerAssignments, TheatreProjectLayer.empty(), StudyProjectLayer.empty(), viewState);
    }

    public DocuPodcastProject(
            ProjectMetadata metadata,
            ProjectAssetCatalog assets,
            ReadingProfile readingProfile,
            VoiceLibrary voiceLibrary,
            List<NarrativeLayerAssignment> narrativeLayerAssignments,
            TheatreProjectLayer theatre,
            Map<String, String> viewState) {
        this(metadata, assets, readingProfile, voiceLibrary, narrativeLayerAssignments, theatre, StudyProjectLayer.empty(), viewState);
    }

    public static DocuPodcastProject createNew(String title) {
        return createNew(title, ProjectMode.defaultMode());
    }

    public static DocuPodcastProject createNew(String title, ProjectMode mode) {
        return new DocuPodcastProject(
                ProjectMetadata.create(title, mode),
                ProjectAssetCatalog.empty(),
                ReadingProfile.academicDefaults(),
                VoiceLibrary.defaults(),
                List.of(),
                TheatreProjectLayer.empty(),
                StudyProjectLayer.empty(),
                Map.of("activeWorkspace", "WELCOME_HOME")
        );
    }

    /**
     * Compatibility factory used by tests and lightweight scaffolding code.
     *
     * <p>The project is not semantically different from {@link #createNew(String)};
     * the name makes tests read naturally when they only need an empty project
     * aggregate with defaults.</p>
     */
    public static DocuPodcastProject empty(String title) {
        return createNew(title);
    }

    public DocuPodcastProject withAsset(ProjectAssetReference reference) {
        return new DocuPodcastProject(touch(metadata), assets.withReference(reference), readingProfile, voiceLibrary, narrativeLayerAssignments, theatre, study, viewState);
    }

    public DocuPodcastProject withoutAsset(String assetId) {
        return new DocuPodcastProject(touch(metadata), assets.withoutReference(assetId), readingProfile, voiceLibrary, narrativeLayerAssignments, theatre, study, viewState);
    }

    public DocuPodcastProject withMetadata(ProjectMetadata newMetadata) {
        return new DocuPodcastProject(newMetadata, assets, readingProfile, voiceLibrary, narrativeLayerAssignments, theatre, study, viewState);
    }

    public DocuPodcastProject withReadingProfile(ReadingProfile newReadingProfile) {
        return new DocuPodcastProject(touch(metadata), assets, Objects.requireNonNull(newReadingProfile, "newReadingProfile"), voiceLibrary, narrativeLayerAssignments, theatre, study, viewState);
    }

    public DocuPodcastProject withVoiceLibrary(VoiceLibrary newVoiceLibrary) {
        return new DocuPodcastProject(touch(metadata), assets, readingProfile, Objects.requireNonNull(newVoiceLibrary, "newVoiceLibrary"), narrativeLayerAssignments, theatre, study, viewState);
    }

    public DocuPodcastProject withTheatre(TheatreProjectLayer newTheatre) {
        return new DocuPodcastProject(touch(metadata), assets, readingProfile, voiceLibrary, narrativeLayerAssignments,
                Objects.requireNonNullElseGet(newTheatre, TheatreProjectLayer::empty), study, viewState);
    }

    public DocuPodcastProject withStudy(StudyProjectLayer newStudy) {
        return new DocuPodcastProject(touch(metadata), assets, readingProfile, voiceLibrary, narrativeLayerAssignments,
                theatre, Objects.requireNonNullElseGet(newStudy, StudyProjectLayer::empty), viewState);
    }

    /**
     * Adds or replaces a project-side production layer. The assignment is stored in
     * the DocuPodcast project and never written into the imported Word/DOCX. When a
     * primary narration layer overlaps another primary layer, the caller must first
     * remove, replace explicitly or split the existing range.
     */
    public DocuPodcastProject withNarrativeLayerAssignment(NarrativeLayerAssignment candidate) {
        Objects.requireNonNull(candidate, "candidate");
        ArrayList<NarrativeLayerAssignment> updated = new ArrayList<>();
        for (NarrativeLayerAssignment existing : narrativeLayerAssignments) {
            if (!existing.id().equals(candidate.id())) {
                if (!NarrativeLayerAssignmentPolicy.compatible(existing, candidate)) {
                    throw new IllegalArgumentException(NarrativeLayerAssignmentPolicy.conflictMessage(existing, candidate));
                }
                updated.add(existing);
            }
        }
        updated.add(candidate);
        return withNarrativeLayerAssignments(updated);
    }

    public DocuPodcastProject withoutNarrativeLayerAssignment(String assignmentId) {
        String normalized = assignmentId == null ? "" : assignmentId.strip();
        if (normalized.isBlank()) {
            return this;
        }
        List<NarrativeLayerAssignment> updated = narrativeLayerAssignments.stream()
                .filter(assignment -> !assignment.id().equals(normalized))
                .toList();
        return withNarrativeLayerAssignments(updated);
    }

    public DocuPodcastProject withNarrativeLayerAssignments(List<NarrativeLayerAssignment> assignments) {
        return new DocuPodcastProject(touch(metadata), assets, readingProfile, voiceLibrary, assignments, theatre, study, viewState);
    }

    public DocuPodcastProject withViewState(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("viewState key is required");
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(viewState);
        if (value == null) {
            updated.remove(key);
        } else {
            updated.put(key, value);
        }
        return new DocuPodcastProject(touch(metadata), assets, readingProfile, voiceLibrary, narrativeLayerAssignments, theatre, study, updated);
    }

    private static ProjectMetadata touch(ProjectMetadata metadata) {
        return new ProjectMetadata(
                metadata.id(),
                metadata.title(),
                metadata.description(),
                metadata.language(),
                metadata.kind(),
                metadata.mode(),
                metadata.status(),
                metadata.createdAt(),
                Instant.now()
        );
    }

    private static List<NarrativeLayerAssignment> normalizeAssignments(List<NarrativeLayerAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }
        ArrayList<NarrativeLayerAssignment> normalized = new ArrayList<>();
        for (NarrativeLayerAssignment candidate : assignments) {
            Objects.requireNonNull(candidate, "narrativeLayerAssignments item");
            for (NarrativeLayerAssignment existing : normalized) {
                if (!existing.id().equals(candidate.id()) && !NarrativeLayerAssignmentPolicy.compatible(existing, candidate)) {
                    throw new IllegalArgumentException(NarrativeLayerAssignmentPolicy.conflictMessage(existing, candidate));
                }
            }
            normalized.removeIf(existing -> existing.id().equals(candidate.id()));
            normalized.add(candidate);
        }
        return List.copyOf(normalized);
    }
}
