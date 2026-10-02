package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Reconciles catalog entries created for document-audio jobs.
 *
 * <p>The authority is deliberately narrow: only final audio and manifest
 * entries materialized below {@code jobs/} belong to this lifecycle. User
 * audio clips, voice samples and any media outside that directory are never
 * candidates.</p>
 */
public final class ReconcileGeneratedAudioJobAssetsUseCase {

    /** Removes generated job entries whose physical files no longer exist. */
    public Result removeMissing(DocuPodcastProject project, Path projectFile) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) {
            return Result.unchanged(project);
        }
        return removeMatching(project, reference -> isGeneratedJobAsset(reference)
                && !Files.isRegularFile(root.resolve(reference.relativePath()).normalize()));
    }

    /** Removes all generated job entries after the physical jobs were deleted. */
    public Result removeAll(DocuPodcastProject project) {
        Objects.requireNonNull(project, "project");
        return removeMatching(project, ReconcileGeneratedAudioJobAssetsUseCase::isGeneratedJobAsset);
    }

    private Result removeMatching(
            DocuPodcastProject project,
            java.util.function.Predicate<ProjectAssetReference> predicate) {
        LinkedHashSet<String> removedAssetIds = project.assets().references().stream()
                .filter(predicate)
                .map(ProjectAssetReference::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (removedAssetIds.isEmpty()) {
            return Result.unchanged(project);
        }

        List<String> removedLayerIds = project.narrativeLayerAssignments().stream()
                .filter(layer -> removedAssetIds.contains(layer.targetId()))
                .map(layer -> layer.id())
                .toList();

        DocuPodcastProject updated = project;
        for (String layerId : removedLayerIds) {
            updated = updated.withoutNarrativeLayerAssignment(layerId);
        }
        for (String assetId : removedAssetIds) {
            updated = updated.withoutAsset(assetId);
        }
        if (updated.metadata().kind() == ProjectKind.AUDIO_PROJECT
                && !hasAnyAudioAsset(updated)) {
            updated = updated.withMetadata(updated.metadata()
                    .withKind(ProjectKind.NARRATION_SCRIPT)
                    .withStatus(ProjectStatus.SCRIPT_READY));
        }
        return new Result(updated, List.copyOf(removedAssetIds), removedLayerIds);
    }

    private static boolean hasAnyAudioAsset(DocuPodcastProject project) {
        return project.assets().containsKind(ProjectAssetKind.AUDIO_CLIP)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_FINAL)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_MANIFEST);
    }

    private static boolean isGeneratedJobAsset(ProjectAssetReference reference) {
        ProjectAssetKind kind = reference.kind();
        if (kind != ProjectAssetKind.AUDIO_FINAL
                && kind != ProjectAssetKind.AUDIO_MANIFEST) {
            return false;
        }
        String path = reference.relativePath().replace('\\', '/');
        return path.startsWith("jobs/");
    }

    public record Result(
            DocuPodcastProject project,
            List<String> removedAssetIds,
            List<String> removedLayerAssignmentIds) {
        public Result {
            project = Objects.requireNonNull(project, "project");
            removedAssetIds = removedAssetIds == null ? List.of() : List.copyOf(removedAssetIds);
            removedLayerAssignmentIds = removedLayerAssignmentIds == null
                    ? List.of() : List.copyOf(removedLayerAssignmentIds);
        }

        public static Result unchanged(DocuPodcastProject project) {
            return new Result(project, List.of(), List.of());
        }

        public boolean changed() {
            return !removedAssetIds.isEmpty() || !removedLayerAssignmentIds.isEmpty();
        }
    }
}
