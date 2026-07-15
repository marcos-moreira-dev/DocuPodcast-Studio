package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import java.util.List;
import java.util.Objects;

/** Summary shown after a Markdown grammar import. */
public record GrammarImportReport(
        ProjectGrammarKind grammarKind,
        String grammarVersion,
        String sourceFileName,
        int createdCount,
        int updatedCount,
        int omittedCount,
        boolean semanticsMaterialized,
        List<GrammarDiagnostic> diagnostics
) {
    public GrammarImportReport {
        grammarKind = Objects.requireNonNull(grammarKind, "grammarKind");
        grammarVersion = normalize(grammarVersion).isBlank() ? grammarKind.grammarVersion() : normalize(grammarVersion);
        sourceFileName = normalize(sourceFileName);
        createdCount = Math.max(0, createdCount);
        updatedCount = Math.max(0, updatedCount);
        omittedCount = Math.max(0, omittedCount);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public GrammarImportReport withMaterialized(boolean materialized) {
        return new GrammarImportReport(grammarKind, grammarVersion, sourceFileName, createdCount, updatedCount,
                omittedCount, materialized, diagnostics);
    }

    public GrammarImportReport withAdditionalDiagnostics(List<GrammarDiagnostic> extraDiagnostics) {
        if (extraDiagnostics == null || extraDiagnostics.isEmpty()) {
            return this;
        }
        java.util.ArrayList<GrammarDiagnostic> merged = new java.util.ArrayList<>(diagnostics);
        merged.addAll(extraDiagnostics);
        return new GrammarImportReport(grammarKind, grammarVersion, sourceFileName, createdCount, updatedCount,
                omittedCount, semanticsMaterialized, merged);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == GrammarDiagnosticSeverity.ERROR);
    }

    public String humanSummary() {
        String materialized = semanticsMaterialized ? " Semantica materializada." : " Semantica pendiente de guardar.";
        String warnings = diagnostics.isEmpty()
                ? ""
                : " Diagnosticos: " + diagnostics.stream().map(GrammarDiagnostic::message)
                .distinct().reduce((left, right) -> left + "; " + right).orElse("") + ".";
        return grammarKind.displayName() + ": " + createdCount + " creados, " + updatedCount + " actualizados, "
                + omittedCount + " omitidos." + materialized + warnings;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
