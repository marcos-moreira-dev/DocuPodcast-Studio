package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectComputeEnvironmentUseCaseTest {
    @Test
    void detectsCpuAndGpuSignalsWithoutBenchmarks() {
        OperationalSettings settings = new OperationalSettings(
                null, null, null, null,
                new OperationalSettings.ComputeSettings("PREFER_GPU", "", true, true, "NVIDIA_NVENC"),
                null, null);
        Properties props = new Properties();
        props.setProperty("os.name", "TestOS");
        props.setProperty("os.arch", "x64");

        ComputeEnvironmentReport report = new InspectComputeEnvironmentUseCase()
                .inspect(settings, props, Map.of("CUDA_VISIBLE_DEVICES", "0"), 8);

        assertTrue(report.gpuDetected());
        assertTrue(report.summary().contains("CPU/GPU detectadas"));
        assertTrue(report.toMarkdown().contains("PREFER_GPU"));
        assertTrue(report.toMarkdown().contains("Dispositivo solicitado al proceso de voz"));
    }

    @Test
    void warnsWhenSpecificDeviceIsNotConfigured() {
        OperationalSettings settings = new OperationalSettings(
                null, null, null, null,
                new OperationalSettings.ComputeSettings("SPECIFIC_DEVICE", "", true, true, "AUTO"),
                null, null);

        ComputeEnvironmentReport report = new InspectComputeEnvironmentUseCase()
                .inspect(settings, new Properties(), Map.of(), 2);

        assertFalse(report.warnings().isEmpty());
        assertEquals(ComputeDevicePolicy.SPECIFIC_DEVICE, report.policy());
    }
}
