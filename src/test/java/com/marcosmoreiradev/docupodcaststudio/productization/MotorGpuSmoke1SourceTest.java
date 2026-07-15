package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** MOTOR-GPU-SMOKE1: GPU is only usable for Voz IA avanzada after Python-local CUDA smoke. */
final class MotorGpuSmoke1SourceTest {
    @Test
    void gpuForAdvancedVoiceRequiresLocalPythonCudaSmoke() throws Exception {
        String runSmoke = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/RunXttsCudaSmokeUseCase.java"));
        String inspectSmoke = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectXttsCudaSmokeUseCase.java"));
        String probe = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/ProcessXttsCudaRuntimeProbeGateway.java"));
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(probe.contains("torch.cuda.is_available"));
        assertTrue(probe.contains("Files.createTempFile"));
        assertTrue(probe.contains("probeScript.toString()"));
        assertTrue(Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/XttsCudaSmokeReport.java")).contains("xtts-cuda-smoke.json"));
        assertTrue(inspectSmoke.contains("XttsCudaSmokeReport.MANIFEST_NAME"));
        assertTrue(runSmoke.contains("docupodcast-xtts-cuda-smoke-v1"));
        assertTrue(gateway.contains("cudaSmoke.gpuUsableForXtts()"));
        assertTrue(gateway.contains("manualTtsDeviceRequested"));
        assertTrue(gateway.contains("AUTO/PREFER_GPU"));
        assertTrue(settings.contains("Probar GPU para Voz IA avanzada"));
        assertTrue(runSmoke.contains("GPU detectada por Windows, pero no disponible para Voz IA avanzada"));
    }
}
