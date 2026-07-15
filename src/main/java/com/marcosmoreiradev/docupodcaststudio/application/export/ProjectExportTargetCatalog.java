package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.util.List;

/** Creative export targets visible to the user for each project mode. */
public final class ProjectExportTargetCatalog {
    public List<ExportableArtifactKind> creativeTargets(ProjectMode mode) {
        ProjectMode resolved = mode == null ? ProjectMode.defaultMode() : mode;
        return switch (resolved) {
            case DOCUMENTARY_STUDIO -> List.of(
                    ExportableArtifactKind.PODCAST_WAV,
                    ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO);
            case NARRATIVE_VIDEO -> List.of(
                    ExportableArtifactKind.PODCAST_WAV,
                    ExportableArtifactKind.FINAL_VIDEO_MP4);
            case THEATRE_PRODUCTION -> List.of(
                    ExportableArtifactKind.PODCAST_WAV,
                    ExportableArtifactKind.THEATRE_WORK_VIDEO,
                    ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO,
                    ExportableArtifactKind.THEATRE_PORTION_VIDEO);
        };
    }

    public boolean isCreativeTarget(ProjectMode mode, ExportableArtifactKind kind) {
        return kind != null && creativeTargets(mode).contains(kind);
    }

    public List<ExportableArtifactKind> supportTargets() {
        return List.of(
                ExportableArtifactKind.PROJECT_BUNDLE,
                ExportableArtifactKind.DIAGNOSTIC_REPORT,
                ExportableArtifactKind.STORYBOARD_SUMMARY,
                ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE);
    }
}
