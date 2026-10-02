package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;

/** Stable source-fragment identity carried from narration preparation to playback/export. */
public record PdfNarrationFragmentBinding(
        String unitId,
        String segmentId,
        String sourceBlockId,
        String regionId,
        int pageNumber,
        int sourceTextStart,
        int sourceTextEnd,
        List<Integer> sourceLineIndices,
        List<Integer> sourceWordIndices,
        List<PdfPageGeometry> playbackBboxes,
        String sourceText,
        GeometryAuthority geometryAuthority
) {
    public PdfNarrationFragmentBinding {
        unitId = token(unitId, "unitId");
        segmentId = token(segmentId, "segmentId");
        sourceBlockId = token(sourceBlockId, "sourceBlockId");
        regionId = token(regionId, "regionId");
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be positive");
        sourceTextStart = Math.max(0, sourceTextStart);
        sourceTextEnd = Math.max(sourceTextStart, sourceTextEnd);
        sourceLineIndices = normalizeIndices(sourceLineIndices);
        sourceWordIndices = normalizeIndices(sourceWordIndices);
        playbackBboxes = playbackBboxes == null ? List.of()
                : playbackBboxes.stream().filter(java.util.Objects::nonNull).toList();
        sourceText = sourceText == null ? "" : sourceText.strip();
        geometryAuthority = geometryAuthority == null
                ? GeometryAuthority.REGION_FALLBACK : geometryAuthority;
    }

    public boolean fineGeometry() {
        return geometryAuthority == GeometryAuthority.LINE_CHAR_RANGES
                || geometryAuthority == GeometryAuthority.PAGE_MAP_SENTENCE
                || geometryAuthority == GeometryAuthority.EXPLICIT_FOCUS;
    }

    public enum GeometryAuthority {
        LINE_CHAR_RANGES,
        PAGE_MAP_SENTENCE,
        EXPLICIT_FOCUS,
        REGION_FALLBACK,
        PAGE_FALLBACK
    }

    private static List<Integer> normalizeIndices(List<Integer> values) {
        return values == null ? List.of() : values.stream()
                .filter(java.util.Objects::nonNull).filter(value -> value >= 0)
                .distinct().sorted().toList();
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }
}
