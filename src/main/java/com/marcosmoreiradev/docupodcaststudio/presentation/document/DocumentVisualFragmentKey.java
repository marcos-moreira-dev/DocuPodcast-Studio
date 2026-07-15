package com.marcosmoreiradev.docupodcaststudio.presentation.document;

/** Exact sentence-level identity for the visual fragment pinned in the CDoc. */
public record DocumentVisualFragmentKey(
        String unitId,
        String segmentId,
        String blockId,
        int startOffset,
        int endOffset) {

    public DocumentVisualFragmentKey {
        unitId = normalize(unitId);
        segmentId = normalize(segmentId);
        blockId = normalize(blockId);
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
    }

    public static DocumentVisualFragmentKey empty() {
        return new DocumentVisualFragmentKey("", "", "", 0, 0);
    }

    public static DocumentVisualFragmentKey from(DocumentFragmentRailPresentation fragment) {
        if (fragment == null) {
            return empty();
        }
        return new DocumentVisualFragmentKey(
                fragment.unitId(),
                fragment.segmentId(),
                fragment.blockId(),
                fragment.startOffset(),
                fragment.endOffset());
    }

    public boolean emptyKey() {
        return unitId.isBlank() && segmentId.isBlank();
    }

    public boolean matchesUnit(String candidateUnitId) {
        return !unitId.isBlank() && unitId.equals(normalize(candidateUnitId));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
