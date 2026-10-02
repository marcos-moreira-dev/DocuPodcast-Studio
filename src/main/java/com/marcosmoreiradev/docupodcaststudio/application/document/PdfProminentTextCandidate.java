package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** OCR/native evidence of uncovered prominent text; it deliberately has no semantic type. */
public record PdfProminentTextCandidate(
        String evidenceId,
        String text,
        PdfPageRegion region,
        double confidence,
        double relativeHeight,
        double relativeY,
        double prominenceRatio,
        String source,
        List<String> signals
) {
    public PdfProminentTextCandidate {
        evidenceId = evidenceId == null ? "" : evidenceId.strip();
        text = text == null ? "" : text.strip();
        confidence = finiteUnit(confidence);
        relativeHeight = finiteUnit(relativeHeight);
        relativeY = finiteUnit(relativeY);
        prominenceRatio = Double.isFinite(prominenceRatio)
                ? Math.max(0.0, prominenceRatio) : 0.0;
        source = source == null ? "" : source.strip();
        signals = signals == null ? List.of() : List.copyOf(signals);
    }

    private static double finiteUnit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
