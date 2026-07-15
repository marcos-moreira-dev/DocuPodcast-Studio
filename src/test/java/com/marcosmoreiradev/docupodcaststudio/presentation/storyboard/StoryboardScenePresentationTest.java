package com.marcosmoreiradev.docupodcaststudio.presentation.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardScene;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardScenePresentationTest {
    @Test
    void projectsImageAudioValidationAndPlaybackState() {
        StoryboardScene scene = new StoryboardScene(
                "SCN-001",
                "SEG-001",
                "Introducción",
                "Texto narrable de apertura",
                "IMG-001",
                "Plano de apertura",
                StoryboardDisplayMode.FIT_CONTAIN,
                true);
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(new ProjectAssetReference(
                "IMG-001",
                ProjectAssetKind.IMAGE,
                "apertura.png",
                "media/images/apertura.png",
                "image/png",
                "Imagen para storyboard",
                "",
                "")));
        PlaybackManifest manifest = new PlaybackManifest(
                "PLAYBACK-001",
                "JOB-001",
                List.of(new PlaybackCue("SEG-001", 0.0, 4.5, "CLIP-001", "audio/seg-001.wav", "IMG-001", "Introducción")),
                "",
                Instant.EPOCH);

        StoryboardScenePresentation projection = StoryboardScenePresentation.from(
                scene,
                assets,
                Optional.of(Path.of("/tmp/proyecto")),
                manifest,
                new PlaybackCursor("SEG-001", 1.0, false),
                "SEG-001",
                List.of());

        assertTrue(projection.imageReady());
        assertTrue(projection.audioReady());
        assertTrue(projection.validationOk());
        assertTrue(projection.playbackActive());
        assertEquals("media/images/apertura.png", projection.imageRelativePath());
        assertTrue(projection.imageFileUri().endsWith("/media/images/apertura.png"));
    }

    @Test
    void marksSceneIncompleteWhenImageIsMissing() {
        StoryboardScene scene = new StoryboardScene(
                "SCN-001",
                "SEG-001",
                "Introducción",
                "Texto narrable de apertura",
                "",
                "",
                StoryboardDisplayMode.FIT_CONTAIN,
                false);

        StoryboardScenePresentation projection = StoryboardScenePresentation.from(
                scene,
                ProjectAssetCatalog.empty(),
                Optional.empty(),
                PlaybackManifest.empty(),
                PlaybackCursor.stopped(),
                "",
                List.of());

        assertFalse(projection.imageReady());
        assertFalse(projection.audioReady());
        assertFalse(projection.validationOk());
        assertEquals("Imagen pendiente", projection.imageStatus());
        assertEquals("Falta imagen", projection.validationStatus());
    }
}
