package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read/highlight projection for faithful PDF pages plus available text layer. */
public record PdfVisualReadingProjection(
        List<PdfTextLayer> textLayers,
        Map<String, PdfVisualTextHighlight> highlightsByBlockId,
        List<PdfVisualTextTarget> targets,
        List<String> warnings
) {
    private static final double HIT_TOLERANCE_POINTS = 2.5;

    public PdfVisualReadingProjection(List<PdfTextLayer> textLayers,
                                      Map<String, PdfVisualTextHighlight> highlightsByBlockId,
                                      List<String> warnings) {
        this(textLayers, highlightsByBlockId, List.of(), warnings);
    }

    public PdfVisualReadingProjection {
        textLayers = textLayers == null ? List.of() : List.copyOf(textLayers);
        highlightsByBlockId = highlightsByBlockId == null ? Map.of() : Map.copyOf(highlightsByBlockId);
        targets = targets == null ? List.of() : List.copyOf(targets);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public Optional<PdfVisualTextHighlight> highlightForBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        PdfVisualTextHighlight highlight = highlightsByBlockId.get(blockId);
        return highlight != null && highlight.available() ? Optional.of(highlight) : Optional.empty();
    }

    public Optional<PdfVisualTextTarget> targetForBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> blockId.equals(target.blockId()))
                .filter(target -> target.kind() == PdfVisualTextTargetKind.BLOCK)
                .findFirst();
    }

    public Optional<PdfVisualTextTarget> targetForRange(com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange range) {
        if (range == null) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> range.equals(target.range()))
                .findFirst();
    }

    public Optional<PdfVisualTextTarget> targetAt(int pageNumber, double xPoints, double yPoints) {
        if (pageNumber <= 0) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> target.pageNumber() == pageNumber)
                .filter(target -> target.contains(xPoints, yPoints, HIT_TOLERANCE_POINTS))
                .min(java.util.Comparator
                        .comparingInt(PdfVisualReadingProjection::targetPriority)
                        .thenComparingDouble(PdfVisualTextTarget::areaPoints));
    }

    public boolean hasTextLayer() {
        return textLayers.stream().anyMatch(PdfTextLayer::available);
    }

    private static int targetPriority(PdfVisualTextTarget target) {
        return target.kind() == PdfVisualTextTargetKind.SENTENCE ? 0 : 1;
    }
}
