package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import java.time.Instant;

/**
 * Diagnostic event emitted by a real local TTS process execution.
 *
 * <p>The event is intentionally lightweight and JSONL-friendly. It captures enough context to debug
 * a failed segment without storing the complete user document or huge process logs.</p>
 */
public record AudioProcessDiagnosticEvent(
        Instant timestamp,
        String jobId,
        String segmentId,
        int attempt,
        String engineName,
        String commandSummary,
        int exitCode,
        long durationMillis,
        boolean timedOut,
        String outputRelativePath,
        long outputBytes,
        String message,
        String outputTail
) {
    public AudioProcessDiagnosticEvent {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        jobId = requiredToken(jobId, "jobId");
        segmentId = normalize(segmentId);
        attempt = Math.max(1, attempt);
        engineName = normalize(engineName).isBlank() ? "TTS local" : normalize(engineName);
        commandSummary = normalize(commandSummary);
        exitCode = exitCode;
        durationMillis = Math.max(0L, durationMillis);
        outputRelativePath = portableOptionalPath(outputRelativePath);
        outputBytes = Math.max(0L, outputBytes);
        message = truncate(normalize(message), 1_000);
        outputTail = truncate(normalize(outputTail), 2_000);
    }

    public static AudioProcessDiagnosticEvent success(String jobId, String segmentId, int attempt, String engineName,
                                                      String commandSummary, long durationMillis, String outputRelativePath,
                                                      long outputBytes, String outputTail) {
        return new AudioProcessDiagnosticEvent(Instant.now(), jobId, segmentId, attempt, engineName, commandSummary,
                0, durationMillis, false, outputRelativePath, outputBytes, "Proceso TTS completado.", outputTail);
    }

    public static AudioProcessDiagnosticEvent failure(String jobId, String segmentId, int attempt, String engineName,
                                                      String commandSummary, int exitCode, long durationMillis,
                                                      boolean timedOut, String outputRelativePath, long outputBytes,
                                                      String message, String outputTail) {
        return new AudioProcessDiagnosticEvent(Instant.now(), jobId, segmentId, attempt, engineName, commandSummary,
                exitCode, durationMillis, timedOut, outputRelativePath, outputBytes, message, outputTail);
    }

    public boolean successful() {
        return exitCode == 0 && !timedOut;
    }

    public String compactLabel() {
        String status = successful() ? "OK" : (timedOut ? "TIMEOUT" : "ERROR " + exitCode);
        return timestamp + " · " + status + " · " + segmentId + " · intento " + attempt + " · " + message;
    }

    private static String requiredToken(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String portableOptionalPath(String value) {
        String normalized = normalize(value).replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase().startsWith("file:")) {
            throw new IllegalArgumentException("outputRelativePath must be project-relative: " + value);
        }
        return normalized;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
