package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadinessCommandsHf1SourceTest {
    @Test
    void exportCommandsRequireSaveableProjectAndExplainWhy() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));

        assertTrue(policy.contains("EXPORT_PROJECT_BUNDLE"));
        assertTrue(policy.contains("EXPORT_PODCAST_WAV"));
        assertTrue(policy.contains("EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertTrue(policy.contains("EXPORT_THEATRE_WORK"));
        assertTrue(policy.contains("EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(policy.contains("viewModel.saveableProjectOpenProperty().not()"));
        assertTrue(policy.contains("!viewModel.saveableProjectOpenProperty().get()"));
        assertTrue(policy.contains("Guarda el proyecto en una carpeta contenedora antes de exportar."));
    }
}
