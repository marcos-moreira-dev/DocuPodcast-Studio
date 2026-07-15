package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Aggregate export readiness report used by package export, diagnostics and smoke tests. */
public record ExportReadinessReport(
        String projectTitle,
        ExportReadinessStatus status,
        List<ExportReadinessItem> items,
        Instant createdAt
) {
    public ExportReadinessReport {
        projectTitle = projectTitle == null || projectTitle.isBlank() ? "Proyecto DocuPodcast" : projectTitle.strip();
        status = Objects.requireNonNull(status, "status");
        items = items == null ? List.of() : List.copyOf(items);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static ExportReadinessReport from(String projectTitle, List<ExportReadinessItem> items) {
        List<ExportReadinessItem> safeItems = items == null ? List.of() : List.copyOf(items);
        return new ExportReadinessReport(projectTitle, aggregate(safeItems), safeItems, Instant.now());
    }

    public long exportableCount() {
        return items.stream().filter(ExportReadinessItem::exportable).count();
    }

    public long blockedCount() {
        return items.stream().filter(ExportReadinessItem::blocked).count();
    }

    public boolean hasExportableOutput() {
        return exportableCount() > 0;
    }

    public boolean hasBlockedOutput() {
        return blockedCount() > 0;
    }

    public String toMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Preparación de exportaciones DocuPodcast\n\n");
        markdown.append("- Proyecto: ").append(projectTitle).append("\n");
        markdown.append("- Estado general: ").append(status.displayName()).append("\n");
        markdown.append("- Fecha UTC: ").append(createdAt).append("\n");
        markdown.append("- Salidas exportables: ").append(exportableCount()).append("\n");
        markdown.append("- Salidas bloqueadas: ").append(blockedCount()).append("\n\n");
        markdown.append("Este reporte pertenece al cerebro de exportación: declara qué puede salir, qué falta y qué no se promete. ")
                .append("No depende de la interfaz gráfica y evita botones o promesas sin cadena real.\n\n");
        markdown.append("| Salida | Formato | Estado | Destino sugerido |\n");
        markdown.append("|---|---|---|---|\n");
        for (ExportReadinessItem item : items) {
            markdown.append("| ").append(item.kind().displayName())
                    .append(" | ").append(item.format().displayName())
                    .append(" | ").append(item.status().displayName())
                    .append(" | `").append(item.targetHint()).append("` |\n");
        }
        for (ExportReadinessItem item : items) {
            markdown.append("\n## ").append(item.kind().displayName()).append("\n\n");
            markdown.append("- Estado: ").append(item.status().displayName()).append("\n");
            markdown.append("- Formato: ").append(item.format().displayName()).append("\n");
            if (!item.targetHint().isBlank()) {
                markdown.append("- Destino sugerido: `").append(item.targetHint()).append("`\n");
            }
            appendList(markdown, "Evidencia", item.evidence());
            appendList(markdown, "Faltantes", item.missingRequirements());
            appendList(markdown, "Limitaciones honestas", item.limitations());
        }
        return markdown.toString();
    }

    private static ExportReadinessStatus aggregate(List<ExportReadinessItem> items) {
        if (items.isEmpty() || items.stream().noneMatch(ExportReadinessItem::exportable)) {
            return ExportReadinessStatus.BLOQUEADO;
        }
        if (items.stream().anyMatch(item -> item.status() != ExportReadinessStatus.EXPORTABLE)) {
            return ExportReadinessStatus.EXPORTABLE_CON_ADVERTENCIAS;
        }
        return ExportReadinessStatus.EXPORTABLE;
    }

    private static void appendList(StringBuilder markdown, String title, List<String> values) {
        markdown.append("\n### ").append(title).append("\n\n");
        if (values == null || values.isEmpty()) {
            markdown.append("- Ninguno.\n");
            return;
        }
        for (String value : values) {
            markdown.append("- ").append(value).append("\n");
        }
    }
}
