package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.Optional;

/** Builds the user-visible notice for automatic GPU preference when XTTS CUDA is not verified. */
public final class InspectXttsGpuFallbackDecisionUseCase {
    public Optional<UserVisibleDecision> inspect(OperationalSettings settings, XttsCudaSmokeReport cudaSmokeReport) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (!advancedVoiceSelected(current)) {
            return Optional.empty();
        }
        OperationalSettings.ComputeSettings compute = current.compute();
        if (!explicitGpuRequested(compute)) {
            return Optional.empty();
        }
        XttsCudaSmokeReport smoke = cudaSmokeReport == null
                ? XttsCudaSmokeReport.pending(null, "Falta ejecutar la prueba CUDA de Voz IA avanzada dentro del Python local.")
                : cudaSmokeReport;
        if (smoke.gpuUsableForXtts()) {
            return Optional.empty();
        }
        String selected = compute.selectedDeviceId().isBlank() ? compute.policy().label() : compute.selectedDeviceId();
        String message = "Preferiste GPU para Voz IA avanzada, pero la prueba CUDA del Python local no está aprobada. "
                + "En modo automático DocuPodcast usará CPU para esta generación. Si quieres intentar una GPU de todos modos, elige Dispositivo específico.";
        String detail = "Motor: Voz IA avanzada"
                + System.lineSeparator() + "GPU solicitada: " + selected
                + System.lineSeparator() + "Estado CUDA: " + smoke.statusLabel()
                + System.lineSeparator() + "Device solicitado al wrapper: " + blankAsDash(smoke.deviceArgument())
                + System.lineSeparator() + "GPU reportada por PyTorch: " + blankAsDash(smoke.deviceName())
                + System.lineSeparator() + "PyTorch: " + blankAsDash(smoke.torchVersion())
                + System.lineSeparator() + "PyTorch CUDA: " + blankAsDash(smoke.torchCudaVersion())
                + (smoke.issues().isEmpty() ? "" : System.lineSeparator() + "Detalle: " + String.join(" | ", smoke.issues()));
        return Optional.of(UserVisibleDecision.defensiveFallback(
                "Voz IA avanzada usará CPU en modo automático",
                message,
                detail));
    }

    private static boolean explicitGpuRequested(OperationalSettings.ComputeSettings compute) {
        return compute.allowGpuForTts()
                && compute.policy() == ComputeDevicePolicy.PREFER_GPU;
    }

    private static boolean advancedVoiceSelected(OperationalSettings settings) {
        String mode = settings.tts().engineMode() == null ? "" : settings.tts().engineMode().toLowerCase(java.util.Locale.ROOT);
        String command = settings.tts().commandTemplate() == null ? "" : settings.tts().commandTemplate().toLowerCase(java.util.Locale.ROOT);
        return mode.equals("xtts") || mode.equals("coqui")
                || command.contains("xtts-file-to-wav")
                || command.contains("synthesize_xtts")
                || command.contains("xtts-wrapper");
    }

    private static String blankAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value.strip();
    }
}
