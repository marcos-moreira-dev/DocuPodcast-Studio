package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.List;

/** Human-readable local audit of bundled/runtime engine artifacts. */
public record EngineArtifactAuditReport(Path applicationRoot, List<EngineArtifactFileStatus> items) {
    public EngineArtifactAuditReport {
        if (applicationRoot == null) {
            applicationRoot = Path.of(".").toAbsolutePath().normalize();
        } else {
            applicationRoot = applicationRoot.toAbsolutePath().normalize();
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public long presentCount() {
        return items.stream().filter(EngineArtifactFileStatus::present).count();
    }

    public long missingRequiredCount() {
        return items.stream().filter(EngineArtifactFileStatus::missingRequiredForFinalRc).count();
    }

    public boolean readyForPersonalRuntime() {
        return missingRequiredCount() == 0L;
    }

    public String headline() {
        if (readyForPersonalRuntime()) {
            return "Artefactos locales completos para una prueba personal con motores reales.";
        }
        return "Faltan " + missingRequiredCount() + " artefactos obligatorios para cerrar motores reales.";
    }

    public String compactSummary() {
        return headline() + " Presentes: " + presentCount() + "/" + items.size() + ".";
    }

    public String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# DocuPodcast Studio - auditoría local de artefactos de motores\n\n");
        out.append("Raíz auditada: `").append(applicationRoot).append("`\n\n");
        out.append("Resultado: ").append(headline()).append("\n\n");
        out.append("| Artefacto | Ruta esperada | Estado | RC final | SHA-256 local | Acción |\n");
        out.append("|---|---|---:|---:|---|---|\n");
        for (EngineArtifactFileStatus item : items) {
            out.append(item.toMarkdownRow()).append('\n');
        }
        out.append("\nNotas:\n");
        out.append("- Esta auditoría no descarga ni instala nada por sí sola.\n");
        out.append("- Los checksums se calculan solo sobre archivos locales encontrados.\n");
        out.append("- Para uso final limpio, Configuración debe poder preparar o importar los faltantes sin depender de PATH global.\n");
        return out.toString();
    }
}
