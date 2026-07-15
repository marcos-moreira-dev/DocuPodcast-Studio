package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioClipPlaybackExportT92SourceTest {
    @Test
    void t92MakesUserAudioClipsEffectiveForPlaybackAndExport() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String playback = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/BuildPlaybackManifestUseCase.java"));
        String export = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportPodcastWavUseCase.java"));
        String workflow = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));
        String shell = Files.readString(root.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String doc = Files.readString(root.resolve("docs/productizacion/T92_AUDIO_CLIP_PLAYBACK_EXPORT.md"));

        assertTrue(playback.contains("NarrationRenderPlan"));
        assertTrue(playback.contains("NarrationRenderUnit"));
        assertTrue(playback.contains("ProjectAssetReference::isAudio"));
        assertTrue(playback.contains("unit.usesAudioClip()"));
        assertTrue(export.contains("exportPlaybackManifest"));
        assertTrue(workflow.contains("exportPlaybackManifest"));
        assertTrue(workflow.contains("exportPodcastWav"));
        assertTrue(shell.contains("exportWorkflow.exportPodcastWav"));
        assertTrue(doc.contains("Audio del computador"));
        assertTrue(doc.contains("clip genérico"));
    }
}
