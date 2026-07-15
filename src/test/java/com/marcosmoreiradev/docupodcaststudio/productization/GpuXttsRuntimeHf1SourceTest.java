package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GPU-XTTS-RUNTIME-HF1: automatic GPU->CPU fallback for XTTS must be visible and backed by CUDA evidence. */
final class GpuXttsRuntimeHf1SourceTest {
    @Test
    void gpuFallbackDecisionIncludesRuntimeEvidenceAndRequiresDialog() throws IOException {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectXttsGpuFallbackDecisionUseCase.java");
        assertTrue(useCase.contains("UserVisibleDecision.defensiveFallback"));
        assertTrue(useCase.contains("Voz IA avanzada usará CPU en modo automático"));
        assertTrue(useCase.contains("torchCudaVersion"));
        assertTrue(useCase.contains("deviceArgument"));
        assertTrue(useCase.contains("selectedDeviceId"));
        assertTrue(useCase.contains("PREFER_GPU"));
        assertFalse(useCase.contains("compute.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE"));
    }

    @Test
    void shellShowsDialogBeforeContinuingWithDefensiveAudioFallback() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        assertTrue(shell.contains("showDocumentAudioDefensiveDecisions"));
        assertTrue(shell.contains("DocumentAudioDefensiveDecisionGuard"));
        assertTrue(shell.contains("alertPresenter.showDialogDecisions"));
        assertTrue(shell.contains("lastAudioDefensiveDecisionKey"));
        assertTrue(shell.contains("showAudioGenerationFailureIfNeeded"));
        assertTrue(shell.contains("lastAudioFailureKey"));
        assertTrue(shell.contains("backend GPU"));
        assertFalse(shell.contains("showDocumentAudioDefensiveDecisions();\n            return false"));
    }

    @Test
    void guardKeepsJavaFxOutOfApplicationFallbackDecision() throws IOException {
        String guard = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentAudioDefensiveDecisionGuard.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectXttsGpuFallbackDecisionUseCase.java");
        assertTrue(guard.contains("decisionsBeforeDocumentGeneration"));
        assertTrue(guard.contains("inspectXttsCudaSmoke"));
        assertFalse(useCase.contains("javafx"));
        assertFalse(useCase.contains("Alert"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
