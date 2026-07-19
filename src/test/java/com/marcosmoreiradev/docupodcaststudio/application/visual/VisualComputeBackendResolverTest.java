package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualComputeBackendResolverTest {
    private final VisualComputeBackendResolver resolver = new VisualComputeBackendResolver();

    @Test
    void resolvesExactCudaDeviceAndIndex() {
        VisualComputeBinding binding = resolver.resolve(
                settings("gpu-nvidia-1"),
                List.of(ComputeDeviceDescriptor.cpu("CPU"),
                        ComputeDeviceDescriptor.gpu("gpu-nvidia-1", "NVIDIA GeForce GTX 1650", "NVIDIA")),
                capabilities("--cuda-device", "--cpu"));

        assertEquals(VisualComputeBackend.CUDA, binding.backend());
        assertEquals(1, binding.deviceIndex());
        assertEquals(List.of("--cuda-device", "1"), binding.launchArguments());
    }

    @Test
    void prefersXpuForIntelOnlyWhenRuntimeSupportsIt() {
        VisualComputeBinding binding = resolver.resolve(
                settings("gpu-intel-0"),
                List.of(ComputeDeviceDescriptor.gpu("gpu-intel-0", "Intel Arc A770", "Intel")),
                capabilities("--oneapi-device-selector", "--directml"));

        assertEquals(VisualComputeBackend.XPU, binding.backend());
        assertEquals(List.of("--oneapi-device-selector", "level_zero:0"), binding.launchArguments());
    }

    @Test
    void usesDirectMlForIntelOrAmdWithExactIndex() {
        VisualComputeBinding intel = resolver.resolve(
                settings("gpu-intel-2"),
                List.of(ComputeDeviceDescriptor.gpu("gpu-intel-2", "Intel UHD Graphics 630", "Intel")),
                capabilities("--directml"));
        VisualComputeBinding amd = resolver.resolve(
                settings("gpu-amd-3"),
                List.of(ComputeDeviceDescriptor.gpu("gpu-amd-3", "AMD Radeon RX 6600", "AMD")),
                capabilities("--directml"));

        assertEquals(List.of("--directml", "2"), intel.launchArguments());
        assertEquals(List.of("--directml", "3"), amd.launchArguments());
    }

    @Test
    void resolvesCpuOnlyWithoutGpuFallback() {
        OperationalSettings.ComputeSettings cpuOnly = new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.CPU_ONLY, "", true, true, VideoEncoderPolicy.AUTO);

        VisualComputeBinding binding = resolver.resolve(
                cpuOnly,
                List.of(ComputeDeviceDescriptor.cpu("CPU local")),
                capabilities("--cpu"));

        assertEquals(VisualComputeBackend.CPU, binding.backend());
        assertEquals(List.of("--cpu"), binding.launchArguments());
    }

    @Test
    void rejectsAutoMissingDeviceAndUnsupportedBackend() {
        IllegalStateException automatic = assertThrows(IllegalStateException.class,
                () -> resolver.resolve(settings("auto"), List.of(ComputeDeviceDescriptor.cpu("CPU")),
                        capabilities("--cpu")));
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> resolver.resolve(settings("gpu-nvidia-0"), List.of(ComputeDeviceDescriptor.cpu("CPU")),
                        capabilities("--cuda-device")));
        IllegalStateException unsupported = assertThrows(IllegalStateException.class,
                () -> resolver.resolve(settings("gpu-amd-0"),
                        List.of(ComputeDeviceDescriptor.gpu("gpu-amd-0", "AMD Radeon", "AMD")),
                        capabilities("--cpu")));

        assertTrue(automatic.getMessage().contains("no usa fallback automatico"));
        assertTrue(missing.getMessage().contains("no esta disponible"));
        assertTrue(unsupported.getMessage().contains("--directml"));
    }

    private static OperationalSettings.ComputeSettings settings(String id) {
        return new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.SPECIFIC_DEVICE, id, true, true, VideoEncoderPolicy.AUTO);
    }

    private static ComfyUiRuntimeCapabilities capabilities(String... flags) {
        return new ComfyUiRuntimeCapabilities(Set.of(flags), true, "test");
    }
}
