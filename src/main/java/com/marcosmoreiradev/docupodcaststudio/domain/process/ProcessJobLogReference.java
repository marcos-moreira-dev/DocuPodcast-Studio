package com.marcosmoreiradev.docupodcaststudio.domain.process;

/** Project-relative references to logs/diagnostics produced by a local process. */
public record ProcessJobLogReference(
        String generationLogPath,
        String diagnosticsPath,
        String stdoutLogPath,
        String stderrLogPath
) {
    public ProcessJobLogReference {
        generationLogPath = portableOptionalPath(generationLogPath);
        diagnosticsPath = portableOptionalPath(diagnosticsPath);
        stdoutLogPath = portableOptionalPath(stdoutLogPath);
        stderrLogPath = portableOptionalPath(stderrLogPath);
    }

    public boolean hasAnyLog() {
        return !generationLogPath.isBlank() || !diagnosticsPath.isBlank()
                || !stdoutLogPath.isBlank() || !stderrLogPath.isBlank();
    }

    private static String portableOptionalPath(String value) {
        String normalized = normalize(value).replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase().startsWith("file:")) {
            throw new IllegalArgumentException("Log path must be project-relative: " + value);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
