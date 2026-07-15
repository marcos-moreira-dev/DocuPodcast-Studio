package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Handles image asset cleanup and removal from projects for the shell. */
public final class ProjectImageManagementWorkflow {
    private final ApplicationServices applicationServices;

    public ProjectImageManagementWorkflow(ApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public record RemoveAllImagesResult(
            int removedCount,
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            boolean nothingToRemove) {
        public static RemoveAllImagesResult empty() {
            return new RemoveAllImagesResult(0, null, null, true);
        }
    }

    public RemoveAllImagesResult removeAllDocumentImages(
            ProjectSession session,
            StoryboardDocument currentStoryboard,
            Optional<Path> projectFile) {
        DocuPodcastProject project = session.project();
        Set<String> theatreImageAssetIds = project.theatre().characterImages().stream()
                .map(TheatreProjectLayer.CharacterImage::assetId)
                .collect(Collectors.toSet());
        theatreImageAssetIds.addAll(project.theatre().intervencionesVisuales().stream()
                .map(TheatreProjectLayer.IntervencionVisual::assetId).toList());
        theatreImageAssetIds.addAll(project.theatre().objectImages().stream()
                .map(TheatreProjectLayer.ObjectImage::assetId).toList());
        List<ProjectAssetReference> imageAssets = project.assets().references().stream()
                .filter(ProjectAssetReference::isImage)
                .filter(image -> !theatreImageAssetIds.contains(image.id()))
                .toList();
        List<NarrativeLayerAssignment> keptLayers = project.narrativeLayerAssignments().stream()
                .filter(layer -> layer.kind() != NarrativeLayerKind.IMAGE
                        && layer.kind() != NarrativeLayerKind.BRIDGE_IMAGE)
                .toList();
        if (imageAssets.isEmpty() && keptLayers.size() == project.narrativeLayerAssignments().size()) {
            return RemoveAllImagesResult.empty();
        }
        for (ProjectAssetReference image : imageAssets) {
            deleteAssetFileIfPresent(image, projectFile);
            project = applicationServices.assets().removeProjectAsset().remove(project, image.id());
        }
        project = project.withNarrativeLayerAssignments(keptLayers);
        StoryboardDocument storyboard = currentStoryboard;
        if (storyboard != null && !storyboard.bindings().isEmpty()) {
            storyboard = new StoryboardDocument(storyboard.id(), storyboard.title(), storyboard.sourceScriptId(),
                    storyboard.bindings(), storyboard.layout(), storyboard.createdAt(), Instant.now(), storyboard.notes());
        }
        session.replaceProject(project, true);
        if (storyboard != null) session.setStoryboard(storyboard);
        return new RemoveAllImagesResult(imageAssets.size(), project, storyboard, false);
    }

    public record RemoveSingleImageResult(
            boolean removed,
            boolean assetDeleted,
            String message,
            DocuPodcastProject project,
            StoryboardDocument storyboard) {}

    public RemoveSingleImageResult removeImageAssignment(
            ProjectSession session,
            NarrativeLayerAssignment removable,
            StoryboardDocument currentStoryboard,
            Optional<Path> projectFile,
            String lastStoryboardImageAssetId) {
        String assetId = removable.targetId();
        DocuPodcastProject project = session.project().withoutNarrativeLayerAssignment(removable.id());
        StoryboardDocument storyboard = currentStoryboard;
        if (storyboard != null) storyboard = storyboard.withoutBindingForSegment(removable.textRange().segmentId());
        boolean stillUsed = NarrativeLayerCoordinator.projectImageAssetInUse(project, storyboard, assetId);
        boolean deleted = false;
        if (!stillUsed) {
            session.project().assets().byId(assetId).ifPresent(asset -> deleteAssetFileIfPresent(asset, projectFile));
            project = applicationServices.assets().removeProjectAsset().remove(project, assetId);
            deleted = true;
        }
        session.replaceProject(project, true);
        if (storyboard != null) session.setStoryboard(storyboard);
        String msg = stillUsed
                ? "Imagen quitada de este fragmento. El archivo sigue en el proyecto porque se usa en otro fragmento."
                : "Imagen quitada y eliminada del proyecto.";
        return new RemoveSingleImageResult(true, deleted, msg, project, storyboard);
    }

    public StoryboardDocument refreshStoryboardFromImageLayers(
            NarrationScriptDocument script,
            StoryboardDocument currentStoryboard,
            ProjectAssetCatalog assets,
            List<NarrativeLayerAssignment> narrativeLayerAssignments) {
        return applicationServices.storyboard().buildStoryboardFromImageLayers().build(
                script, currentStoryboard, assets, narrativeLayerAssignments);
    }

    public static void deleteAssetFileIfPresent(ProjectAssetReference asset, Optional<Path> projectFile) {
        if (projectFile == null || projectFile.isEmpty() || asset == null || asset.relativePath().isBlank()) return;
        try {
            Path projectRoot = projectFile.get().toAbsolutePath().normalize().getParent();
            Path target = projectRoot.resolve(asset.relativePath()).normalize();
            if (target.startsWith(projectRoot)) Files.deleteIfExists(target);
        } catch (IOException ignored) {}
    }
}
