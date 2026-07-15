package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class ComputeDeviceBrainContractSourceTest {
    @Test
    void computeDeviceContractIsExplicitBeforeBrainFreeze() throws Exception {
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java");
        String compute = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectComputeEnvironmentUseCase.java");
        String environmentDiscovery = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/EnvironmentComputeDeviceDiscoveryGateway.java");
        String windowsDiscovery = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/compute/WindowsComputeDeviceDiscoveryGateway.java");
        String tts = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java");
        String render = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/VideoRenderCommandPlan.java");
        String docs = read("docs/productizacion/COMPUTE_DEVICE_CONTRACT_T80A.md");

        assertTrue(settings.contains("ComputeSettings"));
        assertTrue(settings.contains("allowGpuForTts"));
        assertFalse(settings.contains("allowGpuForStt"));
        assertTrue(settings.contains("allowGpuForVideo"));
        assertTrue(compute.contains("ComputeEnvironmentReport"));
        assertTrue(compute.contains("ComputeDeviceDiscoveryGateway"));
        assertTrue(environmentDiscovery.contains("CUDA_VISIBLE_DEVICES"));
        assertTrue(windowsDiscovery.contains("Win32_VideoController"));
        assertTrue(tts.contains("{computePolicy}"));
        assertTrue(tts.contains("{computeDevice}"));
        assertTrue(tts.contains("{gpuIndex}"));
        assertTrue(render.contains("renderDevicePolicy"));
        assertTrue(render.contains("requestedEncoder"));
        assertTrue(render.contains("hardwareAccelerationReady"));
        assertTrue(docs.contains("AUTO"));
        assertTrue(docs.contains("CPU_ONLY"));
        assertTrue(docs.contains("PREFER_GPU"));
        assertTrue(docs.contains("NVIDIA_NVENC"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
