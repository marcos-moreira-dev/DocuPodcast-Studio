package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.List;

/** Diagnostic report for CPU/GPU policy without exposing technical noise on the reader screen. */
public record ComputeEnvironmentReport(
        ComputeDevicePolicy policy,
        String selectedDeviceId,
        List<ComputeDeviceDescriptor> devices,
        List<String> warnings,
        List<String> info
) {
    public ComputeEnvironmentReport {
        policy = policy == null ? ComputeDevicePolicy.AUTO : policy;
        selectedDeviceId = selectedDeviceId == null ? "" : selectedDeviceId.strip();
        devices = devices == null ? List.of() : List.copyOf(devices);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        info = info == null ? List.of() : List.copyOf(info);
    }

    public boolean gpuDetected() {
        return devices.stream().anyMatch(ComputeDeviceDescriptor::gpu);
    }

    public boolean usableForGpuWork() {
        return policy.canUseGpu() && gpuDetected() && warnings.stream().noneMatch(w -> w.toLowerCase(java.util.Locale.ROOT).contains("dispositivo específico"));
    }

    public String effectiveXttsDeviceArgument() {
        return ComputeDeviceArgumentMapper.toProcessDeviceArgument(policy, selectedDeviceId);
    }

    public java.util.Optional<ComputeDeviceDescriptor> selectedDevice() {
        if (selectedDeviceId.isBlank()) {
            return java.util.Optional.empty();
        }
        return devices.stream().filter(device -> device.id().equalsIgnoreCase(selectedDeviceId)).findFirst();
    }

    public String summary() {
        if (policy == ComputeDevicePolicy.CPU_ONLY) {
            return "Solo CPU: Voz IA avanzada procesará la lectura en CPU.";
        }
        if (gpuDetected()) {
            return policy.label() + ": CPU/GPU detectadas para elegir el dispositivo de Voz IA avanzada.";
        }
        return policy.label() + ": no se detectó GPU; Voz IA avanzada usará CPU o fallback honesto.";
    }

    public String toMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Diagnóstico de dispositivo de inferencia\n\n");
        markdown.append("- Política: ").append(policy.name()).append(" — ").append(policy.label()).append("\n");
        markdown.append("- Dispositivo seleccionado: ").append(selectedDeviceId.isBlank() ? "automático" : selectedDeviceId).append("\n");
        markdown.append("- GPU detectada: ").append(gpuDetected() ? "sí" : "no").append("\n");
        markdown.append("- Dispositivo solicitado al proceso de voz: ").append(effectiveXttsDeviceArgument()).append("\n\n");
        markdown.append("## Dispositivos\n\n");
        for (ComputeDeviceDescriptor device : devices) {
            markdown.append("- ").append(device.id()).append(" · ").append(device.displayName())
                    .append(" · ").append(device.type()).append(" · ").append(device.vendor()).append("\n");
        }
        markdown.append("\n## Advertencias\n\n");
        if (warnings.isEmpty()) {
            markdown.append("Sin advertencias.\n");
        } else {
            for (String warning : warnings) {
                markdown.append("- ").append(warning).append("\n");
            }
        }
        markdown.append("\n## Información\n\n");
        for (String line : info) {
            markdown.append("- ").append(line).append("\n");
        }
        return markdown.toString();
    }
}
