package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

/**
 * Separates detected hardware from confirmed acceleration.
 * A GPU is only a candidate until a real voice/video smoke proves it works.
 */
public final class AssessComputeAccelerationUseCase {
    public ComputeAccelerationAssessment assess(OperationalSettings settings, ComputeEnvironmentReport environmentReport) {
        return assess(settings, environmentReport, null);
    }

    public ComputeAccelerationAssessment assess(OperationalSettings settings, ComputeEnvironmentReport environmentReport,
                                                XttsCudaSmokeReport cudaSmokeReport) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ComputeEnvironmentReport environment = environmentReport == null
                ? new InspectComputeEnvironmentUseCase().inspect(current)
                : environmentReport;
        OperationalSettings.ComputeSettings compute = current.compute();
        ArrayList<String> warnings = new ArrayList<>(environment.warnings());
        ArrayList<String> evidence = new ArrayList<>(environment.info());

        Optional<ComputeDeviceDescriptor> selectedGpu = selectedGpu(environment, compute);
        VoiceDecision voice = voiceDecision(compute, environment, selectedGpu, cudaSmokeReport, warnings, evidence);
        VideoDecision video = videoDecision(compute, environment, selectedGpu, warnings, evidence);

        evidence.add("La app distingue GPU detectada de GPU confirmada por prueba real.");
        return new ComputeAccelerationAssessment(
                voice.message,
                video.message,
                voice.gpuCandidate,
                video.gpuCandidate,
                voice.gpuConfirmed,
                warnings,
                evidence
        );
    }

    private static Optional<ComputeDeviceDescriptor> selectedGpu(ComputeEnvironmentReport environment,
                                                                 OperationalSettings.ComputeSettings compute) {
        if (compute.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE && !compute.selectedDeviceId().isBlank()) {
            return environment.devices().stream()
                    .filter(ComputeDeviceDescriptor::gpu)
                    .filter(device -> device.id().equalsIgnoreCase(compute.selectedDeviceId()))
                    .findFirst();
        }
        return environment.devices().stream().filter(ComputeDeviceDescriptor::gpu).findFirst();
    }

    private static VoiceDecision voiceDecision(OperationalSettings.ComputeSettings compute,
                                               ComputeEnvironmentReport environment,
                                               Optional<ComputeDeviceDescriptor> selectedGpu,
                                               XttsCudaSmokeReport cudaSmokeReport,
                                               ArrayList<String> warnings,
                                               ArrayList<String> evidence) {
        if (compute.policy() == ComputeDevicePolicy.CPU_ONLY || !compute.allowGpuForTts()) {
            evidence.add("Voz IA avanzada queda en CPU por política/configuración.");
            return new VoiceDecision("Voz IA avanzada usará CPU.", false, false);
        }
        if (manualGpuRequested(compute)) {
            boolean confirmed = cudaSmokeReport != null && cudaSmokeReport.gpuUsableForXtts()
                    && selectedMatchesSmoke(compute, cudaSmokeReport);
            String device = selectedGpu.map(ComputeDeviceDescriptor::displayName)
                    .orElse(compute.selectedDeviceId());
            evidence.add("Modo manual de voz: se respetará el dispositivo solicitado sin cambiarlo a CPU.");
            if (confirmed) {
                evidence.add("CUDA confirmado dentro del Python local de Voz IA avanzada: " + cudaSmokeReport.deviceArgument() + ".");
                return new VoiceDecision("Voz IA avanzada usará GPU confirmada: " + device + ".", true, true);
            }
            warnings.add("Modo manual: el motor recibirá el dispositivo solicitado; si el runtime no lo soporta, puede fallar o ignorarlo.");
            return new VoiceDecision("Voz IA avanzada intentará el dispositivo solicitado en modo manual: " + device + ".", true, false);
        }
        if (!environment.gpuDetected() || selectedGpu.isEmpty()) {
            evidence.add("No hay GPU candidata para voz; CPU es la ruta segura.");
            return new VoiceDecision("Voz IA avanzada usará CPU; no hay GPU candidata.", false, false);
        }
        ComputeDeviceDescriptor gpu = selectedGpu.get();
        String vendor = gpu.vendor().toLowerCase(Locale.ROOT);
        if (vendor.contains("nvidia")) {
            if (cudaSmokeReport != null && cudaSmokeReport.gpuUsableForXtts()) {
                evidence.add("CUDA confirmado dentro del Python local de Voz IA avanzada: " + cudaSmokeReport.deviceArgument() + ".");
                return new VoiceDecision("Voz IA avanzada puede usar GPU confirmada por prueba CUDA local.", true, true);
            }
            warnings.add("GPU detectada para voz: falta prueba CUDA real dentro del Python local antes de prometer aceleración.");
            // Contrato MOTOR-PERF1: falta prueba real del motor antes de prometer GPU.
            if (cudaSmokeReport != null && !cudaSmokeReport.issues().isEmpty()) {
                warnings.addAll(cudaSmokeReport.issues());
            }
            evidence.add("Voz IA avanzada puede intentar GPU NVIDIA, pero la confirmación depende de una prueba CUDA real.");
            return new VoiceDecision("Voz IA avanzada usará CPU; GPU candidata pendiente de prueba CUDA local.", true, false);
        }
        warnings.add("La GPU detectada no se declara compatible para Voz IA avanzada; se usará CPU hasta tener soporte probado.");
        evidence.add("Voz IA avanzada usa CPU para GPU no confirmada: " + gpu.vendor() + ".");
        return new VoiceDecision("Voz IA avanzada usará CPU; GPU detectada no confirmada para este motor.", false, false);
    }

    private static boolean manualGpuRequested(OperationalSettings.ComputeSettings compute) {
        String selected = compute.selectedDeviceId() == null ? "" : compute.selectedDeviceId().strip().toLowerCase(Locale.ROOT);
        return compute.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE
                && !selected.isBlank()
                && !"auto".equals(selected)
                && !"cpu".equals(selected);
    }

    private static boolean selectedMatchesSmoke(OperationalSettings.ComputeSettings compute, XttsCudaSmokeReport cudaSmokeReport) {
        String selected = compute.selectedDeviceId() == null ? "" : compute.selectedDeviceId().strip();
        return selected.isBlank()
                || cudaSmokeReport.selectedDeviceId().isBlank()
                || selected.equalsIgnoreCase(cudaSmokeReport.selectedDeviceId());
    }

    private static VideoDecision videoDecision(OperationalSettings.ComputeSettings compute,
                                               ComputeEnvironmentReport environment,
                                               Optional<ComputeDeviceDescriptor> selectedGpu,
                                               ArrayList<String> warnings,
                                               ArrayList<String> evidence) {
        VideoEncoderPolicy encoder = compute.videoEncoderPolicy();
        if (compute.policy() == ComputeDevicePolicy.CPU_ONLY || !compute.allowGpuForVideo() || encoder == VideoEncoderPolicy.CPU_X264) {
            evidence.add("Video usará codificación CPU como ruta segura.");
            return new VideoDecision("Video usará CPU con componente local.", false);
        }
        if (encoder.hardwareAccelerated()) {
            if (!environment.gpuDetected() || selectedGpu.isEmpty()) {
                warnings.add("Se pidió encoder acelerado, pero no se detectó GPU candidata; se debe usar CPU.");
                return new VideoDecision("Video usará CPU; no hay GPU candidata para el encoder solicitado.", false);
            }
            warnings.add("Encoder de video acelerado solicitado: debe verificarse con el componente local antes de prometer GPU.");
            evidence.add("Video tiene encoder acelerado candidato: " + encoder.label() + ".");
            return new VideoDecision("Video tiene GPU candidata; falta verificación del componente local.", true);
        }
        if (environment.gpuDetected() && compute.policy().canUseGpu()) {
            evidence.add("Video en automático: GPU puede ser candidata, pero CPU queda como fallback.");
            return new VideoDecision("Video en automático: GPU candidata si el componente local la verifica; CPU como respaldo.", true);
        }
        return new VideoDecision("Video usará CPU con componente local.", false);
    }

    private record VoiceDecision(String message, boolean gpuCandidate, boolean gpuConfirmed) {
    }

    private record VideoDecision(String message, boolean gpuCandidate) {
    }
}
