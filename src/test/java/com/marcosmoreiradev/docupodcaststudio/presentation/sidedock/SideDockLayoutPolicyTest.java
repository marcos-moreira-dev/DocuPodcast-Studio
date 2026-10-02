package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SideDockLayoutPolicyTest {
    @Test
    void compactAndExpandedWidthsAreDeclaredOnce() {
        SideDockLayoutPolicy policy = SideDockLayoutPolicy.withCompact(84, 680, 980, 520, 700);
        assertEquals(84, policy.collapsed().pref());
        assertEquals(980, policy.expanded().pref());
        assertEquals(700, policy.compactExpanded().pref());
    }

    @Test
    void standardRailUsesTheReferenceRangeInsteadOfAFixedLegacyWidth() {
        SideDockLayoutPolicy policy = SideDockLayoutPolicy.standard(292, 318);
        assertEquals(74, policy.collapsed().min());
        assertEquals(78, policy.collapsed().pref());
        assertEquals(84, policy.collapsed().max());
    }

    @Test
    void invalidWidthOrderingFailsAtCompositionTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new SideDockLayoutPolicy.WidthRange(500, 400, 600));
    }
}
