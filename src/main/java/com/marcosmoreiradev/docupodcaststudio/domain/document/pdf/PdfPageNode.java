package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;

/** One stable hierarchical object in a PdfPageMap sidecar. */
public record PdfPageNode(
        String id,
        String legacyRegionId,
        String parentId,
        PdfPageNodeKind kind,
        String semanticType,
        int order,
        PdfPageGeometry geometry,
        List<PdfPageGeometry> geometryParts,
        PdfTextStructure text,
        PdfEvidence evidence,
        PdfReviewState reviewState,
        List<String> childIds
) {
    public PdfPageNode {
        id = token(id, "id");
        legacyRegionId = legacyRegionId == null ? "" : legacyRegionId.strip();
        parentId = parentId == null ? "" : parentId.strip();
        if (kind == null || geometry == null || text == null || evidence == null || reviewState == null) {
            throw new IllegalArgumentException("PdfPageNode components are required");
        }
        semanticType = semanticType == null ? "UNKNOWN" : semanticType.strip();
        order = Math.max(0, order);
        geometryParts = geometryParts == null || geometryParts.isEmpty()
                ? List.of(geometry) : List.copyOf(geometryParts);
        childIds = childIds == null ? List.of() : childIds.stream()
                .filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a token");
        }
        return normalized;
    }
}
