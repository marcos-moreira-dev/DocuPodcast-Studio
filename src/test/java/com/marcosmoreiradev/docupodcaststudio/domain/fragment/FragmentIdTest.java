package com.marcosmoreiradev.docupodcaststudio.domain.fragment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FragmentIdTest {
    @Test
    void derivesStableIdsFromDocumentBlocksAndSegments() {
        assertEquals("FRG-B001", FragmentId.fromBlockId(" B001 ").value());
        assertEquals("FRG-SEG-SEG-001", FragmentId.fromSegmentId("SEG-001").value());
        assertEquals("FRG-B001", FragmentId.fromBlockId("FRG-B001").value());
    }

    @Test
    void rejectsBlankOrWhitespaceTokens() {
        assertThrows(IllegalArgumentException.class, () -> FragmentId.of(""));
        assertThrows(IllegalArgumentException.class, () -> FragmentId.fromBlockId("B 001"));
        assertThrows(IllegalArgumentException.class, () -> FragmentId.fromSegmentId("SEG 001"));
    }
}
