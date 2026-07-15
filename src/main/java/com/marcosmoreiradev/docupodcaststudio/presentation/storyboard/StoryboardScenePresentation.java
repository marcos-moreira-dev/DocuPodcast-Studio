package com.marcosmoreiradev.docupodcaststudio.presentation.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardScene;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardValidationIssue;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Product-oriented projection for one storyboard scene.
 *
 * <p>The visual storyboard needs to show scene readiness, not only raw ids. This projection keeps
 * image resolution, playback state, audio cue status and validation rules outside JavaFX so the
 * scene board remains testable and the view does not duplicate product logic.</p>
 */
public record StoryboardScenePresentation(
        String sceneId,
        String segmentId,
        String title,
        String narrationPreview,
        String caption,
        String displayModeLabel,
        boolean imageReady,
        String imageStatus,
        String imageAssetId,
        String imageDisplayName,
        String imageRelativePath,
        String imageFileUri,
        boolean audioReady,
        String audioStatus,
        String audioRelativePath,
        boolean validationOk,
        String validationStatus,
        boolean selected,
        boolean playbackActive,
        boolean playbackPaused
) {
    public StoryboardScenePresentation {
        sceneId = normalize(sceneId);
        segmentId = normalize(segmentId);
        title = normalize(title);
        narrationPreview = normalize(narrationPreview);
        caption = normalize(caption);
        displayModeLabel = normalize(displayModeLabel);
        imageStatus = normalize(imageStatus);
        imageAssetId = normalize(imageAssetId);
        imageDisplayName = normalize(imageDisplayName);
        imageRelativePath = normalize(imageRelativePath);
        imageFileUri = normalize(imageFileUri);
        audioStatus = normalize(audioStatus);
        audioRelativePath = normalize(audioRelativePath);
        validationStatus = normalize(validationStatus);
    }

    public static StoryboardScenePresentation from(
            StoryboardScene scene,
            ProjectAssetCatalog assets,
            Optional<Path> projectDirectory,
            PlaybackManifest playbackManifest,
            PlaybackCursor cursor,
            String selectedSegmentId,
            List<StoryboardValidationIssue> issues
    ) {
        Objects.requireNonNull(scene, "scene");
        ProjectAssetCatalog catalog = assets == null ? ProjectAssetCatalog.empty() : assets;
        List<StoryboardValidationIssue> safeIssues = issues == null ? List.of() : issues;
        Optional<ProjectAssetReference> imageAsset = scene.hasImage()
                ? catalog.byId(scene.imageAssetId()).filter(asset -> asset.kind() == ProjectAssetKind.IMAGE || asset.kind() == ProjectAssetKind.THUMBNAIL)
                : Optional.empty();

        boolean imageReady = scene.hasImage() && imageAsset.isPresent();
        String imageStatus = imageStatus(scene, imageReady);
        String imageDisplayName = imageAsset.map(ProjectAssetReference::displayName).orElse("");
        String imageRelativePath = imageAsset.map(ProjectAssetReference::relativePath).orElse("");
        String imageFileUri = imageAsset.flatMap(asset -> imageUri(projectDirectory, asset)).orElse("");

        Optional<com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue> cue = playbackManifest == null || playbackManifest.emptyManifest()
                ? Optional.empty()
                : playbackManifest.cueForSegment(scene.segmentId());
        boolean audioReady = cue.map(com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue::hasAudio).orElse(false);
        String audioStatus = cue
                .map(value -> audioReady
                        ? "Audio %.0fs–%.0fs".formatted(value.startSeconds(), value.endSeconds())
                        : "Cue sin audio")
                .orElse(playbackManifest == null || playbackManifest.emptyManifest() ? "Sin manifest de audio" : "Audio pendiente");
        String audioRelativePath = cue.map(com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue::audioRelativePath).orElse("");

        List<StoryboardValidationIssue> sceneIssues = safeIssues.stream()
                .filter(issue -> issue.referenceId().equals(scene.segmentId())
                        || issue.referenceId().equals(scene.imageAssetId())
                        || issue.referenceId().equals(scene.id()))
                .toList();
        boolean hasError = sceneIssues.stream().anyMatch(issue -> issue.level().equals("ERROR"));
        boolean hasWarning = sceneIssues.stream().anyMatch(issue -> issue.level().equals("WARNING"));
        boolean validationOk = imageReady && !hasError && !hasWarning;
        String validationStatus = validationStatus(scene, imageReady, hasError, hasWarning, sceneIssues);

        boolean selected = scene.segmentId().equals(normalize(selectedSegmentId));
        boolean sameCursorSegment = cursor != null && scene.segmentId().equals(cursor.segmentId());
        boolean playbackActive = sameCursorSegment && !cursor.paused();
        boolean playbackPaused = sameCursorSegment && cursor.paused();

        return new StoryboardScenePresentation(
                scene.id(),
                scene.segmentId(),
                scene.title(),
                scene.narrationPreview(),
                scene.caption().isBlank() ? "Caption pendiente" : scene.caption(),
                scene.displayMode().displayName(),
                imageReady,
                imageStatus,
                scene.imageAssetId(),
                imageDisplayName,
                imageRelativePath,
                imageFileUri,
                audioReady,
                audioStatus,
                audioRelativePath,
                validationOk,
                validationStatus,
                selected,
                playbackActive,
                playbackPaused
        );
    }

    public String imageCssClass() {
        return imageReady ? "storyboard-chip-ok" : "storyboard-chip-warning";
    }

    public String audioCssClass() {
        return audioReady ? "storyboard-chip-ok" : "storyboard-chip-pending";
    }

    public String validationCssClass() {
        return validationOk ? "storyboard-chip-ok" : "storyboard-chip-warning";
    }

    public String cardStateCssClass() {
        if (playbackActive) {
            return "storyboard-scene-active";
        }
        if (playbackPaused) {
            return "storyboard-scene-paused";
        }
        if (selected) {
            return "storyboard-scene-selected";
        }
        return imageReady ? "storyboard-scene-ready" : "storyboard-scene-incomplete";
    }

    private static String imageStatus(StoryboardScene scene, boolean imageReady) {
        if (!scene.hasImage()) {
            return "Imagen pendiente";
        }
        if (!imageReady) {
            return "Imagen faltante en assets";
        }
        return "Imagen asociada";
    }

    private static String validationStatus(StoryboardScene scene,
                                           boolean imageReady,
                                           boolean hasError,
                                           boolean hasWarning,
                                           List<StoryboardValidationIssue> issues) {
        if (hasError) {
            long count = issues.stream().filter(issue -> issue.level().equals("ERROR")).count();
            return count + " errores";
        }
        if (hasWarning) {
            long count = issues.stream().filter(issue -> issue.level().equals("WARNING")).count();
            return count + " advertencias";
        }
        if (!scene.hasImage()) {
            return "Falta imagen";
        }
        if (!imageReady) {
            return "Asset no disponible";
        }
        return "Escena lista";
    }

    private static Optional<String> imageUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty()) {
            return Optional.empty();
        }
        Path resolved = projectDirectory.get().resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(projectDirectory.get().normalize())) {
            return Optional.empty();
        }
        return Optional.of(resolved.toUri().toString());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
