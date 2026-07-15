package com.marcosmoreiradev.docupodcaststudio.application.smoke;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Markdown-ready report produced by the automatic brain smoke. */
public record BrainSmokeReport(
        String scenarioId,
        String title,
        Instant startedAt,
        Instant finishedAt,
        List<BrainSmokeStep> steps,
        List<String> artifacts
) {
    public BrainSmokeReport {
        scenarioId = normalize(scenarioId, "docupodcast-brain-smoke-v1");
        title = normalize(title, "Smoke automático del cerebro DocuPodcast");
        startedAt = Objects.requireNonNullElseGet(startedAt, Instant::now);
        finishedAt = Objects.requireNonNullElse(finishedAt, startedAt);
        steps = steps == null ? List.of() : List.copyOf(steps);
        artifacts = artifacts == null ? List.of() : artifacts.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public boolean successful() {
        return steps.stream().allMatch(step -> step.status().successful());
    }

    public long passedCount() {
        return steps.stream().filter(step -> step.status() == BrainSmokeStepStatus.PASSED).count();
    }

    public long warningCount() {
        return steps.stream().filter(step -> step.status() == BrainSmokeStepStatus.WARNING).count();
    }

    public long failedCount() {
        return steps.stream().filter(step -> step.status() == BrainSmokeStepStatus.FAILED).count();
    }

    public String statusLabel() {
        return successful() ? (warningCount() > 0 ? "OK_CON_ADVERTENCIAS" : "OK") : "FALLIDO";
    }

    public String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(title).append("\n\n");
        out.append("- Escenario: `").append(scenarioId).append("`\n");
        out.append("- Estado: ").append(statusLabel()).append("\n");
        out.append("- Inicio UTC: ").append(startedAt).append("\n");
        out.append("- Fin UTC: ").append(finishedAt).append("\n");
        out.append("- Pasos OK: ").append(passedCount()).append("\n");
        out.append("- Advertencias: ").append(warningCount()).append("\n");
        out.append("- Fallos: ").append(failedCount()).append("\n\n");
        out.append("## Pasos\n\n");
        out.append("| Paso | Estado | Duración | Detalle |\n");
        out.append("|---|---|---:|---|\n");
        for (BrainSmokeStep step : steps) {
            out.append("| `").append(escape(step.id())).append("` ").append(escape(step.name()))
                    .append(" | ").append(step.status().displayName())
                    .append(" | ").append(step.durationLabel())
                    .append(" | ").append(escape(step.detail()))
                    .append(" |\n");
        }
        if (!artifacts.isEmpty()) {
            out.append("\n## Evidencia generada\n\n");
            for (String artifact : artifacts) {
                out.append("- `").append(artifact.replace('\\', '/')).append("`\n");
            }
        }
        out.append("\n## Alcance\n\n");
        out.append("Este smoke automático valida el cerebro sin JavaFX: importación documental, ")
                .append("narración interna, audio mock, round-trip, integridad, export readiness, ")
                .append("paquete auditable y paquete de video simple. No reemplaza el smoke visual manual.\n");
        return out.toString();
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String escape(String value) {
        return normalize(value, "").replace("|", "\\|").replace("\n", " ");
    }
}
