package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReconcileGeneratedAudioJobAssetsUseCaseTest {
    @TempDir
    Path tempDir;

    private final ReconcileGeneratedAudioJobAssetsUseCase useCase =
            new ReconcileGeneratedAudioJobAssetsUseCase();

    @Test
    void removeMissingPrunesOnlyAbsentGeneratedJobAssetsAndTheirLayers() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("jobs/JOB-CURRENT/final"));
        Files.writeString(tempDir.resolve("jobs/JOB-CURRENT/final/podcast.wav"), "wav");

        DocuPodcastProject project = DocuPodcastProject.createNew("Demo")
                .withAsset(asset("AUD-FINAL-OLD", ProjectAssetKind.AUDIO_FINAL,
                        "jobs/JOB-OLD/final/podcast.wav"))
                .withAsset(asset("AUD-MANIFEST-OLD", ProjectAssetKind.AUDIO_MANIFEST,
                        "jobs/JOB-OLD/audio-manifest.json"))
                .withAsset(asset("AUD-FINAL-CURRENT", ProjectAssetKind.AUDIO_FINAL,
                        "jobs/JOB-CURRENT/final/podcast.wav"))
                .withAsset(asset("AUDIO-USER", ProjectAssetKind.AUDIO_CLIP,
                        "media/audio/interview.wav"))
                .withNarrativeLayerAssignment(new NarrativeLayerAssignment(
                        "LAY-OLD", NarrativeLayerKind.HUMAN_AUDIO,
                        new ScriptTextRange("SEG-001", 0, 4),
                        "AUD-FINAL-OLD", "Audio anterior", ""));

        ReconcileGeneratedAudioJobAssetsUseCase.Result result =
                useCase.removeMissing(project, projectFile);

        assertEquals(2, result.removedAssetIds().size());
        assertEquals(1, result.removedLayerAssignmentIds().size());
        assertTrue(result.project().assets().byId("AUD-FINAL-OLD").isEmpty());
        assertTrue(result.project().assets().byId("AUD-MANIFEST-OLD").isEmpty());
        assertTrue(result.project().assets().byId("AUD-FINAL-CURRENT").isPresent());
        assertTrue(result.project().assets().byId("AUDIO-USER").isPresent());
        assertTrue(result.project().narrativeLayerAssignments().isEmpty());
    }

    @Test
    void removeAllPreservesImportedAudioEvenWhenItIsAnAudioClip() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Demo")
                .withAsset(asset("AUD-FINAL-JOB", ProjectAssetKind.AUDIO_FINAL,
                        "jobs/JOB-001/final/podcast.wav"))
                .withAsset(asset("AUD-MANIFEST-JOB", ProjectAssetKind.AUDIO_MANIFEST,
                        "jobs/JOB-001/audio-manifest.json"))
                .withAsset(asset("AUDIO-USER", ProjectAssetKind.AUDIO_CLIP,
                        "media/audio/interview.wav"));

        ReconcileGeneratedAudioJobAssetsUseCase.Result result = useCase.removeAll(project);

        assertTrue(result.changed());
        assertEquals(2, result.removedAssetIds().size());
        assertFalse(result.project().assets().byId("AUDIO-USER").isEmpty());
        assertEquals(1, result.project().assets().size());
    }

    @Test
    void removingLastGeneratedAudioReturnsProjectToScriptReady() {
        DocuPodcastProject base = DocuPodcastProject.createNew("Demo");
        DocuPodcastProject project = base
                .withMetadata(base.metadata()
                        .withKind(ProjectKind.AUDIO_PROJECT)
                        .withStatus(ProjectStatus.AUDIO_READY))
                .withAsset(asset("SCRIPT", ProjectAssetKind.NARRATION_SCRIPT,
                        "script/narration-script.json"))
                .withAsset(asset("AUD-FINAL-JOB", ProjectAssetKind.AUDIO_FINAL,
                        "jobs/JOB-001/final/podcast.wav"));

        ReconcileGeneratedAudioJobAssetsUseCase.Result result = useCase.removeAll(project);

        assertEquals(ProjectKind.NARRATION_SCRIPT, result.project().metadata().kind());
        assertEquals(ProjectStatus.SCRIPT_READY, result.project().metadata().status());
    }

    private static ProjectAssetReference asset(
            String id, ProjectAssetKind kind, String path) {
        return new ProjectAssetReference(
                id, kind, id, path, "application/octet-stream",
                "Prueba", "", "");
    }
}
