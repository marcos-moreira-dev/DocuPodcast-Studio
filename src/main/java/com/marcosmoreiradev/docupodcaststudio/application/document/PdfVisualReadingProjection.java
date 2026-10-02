package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read/highlight projection for faithful PDF pages plus available text layer. */
public record PdfVisualReadingProjection(
        List<PdfTextLayer> textLayers,
        Map<String, PdfVisualTextHighlight> highlightsByRegionId,
        List<PdfVisualTextTarget> targets,
        List<String> warnings
) {
    private static final double HIT_TOLERANCE_POINTS = 2.5;

    public PdfVisualReadingProjection(List<PdfTextLayer> textLayers,
                                      Map<String, PdfVisualTextHighlight> highlightsByRegionId,
                                      List<String> warnings) {
        this(textLayers, highlightsByRegionId, List.of(), warnings);
    }

    public PdfVisualReadingProjection {
        textLayers = textLayers == null ? List.of() : List.copyOf(textLayers);
        highlightsByRegionId = highlightsByRegionId == null ? Map.of() : Map.copyOf(highlightsByRegionId);
        targets = targets == null ? List.of() : List.copyOf(targets);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public Optional<PdfVisualTextHighlight> highlightForRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        PdfVisualTextHighlight highlight = highlightsByRegionId.get(regionId);
        return highlight != null && highlight.available() ? Optional.of(highlight) : Optional.empty();
    }

    public Optional<PdfVisualTextTarget> targetForRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> regionId.equals(target.regionId())
                        || target.sourceRegionIds().contains(regionId))
                .filter(target -> target.kind() == PdfVisualTextTargetKind.BLOCK
                        || target.kind() == PdfVisualTextTargetKind.SEMANTIC_COMPONENT)
                .sorted(java.util.Comparator.comparingInt(
                        PdfVisualReadingProjection::targetPriority))
                .findFirst();
    }

    public Optional<PdfVisualTextTarget> targetForSelection(PdfRegionSelectionRef selection) {
        if (selection == null) return Optional.empty();
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> target.pageNumber() == selection.pageNumber())
                .filter(target -> target.regionId().equals(selection.regionId()))
                .filter(target -> target.startOffset() == selection.startOffset()
                        && target.endOffset() == selection.endOffset())
                .findFirst();
    }

    public Optional<PdfVisualTextTarget> sentenceTargetForRegion(
            String regionId, int sentenceIndex) {
        if (regionId == null || regionId.isBlank() || sentenceIndex < 0) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(PdfVisualTextTarget::available)
                .filter(target -> regionId.equals(target.regionId()))
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE)
                .sorted(java.util.Comparator.comparingInt(
                        PdfVisualTextTarget::startOffset))
                .skip(sentenceIndex)
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
                        .comparingInt(this::hitTargetPriority)
                        .thenComparingDouble(PdfVisualTextTarget::areaPoints));
    }

    public boolean hasTextLayer() {
        return textLayers.stream().anyMatch(PdfTextLayer::available);
    }

    private static int targetPriority(PdfVisualTextTarget target) {
        return switch (target.kind()) {
            case SEMANTIC_COMPONENT -> 0;
            case SENTENCE -> 1;
            case BLOCK -> 2;
        };
    }

    private int hitTargetPriority(PdfVisualTextTarget target) {
        if (target.kind() != PdfVisualTextTargetKind.SENTENCE) {
            return targetPriority(target);
        }
        boolean fallbackCoversWholeBlock = targets.stream()
                .filter(candidate -> candidate.kind() == PdfVisualTextTargetKind.BLOCK)
                .filter(candidate -> candidate.regionId().equals(target.regionId()))
                .anyMatch(block -> sameGeometry(block.region(), target.region())
                        && (target.startOffset() > block.startOffset()
                        || target.endOffset() < block.endOffset()));
        return fallbackCoversWholeBlock ? 3 : targetPriority(target);
    }

    private static boolean sameGeometry(PdfPageRegion left,
                                        PdfPageRegion right) {
        if (left == null || right == null) return false;
        double epsilon = 0.01;
        return left.pageNumber() == right.pageNumber()
                && Math.abs(left.xMinPoints() - right.xMinPoints()) < epsilon
                && Math.abs(left.yMinPoints() - right.yMinPoints()) < epsilon
                && Math.abs(left.xMaxPoints() - right.xMaxPoints()) < epsilon
                && Math.abs(left.yMaxPoints() - right.yMaxPoints()) < epsilon;
    }
}
