package com.marcosmoreiradev.docupodcaststudio.application.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable request for a local external process such as Python, FFmpeg, Piper or PowerShell. */
public record ExternalProcessRequest(
        List<String> command,
        Path workingDirectory,
        Duration timeout,
        Map<String, String> environment,
        String auditLabel,
        boolean redirectErrorStream
) {
    public ExternalProcessRequest {
        command = List.copyOf(Objects.requireNonNull(command, "command"));
        if (command.isEmpty() || command.stream().anyMatch(part -> part == null || part.isBlank())) {
            throw new IllegalArgumentException("External process command must contain non-blank parts");
        }
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(30) : timeout;
        environment = Map.copyOf(environment == null ? Map.of() : environment);
        auditLabel = auditLabel == null || auditLabel.isBlank() ? command.get(0) : auditLabel.strip();
    }

    public static ExternalProcessRequest of(List<String> command, String auditLabel, Duration timeout) {
        return new ExternalProcessRequest(command, null, timeout, Map.of(), auditLabel, false);
    }

    public ExternalProcessRequest withWorkingDirectory(Path value) {
        return new ExternalProcessRequest(command, value, timeout, environment, auditLabel, redirectErrorStream);
    }

    public ExternalProcessRequest withTimeout(Duration value) {
        return new ExternalProcessRequest(command, workingDirectory, value, environment, auditLabel, redirectErrorStream);
    }

    public ExternalProcessRequest withEnvironment(String key, String value) {
        java.util.LinkedHashMap<String, String> next = new java.util.LinkedHashMap<>(environment);
        if (key != null && !key.isBlank() && value != null) {
            next.put(key, value);
        }
        return new ExternalProcessRequest(command, workingDirectory, timeout, next, auditLabel, redirectErrorStream);
    }

    public ExternalProcessRequest withEnvironment(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        java.util.LinkedHashMap<String, String> next = new java.util.LinkedHashMap<>(environment);
        values.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                next.put(key, value);
            }
        });
        return new ExternalProcessRequest(command, workingDirectory, timeout, next, auditLabel, redirectErrorStream);
    }

    public ExternalProcessRequest redirectingErrorStream() {
        return withRedirectErrorStream(true);
    }

    public ExternalProcessRequest withRedirectErrorStream(boolean value) {
        return new ExternalProcessRequest(command, workingDirectory, timeout, environment, auditLabel, value);
    }

    public String commandAudit() {
        return String.join(" ", command);
    }
}
