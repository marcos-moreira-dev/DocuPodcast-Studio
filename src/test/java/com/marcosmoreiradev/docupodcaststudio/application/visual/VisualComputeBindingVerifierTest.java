package com.marcosmoreiradev.docupodcaststudio.application.visual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualComputeBindingVerifierTest {
    private final VisualComputeBindingVerifier verifier = new VisualComputeBindingVerifier();

    @Test
    void acceptsExactBackendIndexAndModel() {
        VisualComputeBinding expected = new VisualComputeBinding(
                "gpu-nvidia-1", "NVIDIA GeForce GTX 1650",
                VisualComputeBackend.CUDA, 1, List.of("--cuda-device", "1"));
        ComfyUiSystemStats stats = new ComfyUiSystemStats(
                "windows", "3.11", "2.5.1+cu121",
                List.of(new ComfyUiSystemStats.Device(
                        "cuda:1 NVIDIA GeForce GTX 1650", "cuda", 1)), "{}");

        assertTrue(verifier.verify(expected, stats).matches());
    }

    @Test
    void rejectsDifferentIndexBackendOrGpuModel() {
        VisualComputeBinding expected = new VisualComputeBinding(
                "gpu-nvidia-1", "NVIDIA GeForce GTX 1650",
                VisualComputeBackend.CUDA, 1, List.of("--cuda-device", "1"));

        assertFalse(verifier.verify(expected, stats("NVIDIA GeForce GTX 1650", "cuda", 0)).matches());
        assertFalse(verifier.verify(expected, stats("NVIDIA GeForce GTX 1650", "directml", 1)).matches());
        assertFalse(verifier.verify(expected, stats("NVIDIA GeForce RTX 4090", "cuda", 1)).matches());
    }

    @Test
    void rejectsUnverifiableEndpoint() {
        VisualComputeBinding expected = new VisualComputeBinding(
                "cpu", "CPU local", VisualComputeBackend.CPU, -1, List.of("--cpu"));

        assertFalse(verifier.verify(expected,
                new ComfyUiSystemStats("", "", "", List.of(), "{}")).matches());
    }

    private static ComfyUiSystemStats stats(String name, String type, int index) {
        return new ComfyUiSystemStats(
                "windows", "3.11", "2.5.1",
                List.of(new ComfyUiSystemStats.Device(name, type, index)), "{}");
    }
}
