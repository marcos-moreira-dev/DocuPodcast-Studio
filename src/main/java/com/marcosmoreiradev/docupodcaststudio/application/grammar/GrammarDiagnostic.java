package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import java.util.Objects;

/** A controlled diagnostic produced while parsing or materializing a grammar. */
public record GrammarDiagnostic(
        GrammarDiagnosticSeverity severity,
        String code,
        String message,
        String detail
) {
    public GrammarDiagnostic {
        severity = Objects.requireNonNullElse(severity, GrammarDiagnosticSeverity.INFO);
        code = normalize(code);
        message = normalize(message);
        detail = normalize(detail);
        if (code.isBlank()) {
            code = "GRAMMAR";
        }
        if (message.isBlank()) {
            message = "Diagnostico de gramatica.";
        }
    }

    public static GrammarDiagnostic info(String code, String message) {
        return new GrammarDiagnostic(GrammarDiagnosticSeverity.INFO, code, message, "");
    }

    public static GrammarDiagnostic warning(String code, String message) {
        return new GrammarDiagnostic(GrammarDiagnosticSeverity.WARNING, code, message, "");
    }

    public static GrammarDiagnostic error(String code, String message, String detail) {
        return new GrammarDiagnostic(GrammarDiagnosticSeverity.ERROR, code, message, detail);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
