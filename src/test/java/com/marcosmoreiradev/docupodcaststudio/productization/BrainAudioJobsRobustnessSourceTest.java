package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrainAudioJobsRobustnessSourceTest {
    @Test
    void audioJobsHaveBrainLevelMaintenanceBeforeUiPolish() throws IOException {
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/InspectAudioJobMaintenanceUseCase.java", "sourceReport");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/InspectAudioJobMaintenanceUseCase.java", "MISSING_AUDIO");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/InspectAudioJobMaintenanceUseCase.java", "STALE_SOURCE");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobMaintenanceReport.java", "canReuseAudio");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/AudioApplicationServices.java", "inspectAudioJobMaintenance");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java", "maintenanceReport");

        String doc = read("docs/productizacion/AUDIO_JOBS_ROBUSTOS_T73.md");
        assertTrue(doc.contains("reutilizar audio existente"));
        assertTrue(doc.contains("audio obsoleto"));
        assertTrue(doc.contains("reanudación"));
    }

    private static void assertFileContains(String path, String expected) throws IOException {
        assertTrue(read(path).contains(expected), path + " debe contener: " + expected);
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
