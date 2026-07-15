package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of the in-app guided preparation of the local advanced AI voice runtime. */
public record XttsRuntimePreparationReport(
        boolean success,
        int exitCode,
        Path setupScript,
        Path setupReport,
        XttsSetupReadinessReport readinessAfter,
        List<String> outputLines,
        String userMessage
) {
    public XttsRuntimePreparationReport {
        outputLines = List.copyOf(outputLines == null ? List.of() : outputLines);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static XttsRuntimePreparationReport skippedReady(XttsSetupReadinessReport readiness) {
        return new XttsRuntimePreparationReport(true, 0,
                readiness == null ? null : readiness.setupScript(),
                defaultReportPath(readiness == null ? null : readiness.applicationRoot()),
                readiness,
                List.of(),
                "Voz IA avanzada ya estaba preparada; no fue necesario ejecutar la instalación.");
    }

    public static XttsRuntimePreparationReport failed(Path script, Path report, XttsSetupReadinessReport readiness,
                                                      String message, List<String> outputLines) {
        return new XttsRuntimePreparationReport(false, -1, script, report, readiness, outputLines, message);
    }

    public String compactOutput() {
        if (outputLines.isEmpty()) {
            return "";
        }
        int from = Math.max(0, outputLines.size() - 12);
        return String.join(" · ", outputLines.subList(from, outputLines.size()));
    }

    public static Path defaultReportPath(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        return root.resolve("target/docupodcast-engine-setup/T90B_COQUI_PYTHON_SETUP_REPORT.md").normalize();
    }
}
