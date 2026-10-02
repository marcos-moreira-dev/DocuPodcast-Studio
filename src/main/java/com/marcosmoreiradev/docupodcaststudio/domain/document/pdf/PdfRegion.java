package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One persistent, reviewable region of a PDF page. */
public record PdfRegion(
        String id,
        int pageNumber,
        double xMin,
        double yMin,
        double xMax,
        double yMax,
        int columnIndex,
        int readingOrder,
        String text,
        PdfRegionType automaticType,
        PdfNarratability automaticNarratability,
        List<String> reasons,
        PdfRegionEvidence evidence,
        List<PdfRegionEvidence> evidenceCandidates,
        PdfRegionOverride override,
        Map<String, String> attributes,
        long revision
) {
    public PdfRegion {
        id = token(id, "id");
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        if (!finiteRectangle(xMin, yMin, xMax, yMax)) {
            throw new IllegalArgumentException("PDF region must have a finite positive rectangle");
        }
        columnIndex = Math.max(0, columnIndex);
        readingOrder = Math.max(0, readingOrder);
        text = text == null ? "" : text.strip();
        automaticType = Objects.requireNonNullElse(automaticType, PdfRegionType.UNKNOWN);
        automaticNarratability = Objects.requireNonNullElse(automaticNarratability, PdfNarratability.UNCERTAIN);
        reasons = reasons == null ? List.of() : reasons.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank()).distinct().toList();
        evidence = evidence == null
                ? new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.0, "unknown", "unknown", "unknown", "unknown")
                : evidence;
        evidenceCandidates = evidenceCandidates == null || evidenceCandidates.isEmpty()
                ? List.of(evidence)
                : java.util.stream.Stream.concat(java.util.stream.Stream.of(evidence),
                        evidenceCandidates.stream().filter(Objects::nonNull))
                .distinct().toList();
        override = override == null ? PdfRegionOverride.empty() : override;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        revision = Math.max(1L, revision);
    }

    public PdfRegion(String id, int pageNumber, double xMin, double yMin, double xMax, double yMax,
                     int columnIndex, int readingOrder, String text, PdfRegionType automaticType,
                     PdfNarratability automaticNarratability, List<String> reasons,
                     PdfRegionEvidence evidence, PdfRegionOverride override,
                     Map<String, String> attributes, long revision) {
        this(id, pageNumber, xMin, yMin, xMax, yMax, columnIndex, readingOrder, text,
                automaticType, automaticNarratability, reasons, evidence, List.of(evidence),
                override, attributes, revision);
    }

    public String effectiveText() {
        return override.text() == null ? text : override.text();
    }

    public PdfRegionType effectiveType() {
        return override.type() == null ? automaticType : override.type();
    }

    public PdfNarratability effectiveNarratability() {
        return override.narratability() == null ? automaticNarratability : override.narratability();
    }

    public int effectiveReadingOrder() {
        return override.readingOrder() == null ? readingOrder : override.readingOrder();
    }

    public PdfRegionRole regionRole() {
        try {
            return PdfRegionRole.valueOf(attributes.getOrDefault(
                    "regionRole", PdfRegionRole.LEAF.name()));
        } catch (IllegalArgumentException ignored) {
            return PdfRegionRole.LEAF;
        }
    }

    public boolean container() {
        return regionRole() == PdfRegionRole.CONTAINER;
    }

    /** Playback may only bind to leaf regions; legacy regions remain leaves. */
    public boolean playbackTarget() {
        return !container() && Boolean.parseBoolean(
                attributes.getOrDefault("playbackTarget", "true"));
    }

    public String parentId() {
        return attributes.getOrDefault("parentId", "").strip();
    }

    public String semanticRole() {
        return attributes.getOrDefault("semanticRole", "").strip();
    }

    public String presentation() {
        return attributes.getOrDefault("presentation", "").strip();
    }

    public PdfContentRoute contentRoute() {
        try {
            return PdfContentRoute.valueOf(attributes.getOrDefault(
                    "contentRoute", PdfContentRoute.VLM_MIXED.name()));
        } catch (IllegalArgumentException ignored) {
            return PdfContentRoute.VLM_MIXED;
        }
    }

    public PdfRegion withNarratabilityOverride(PdfNarratability narratability) {
        PdfRegionOverride updatedOverride = new PdfRegionOverride(
                override.text(), override.type(), narratability, override.readingOrder());
        return new PdfRegion(id, pageNumber, xMin, yMin, xMax, yMax,
                columnIndex, readingOrder, text, automaticType, automaticNarratability,
                reasons, evidence, evidenceCandidates, updatedOverride, attributes,
                revision + 1);
    }

    private static boolean finiteRectangle(double xMin, double yMin, double xMax, double yMax) {
        return Double.isFinite(xMin) && Double.isFinite(yMin) && Double.isFinite(xMax) && Double.isFinite(yMax)
                && xMax > xMin && yMax > yMin;
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }
}
