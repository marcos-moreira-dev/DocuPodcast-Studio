package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.List;

/** Honest product-level assessment of CPU/GPU acceleration candidates. */
public record ComputeAccelerationAssessment(
        String voiceExecutionMode,
        String videoExecutionMode,
        boolean voiceGpuCandidate,
        boolean videoGpuCandidate,
        boolean gpuUseConfirmedByRuntime,
        List<String> warnings,
        List<String> evidence
) {
    public ComputeAccelerationAssessment {
        voiceExecutionMode = normalize(voiceExecutionMode, "Voz IA avanzada usará CPU hasta tener prueba real.");
        videoExecutionMode = normalize(videoExecutionMode, "Video usará CPU hasta verificar el componente local.");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public String gpuPromiseLabel() {
        if (gpuUseConfirmedByRuntime) {
            return "GPU confirmada por prueba real.";
        }
        if (voiceGpuCandidate || videoGpuCandidate) {
            return "GPU candidata; falta prueba real antes de prometer aceleración.";
        }
        return "Sin promesa GPU; CPU como ruta segura.";
    }

    public String summary() {
        return voiceExecutionMode + " " + videoExecutionMode + " " + gpuPromiseLabel();
    }

    public String toMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Diagnóstico honesto de rendimiento\n\n");
        markdown.append("- Voz IA avanzada: ").append(voiceExecutionMode).append("\n");
        markdown.append("- Video: ").append(videoExecutionMode).append("\n");
        markdown.append("- Promesa GPU: ").append(gpuPromiseLabel()).append("\n\n");
        markdown.append("## Evidencia\n\n");
        if (evidence.isEmpty()) {
            markdown.append("- Sin evidencia adicional.\n");
        } else {
            for (String line : evidence) {
                markdown.append("- ").append(line).append("\n");
            }
        }
        markdown.append("\n## Advertencias\n\n");
        if (warnings.isEmpty()) {
            markdown.append("- Sin advertencias.\n");
        } else {
            for (String warning : warnings) {
                markdown.append("- ").append(warning).append("\n");
            }
        }
        return markdown.toString();
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
