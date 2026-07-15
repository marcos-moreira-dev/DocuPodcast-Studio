package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreCameraAndBackdropResolverTest {
    @TempDir Path projectDirectory;

    @Test
    void cameraCuesInheritForwardFromDefaultAndBlockInterpolationOnCameraChanges() throws Exception {
        createFile("media/theatre/cameras/default.png");
        createFile("media/theatre/cameras/right.png");
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(asset("IMG-CAMERA-DEFAULT", "media/theatre/cameras/default.png"))
                .withAsset(asset("IMG-CAMERA-RIGHT", "media/theatre/cameras/right.png"))
                .withTheatre(theatre(List.of(new TheatreProjectLayer.CameraCue(
                        "INTERVENCION-3", "CERCA_DERECHA_NIVEL", "Cambio"))));

        TheatreCameraReferenceResolver cameraResolver = new TheatreCameraReferenceResolver();
        TheatreVisualContinuityResolver continuityResolver = new TheatreVisualContinuityResolver();

        assertEquals(TheatreProjectLayer.DEFAULT_CAMERA_ID,
                cameraResolver.effectiveCameraId(project.theatre(), "INTERVENCION-1"));
        assertEquals(TheatreProjectLayer.DEFAULT_CAMERA_ID,
                cameraResolver.effectiveCameraId(project.theatre(), "INTERVENCION-2"));
        assertEquals("CERCA_DERECHA_NIVEL",
                cameraResolver.effectiveCameraId(project.theatre(), "INTERVENCION-3"));
        assertEquals("CERCA_DERECHA_NIVEL",
                cameraResolver.effectiveCameraId(project.theatre(), "INTERVENCION-4"));
        assertTrue(cameraResolver.resolve(project, "INTERVENCION-1", projectDirectory).isPresent());
        assertFalse(continuityResolver.canInterpolate(
                project.theatre(), "INTERVENCION-2", "INTERVENCION-3"));
        assertTrue(continuityResolver.canInterpolate(
                project.theatre(), "INTERVENCION-3", "INTERVENCION-4"));
    }

    @Test
    void cameraResolverFallsBackToBundledCatalogWhenProjectHasNoCameraAssets() {
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withTheatre(theatreWithoutCameraReferences());

        TheatreCameraReferenceResolver resolver = new TheatreCameraReferenceResolver();

        assertEquals(TheatreProjectLayer.DEFAULT_CAMERA_ID,
                resolver.effectiveCameraId(project.theatre(), "INTERVENCION-1"));
        assertTrue(resolver.resolve(project, "INTERVENCION-1", projectDirectory).isPresent());
    }

    @Test
    void backdropResolverUsesFragmentOverrideBeforeSceneAssignment() throws Exception {
        createFile("media/backdrops/scene.png");
        createFile("media/backdrops/fragment.png");
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(asset("IMG-SCENE", "media/backdrops/scene.png"))
                .withAsset(asset("IMG-FRAGMENT", "media/backdrops/fragment.png"))
                .withTheatre(theatreWithBackdrops());

        TheatreStageBackdropResolver resolver = new TheatreStageBackdropResolver();

        assertEquals("BACKDROP-SCENE",
                resolver.resolve(project, "SCN-1", "INTERVENCION-1", projectDirectory)
                        .orElseThrow().backdrop().id());
        assertEquals("BACKDROP-FRAGMENT",
                resolver.resolve(project, "SCN-1", "INTERVENCION-2", projectDirectory)
                        .orElseThrow().backdrop().id());
    }

    @Test
    void backdropInterventionCuesInheritForwardAndNoneCueCutsSceneFallback() throws Exception {
        createFile("media/backdrops/scene.png");
        createFile("media/backdrops/fragment.png");
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(asset("IMG-SCENE", "media/backdrops/scene.png"))
                .withAsset(asset("IMG-FRAGMENT", "media/backdrops/fragment.png"))
                .withTheatre(theatreWithInheritedBackdrops());

        TheatreStageBackdropResolver resolver = new TheatreStageBackdropResolver();

        assertEquals("BACKDROP-SCENE",
                resolver.resolve(project, "SCN-1", "INTERVENCION-1", projectDirectory)
                        .orElseThrow().backdrop().id());
        assertEquals("BACKDROP-FRAGMENT",
                resolver.resolve(project, "SCN-1", "INTERVENCION-2", projectDirectory)
                        .orElseThrow().backdrop().id());
        assertEquals("BACKDROP-FRAGMENT",
                resolver.resolve(project, "SCN-1", "INTERVENCION-3", projectDirectory)
                        .orElseThrow().backdrop().id());
        assertTrue(resolver.resolve(project, "SCN-1", "INTERVENCION-4", projectDirectory).isEmpty());
    }

    private TheatreProjectLayer theatre(List<TheatreProjectLayer.CameraCue> cues) {
        return new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002"),
                        TheatreProjectLayer.Intervencion.ofSequence(3, "B0003"),
                        TheatreProjectLayer.Intervencion.ofSequence(4, "B0004")),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(
                        new TheatreProjectLayer.CameraReference(
                                TheatreProjectLayer.DEFAULT_CAMERA_ID, "CERCA CENTRO NIVEL",
                                "IMG-CAMERA-DEFAULT", "CERCA", "CENTRO", "NIVEL", true, ""),
                        new TheatreProjectLayer.CameraReference(
                                "CERCA_DERECHA_NIVEL", "CERCA DERECHA NIVEL",
                                "IMG-CAMERA-RIGHT", "CERCA", "DERECHA", "NIVEL", false, "")),
                cues,
                List.of(),
                List.of());
    }

    private TheatreProjectLayer theatreWithBackdrops() {
        return new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002")),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                List.of(),
                List.of(
                        new TheatreProjectLayer.StageBackdrop("BACKDROP-SCENE", "Escena", "IMG-SCENE", ""),
                        new TheatreProjectLayer.StageBackdrop("BACKDROP-FRAGMENT", "Fragmento", "IMG-FRAGMENT", "")),
                List.of(
                        new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE, "SCN-1", "BACKDROP-SCENE", ""),
                        new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION,
                                "INTERVENCION-2", "BACKDROP-FRAGMENT", "")));
    }

    private TheatreProjectLayer theatreWithInheritedBackdrops() {
        return new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002"),
                        TheatreProjectLayer.Intervencion.ofSequence(3, "B0003"),
                        TheatreProjectLayer.Intervencion.ofSequence(4, "B0004")),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                List.of(),
                List.of(
                        new TheatreProjectLayer.StageBackdrop("BACKDROP-SCENE", "Escena", "IMG-SCENE", ""),
                        new TheatreProjectLayer.StageBackdrop("BACKDROP-FRAGMENT", "Fragmento", "IMG-FRAGMENT", "")),
                List.of(
                        new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE, "SCN-1", "BACKDROP-SCENE", ""),
                        new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION,
                                "INTERVENCION-2", "BACKDROP-FRAGMENT", ""),
                        new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION,
                                "INTERVENCION-4", TheatreProjectLayer.STAGE_BACKDROP_NONE, "")));
    }

    private TheatreProjectLayer theatreWithoutCameraReferences() {
        return new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B0001")),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    private static ProjectAssetReference asset(String id, String relativePath) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, id, relativePath, "image/png", "", "", "");
    }

    private void createFile(String relativePath) throws Exception {
        Path file = projectDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "png");
    }
}
