package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.nio.file.Path;
import java.util.List;

/** Result of installing CUDA-enabled PyTorch inside the self-contained XTTS Python runtime. */
public record XttsCudaPreparationReport(
        boolean success,
        int exitCode,
        boolean timedOut,
        Path setupScript,
        Path logFile,
        List<String> outputLines,
        String userMessage
) {
    public XttsCudaPreparationReport {
        outputLines = List.copyOf(outputLines == null ? List.of() : outputLines);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static XttsCudaPreparationReport failed(Path script, Path logFile, String message, List<String> outputLines) {
        return new XttsCudaPreparationReport(false, -1, false, script, logFile, outputLines, message);
    }

    public String compactOutput() {
        if (outputLines.isEmpty()) {
            return "";
        }
        int from = Math.max(0, outputLines.size() - 16);
        return String.join(" | ", outputLines.subList(from, outputLines.size()));
    }
}
