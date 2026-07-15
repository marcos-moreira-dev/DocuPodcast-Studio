package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RunXttsCudaSmokeUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void confirmsCudaOnlyWhenLocalPythonTorchReportsCudaAvailable() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/xtts-wrapper/.venv/Scripts"));
        Files.writeString(tempDir.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe"), "fake");
        OperationalSettings settings = withCompute(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.SPECIFIC_DEVICE,
                "gpu-nvidia-0",
                true,
                true,
                VideoEncoderPolicy.AUTO));
        ComputeEnvironmentReport environment = new ComputeEnvironmentReport(
                ComputeDevicePolicy.SPECIFIC_DEVICE,
                "gpu-nvidia-0",
                List.of(ComputeDeviceDescriptor.cpu("CPU"), ComputeDeviceDescriptor.gpu("gpu-nvidia-0", "NVIDIA", "NVIDIA")),
                List.of(),
                List.of());
        RunXttsCudaSmokeUseCase useCase = new RunXttsCudaSmokeUseCase((python, device, timeout) ->
                new XttsCudaRuntimeProbeResult(0,
                        "{\"torch_importable\": true, \"cuda_available\": true, \"torch_version\": \"2.1.0\", \"torch_cuda_version\": \"12.1\", \"device_name\": \"NVIDIA GTX\", \"device_count\": 1}",
                        "", false));

        XttsCudaSmokeReport report = useCase.run(settings, environment, tempDir);

        assertTrue(report.gpuUsableForXtts());
        assertTrue(Files.isRegularFile(tempDir.resolve("runtime/tts/xtts-smoke/xtts-cuda-smoke.json")));
        assertTrue(new InspectXttsCudaSmokeUseCase().inspect(tempDir).gpuUsableForXtts());
    }

    @Test
    void cpuOnlyPolicyDoesNotAttemptCuda() {
        OperationalSettings settings = withCompute(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.CPU_ONLY,
                "cpu",
                false,
                true,
                VideoEncoderPolicy.AUTO));
        ComputeEnvironmentReport environment = new ComputeEnvironmentReport(
                ComputeDevicePolicy.CPU_ONLY,
                "cpu",
                List.of(ComputeDeviceDescriptor.cpu("CPU"), ComputeDeviceDescriptor.gpu("gpu-nvidia-0", "NVIDIA", "NVIDIA")),
                List.of(),
                List.of());
        RunXttsCudaSmokeUseCase useCase = new RunXttsCudaSmokeUseCase((python, device, timeout) -> {
            throw new AssertionError("CUDA probe should not run when CPU-only is selected");
        });

        XttsCudaSmokeReport report = useCase.run(settings, environment, tempDir);

        assertFalse(report.attempted());
        assertFalse(report.gpuUsableForXtts());
        assertTrue(report.userMessage().contains("CPU"));
    }

    private static OperationalSettings withCompute(OperationalSettings.ComputeSettings compute) {
        OperationalSettings defaults = OperationalSettings.defaults();
        return new OperationalSettings(
                defaults.readingDocument(),
                defaults.playbackBuffer(),
                defaults.tts(),
                defaults.video(),
                compute,
                defaults.storage(),
                defaults.diagnostics()
        );
    }
}
