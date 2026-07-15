package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioWorkspaceSourceTest {
    @Test
    void audioWorkspaceRendersQueueStateProgressEtaAndCancellation() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java"));

        assertTrue(source.contains("AudioQueueState"));
        assertTrue(source.contains("AudioJobRow"));
        assertTrue(source.contains("ProgressBar"));
        assertTrue(source.contains("ETA"));
        assertTrue(source.contains("Generar audio"));
        assertTrue(source.contains("Cancelar job activo"));
        assertTrue(source.contains("Continuar último reanudable"));
        assertTrue(source.contains("Actualizar cola"));
    }

    @Test
    void renderStatusShouldNotReadPersistedJobsOnEveryProgressEvent() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java"));
        int renderStart = source.indexOf("private void renderStatus");
        int queueStart = source.indexOf("private void refreshQueueState");
        String renderStatus = source.substring(renderStart, queueStart);

        assertFalse(renderStatus.contains("audioQueueState()"));
        assertFalse(renderStatus.contains("listPersistedAudioJobs"));
    }
}
