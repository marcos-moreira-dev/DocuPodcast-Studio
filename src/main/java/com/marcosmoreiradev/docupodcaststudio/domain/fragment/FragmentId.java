package com.marcosmoreiradev.docupodcaststudio.domain.fragment;

/** Stable cross-media identifier for the textual fragment that anchors document, audio, visual and theatre layers. */
public record FragmentId(String value) {
    private static final String BLOCK_PREFIX = "FRG-";
    private static final String SEGMENT_PREFIX = "FRG-SEG-";

    public FragmentId {
        value = token(value, "value");
    }

    public static FragmentId of(String value) {
        return new FragmentId(value);
    }

    public static FragmentId fromBlockId(String blockId) {
        String normalized = token(blockId, "blockId");
        return new FragmentId(normalized.startsWith(BLOCK_PREFIX) ? normalized : BLOCK_PREFIX + normalized);
    }

    public static FragmentId fromSegmentId(String segmentId) {
        String normalized = token(segmentId, "segmentId");
        return new FragmentId(normalized.startsWith(SEGMENT_PREFIX) ? normalized : SEGMENT_PREFIX + normalized);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}
