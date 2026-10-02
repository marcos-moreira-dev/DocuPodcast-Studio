package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Objects;

/** Low-overhead per-object measurements; unavailable hardware values remain zero. */
public record PdfOperationMetrics(
        long durationMillis,
        long ttftMillis,
        long promptTokens,
        long outputTokens,
        long contextCharacters,
        long ramBytes,
        long vramBytes,
        long cpuMillis,
        int widthPixels,
        int heightPixels,
        String engineId,
        String modelId,
        String doneReason
) {
    public PdfOperationMetrics {
        durationMillis = Math.max(0, durationMillis);
        ttftMillis = Math.max(0, ttftMillis);
        promptTokens = Math.max(0, promptTokens);
        outputTokens = Math.max(0, outputTokens);
        contextCharacters = Math.max(0, contextCharacters);
        ramBytes = Math.max(0, ramBytes);
        vramBytes = Math.max(0, vramBytes);
        cpuMillis = Math.max(0, cpuMillis);
        widthPixels = Math.max(0, widthPixels);
        heightPixels = Math.max(0, heightPixels);
        engineId = Objects.toString(engineId, "").strip();
        modelId = Objects.toString(modelId, "").strip();
        doneReason = Objects.toString(doneReason, "").strip();
    }

    public static PdfOperationMetrics empty(String engineId) {
        return new PdfOperationMetrics(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, engineId, "", "");
    }
}
