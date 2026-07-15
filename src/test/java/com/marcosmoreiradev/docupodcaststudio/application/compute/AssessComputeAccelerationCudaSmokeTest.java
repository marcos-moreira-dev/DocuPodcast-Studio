package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssessComputeAccelerationCudaSmokeTest {
    @Test
    void confirmedCudaSmokeTurnsGpuCandidateIntoConfirmedRuntimeEvidence() {
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
        XttsCudaSmokeReport cuda = new XttsCudaSmokeReport(
                Path.of("runtime/tts/xtts-smoke"),
                Path.of("runtime/tts/xtts-smoke/xtts-cuda-smoke.json"),
                Instant.now(),
                true,
                true,
                true,
                true,
                "2.1.0",
                "12.1",
                "gpu-nvidia-0",
                "cuda:0",
                "NVIDIA GTX",
                1,
                List.of(),
                "GPU confirmada");

        ComputeAccelerationAssessment assessment = new AssessComputeAccelerationUseCase().assess(settings, environment, cuda);

        assertTrue(assessment.gpuUseConfirmedByRuntime());
        assertTrue(assessment.gpuPromiseLabel().contains("confirmada"));
        assertTrue(assessment.voiceExecutionMode().contains("GPU confirmada"));
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
