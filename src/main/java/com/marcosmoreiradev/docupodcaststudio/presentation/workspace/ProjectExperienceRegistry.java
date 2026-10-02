package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Official registration of product-specific surfaces over shared platform capabilities. */
public final class ProjectExperienceRegistry {
    private final Map<ProjectMode, ProjectExperience> experiences = new EnumMap<>(ProjectMode.class);

    public ProjectExperienceRegistry register(ProjectExperience experience) {
        if (experiences.putIfAbsent(experience.mode(), experience) != null) {
            throw new IllegalArgumentException("duplicate project experience: " + experience.mode());
        }
        return this;
    }

    public ProjectExperience require(ProjectMode mode) {
        ProjectExperience experience = experiences.get(mode);
        if (experience == null) throw new IllegalArgumentException("project experience not registered: " + mode);
        return experience;
    }

    public List<ProjectExperience> experiences() { return List.copyOf(experiences.values()); }

    public static ProjectExperienceRegistry official() {
        return new ProjectExperienceRegistry()
                .register(new ProjectExperience(ProjectMode.DOCUMENTARY_STUDIO, ProductMaturity.STABLE,
                        WorkspaceKind.DOCUMENT_READER,
                        java.util.Set.of(WorkspaceKind.DOCUMENT_READER, WorkspaceKind.VOICE_LIBRARY),
                        java.util.Set.of(AppCommandId.LISTEN_DOCUMENT, AppCommandId.PREPARE_TECHNICAL_PROBLEM,
                                AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO),
                        java.util.Set.of(SideDockModuleId.DOCUMENT_STUDY_VIDEO,
                                SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM),
                        java.util.Set.of("documentary-video", "podcast-wav"),
                        java.util.Set.of(CapabilityId.VOICE_SYNTHESIS, CapabilityId.IMAGE_GENERATION,
                                CapabilityId.VIDEO_RENDERING),
                        java.util.Set.of(ProductRequirements.DOCUMENT_PAGED, ProductRequirements.CONTEXT_RAILS,
                                ProductRequirements.INK_EDITING, ProductRequirements.PERSISTENT_JOBS,
                                ProductRequirements.RECOVERABLE_JOBS, ProductRequirements.LONG_RUNNING_OPERATIONS)))
                .register(new ProjectExperience(ProjectMode.THEATRE_PRODUCTION, ProductMaturity.STABLE,
                        WorkspaceKind.THEATRE_SCRIPT,
                        java.util.Set.of(WorkspaceKind.THEATRE_SCRIPT, WorkspaceKind.THEATRE_IMAGE_GENERATION,
                                WorkspaceKind.VOICE_LIBRARY),
                        java.util.Set.of(AppCommandId.OPEN_THEATRE_IMAGE_GENERATION, AppCommandId.REFRESH_THEATRE_PACKAGE,
                                AppCommandId.EXPORT_THEATRE_WORK),
                        java.util.Set.of(SideDockModuleId.THEATRE_FRAGMENT_IMAGES, SideDockModuleId.THEATRE_CHARACTERS,
                                SideDockModuleId.THEATRE_TEXTUAL_MAP, SideDockModuleId.THEATRE_SPATIAL_MAP,
                                SideDockModuleId.THEATRE_OBJECTS),
                        java.util.Set.of("theatre-work", "theatre-spatial-view"),
                        java.util.Set.of(CapabilityId.VOICE_SYNTHESIS, CapabilityId.IMAGE_GENERATION,
                                CapabilityId.VIDEO_RENDERING),
                        java.util.Set.of(ProductRequirements.CONTEXT_RAILS, ProductRequirements.PRODUCTION_BOARD,
                                ProductRequirements.INK_EDITING, ProductRequirements.MEDIA_CANDIDATE_REVIEW,
                                ProductRequirements.PERSISTENT_JOBS, ProductRequirements.RECOVERABLE_JOBS,
                                ProductRequirements.LONG_RUNNING_OPERATIONS)))
                .register(new ProjectExperience(ProjectMode.NARRATIVE_VIDEO, ProductMaturity.EVOLVING,
                        WorkspaceKind.DOCUMENT_READER,
                        java.util.Set.of(WorkspaceKind.DOCUMENT_READER, WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION,
                                WorkspaceKind.VOICE_LIBRARY),
                        java.util.Set.of(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION,
                                AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE),
                        java.util.Set.of(SideDockModuleId.NARRATIVE_VIDEO_CONTENT),
                        java.util.Set.of("narrative-video"),
                        java.util.Set.of(CapabilityId.VOICE_SYNTHESIS, CapabilityId.IMAGE_GENERATION,
                                CapabilityId.VIDEO_GENERATION, CapabilityId.VIDEO_RENDERING),
                        java.util.Set.of(ProductRequirements.DOCUMENT_PAGED, ProductRequirements.CONTEXT_RAILS,
                                ProductRequirements.MEDIA_CANDIDATE_REVIEW, ProductRequirements.PERSISTENT_JOBS,
                                ProductRequirements.RECOVERABLE_JOBS, ProductRequirements.LONG_RUNNING_OPERATIONS)));
    }
}
