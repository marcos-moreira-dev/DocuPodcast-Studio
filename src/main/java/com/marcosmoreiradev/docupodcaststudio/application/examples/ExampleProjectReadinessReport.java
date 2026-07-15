package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.util.List;

/** Operational readiness summary for a bundled demo before the user creates it. */
public record ExampleProjectReadinessReport(
        String exampleId,
        boolean sourceAvailable,
        int assetCount,
        int visualBindingCount,
        int unresolvedBindingCount,
        List<String> warnings
) {
    public ExampleProjectReadinessReport {
        exampleId = exampleId == null ? "" : exampleId.strip();
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean readyForVisualDemo() {
        return sourceAvailable && assetCount > 0 && visualBindingCount > 0 && unresolvedBindingCount == 0;
    }

    public String statusLabel() {
        if (!sourceAvailable) {
            return "Falta Word demo";
        }
        if (readyForVisualDemo()) {
            return "Listo para demo visual";
        }
        if (assetCount > 0 && visualBindingCount == 0) {
            return "Imágenes disponibles";
        }
        if (unresolvedBindingCount > 0) {
            return "Revisar asociaciones";
        }
        return "Demo básico";
    }

    public String operationalSummary() {
        if (readyForVisualDemo()) {
            return "Creará un proyecto con " + assetCount + " imágenes y " + visualBindingCount
                    + " asociaciones visuales listas para el rail.";
        }
        if (!warnings.isEmpty()) {
            return String.join(" ", warnings);
        }
        return "Creará el Word demo y permitirá completar voces, imágenes o audio desde Documento.";
    }
}
