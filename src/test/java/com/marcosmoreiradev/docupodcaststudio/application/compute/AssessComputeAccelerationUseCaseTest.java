package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssessComputeAccelerationUseCaseTest {
    @Test
    void nvidiaGpuIsCandidateButNotPromisedUntilRuntimeSmoke() {
        OperationalSettings settings = withCompute(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.PREFER_GPU,
                "gpu-nvidia-0",
                true,
                true,
                VideoEncoderPolicy.NVIDIA_NVENC
        ));
        ComputeEnvironmentReport environment = new ComputeEnvironmentReport(
                ComputeDevicePolicy.PREFER_GPU,
                "gpu-nvidia-0",
                List.of(ComputeDeviceDescriptor.cpu("CPU local"), ComputeDeviceDescriptor.gpu("gpu-nvidia-0", "GPU NVIDIA local", "NVIDIA")),
                List.of(),
                List.of("GPU detectada: NVIDIA")
        );

        ComputeAccelerationAssessment assessment = new AssessComputeAccelerationUseCase().assess(settings, environment);

        assertTrue(assessment.voiceGpuCandidate());
        assertTrue(assessment.videoGpuCandidate());
        assertFalse(assessment.gpuUseConfirmedByRuntime());
        assertTrue(assessment.gpuPromiseLabel().contains("falta prueba real"));
    }

    @Test
    void nonNvidiaGpuDoesNotPromiseAdvancedVoiceGpu() {
        OperationalSettings settings = withCompute(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.PREFER_GPU,
                "gpu-amd-0",
                true,
                true,
                VideoEncoderPolicy.AUTO
        ));
        ComputeEnvironmentReport environment = new ComputeEnvironmentReport(
                ComputeDevicePolicy.PREFER_GPU,
                "gpu-amd-0",
                List.of(ComputeDeviceDescriptor.cpu("CPU local"), ComputeDeviceDescriptor.gpu("gpu-amd-0", "GPU AMD local", "AMD")),
                List.of(),
                List.of("GPU detectada: AMD")
        );

        ComputeAccelerationAssessment assessment = new AssessComputeAccelerationUseCase().assess(settings, environment);

        assertFalse(assessment.voiceGpuCandidate());
        assertTrue(assessment.voiceExecutionMode().contains("CPU"));
        assertTrue(assessment.warnings().stream().anyMatch(w -> w.contains("no se declara compatible")));
    }

    @Test
    void specificNonNvidiaGpuIsManualIntentInsteadOfCpuFallback() {
        OperationalSettings settings = withCompute(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.SPECIFIC_DEVICE,
                "gpu-amd-0",
                true,
                true,
                VideoEncoderPolicy.AUTO
        ));
        ComputeEnvironmentReport environment = new ComputeEnvironmentReport(
                ComputeDevicePolicy.SPECIFIC_DEVICE,
                "gpu-amd-0",
                List.of(ComputeDeviceDescriptor.cpu("CPU local"), ComputeDeviceDescriptor.gpu("gpu-amd-0", "GPU AMD local", "AMD")),
                List.of(),
                List.of("GPU detectada: AMD")
        );

        ComputeAccelerationAssessment assessment = new AssessComputeAccelerationUseCase().assess(settings, environment);

        assertTrue(assessment.voiceGpuCandidate());
        assertTrue(assessment.voiceExecutionMode().contains("intentará el dispositivo solicitado"));
        assertTrue(assessment.warnings().stream().anyMatch(w -> w.contains("Modo manual")));
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
