package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MOTOR-PERF1: detected GPU is not a product promise until runtime proof exists. */
final class MotorPerf1HonestGpuSourceTest {
    @Test
    void computeAssessmentSeparatesDetectedGpuFromConfirmedGpu() throws Exception {
        String assessment = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/ComputeAccelerationAssessment.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/AssessComputeAccelerationUseCase.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(assessment.contains("gpuUseConfirmedByRuntime"));
        assertTrue(assessment.contains("GPU candidata; falta prueba real"));
        assertTrue(useCase.contains("falta prueba real del motor"));
        assertTrue(useCase.contains("prueba CUDA real"));
        assertTrue(settings.contains("Dispositivo específico"));
        assertTrue(settings.contains("se intentará el dispositivo solicitado"));
        assertFalse(settings.contains("GPU lista"));
    }
}
