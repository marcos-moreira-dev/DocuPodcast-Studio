package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.util.List;

/**
 * Lightweight preflight result for a command-based local TTS engine.
 *
 * <p>The report is intentionally infrastructure-local: it validates whether the command template looks
 * executable before a potentially long audio job is submitted, without coupling the domain or application
 * layers to process details.</p>
 */
public record LocalTtsPreflightReport(
        boolean ready,
        String summary,
        List<String> issues,
        List<String> hints
) {
    public LocalTtsPreflightReport {
        summary = normalize(summary);
        issues = issues == null ? List.of() : List.copyOf(issues.stream().map(LocalTtsPreflightReport::normalize).filter(value -> !value.isBlank()).toList());
        hints = hints == null ? List.of() : List.copyOf(hints.stream().map(LocalTtsPreflightReport::normalize).filter(value -> !value.isBlank()).toList());
        ready = ready && issues.isEmpty();
        if (summary.isBlank()) {
            summary = ready ? "Motor TTS local listo para validación por job." : "Motor TTS local no está listo.";
        }
    }

    public static LocalTtsPreflightReport ready(String summary, List<String> hints) {
        return new LocalTtsPreflightReport(true, summary, List.of(), hints);
    }

    public static LocalTtsPreflightReport notReady(String summary, List<String> issues, List<String> hints) {
        return new LocalTtsPreflightReport(false, summary, issues, hints);
    }

    public String userMessage() {
        if (ready) {
            return summary;
        }
        return summary + " " + String.join(" ", issues);
    }

    public String diagnosticLine() {
        String issueText = issues.isEmpty() ? "sin incidencias" : String.join(" | ", issues);
        String hintText = hints.isEmpty() ? "sin recomendaciones" : String.join(" | ", hints);
        return summary + " Issues: " + issueText + ". Hints: " + hintText + ".";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
