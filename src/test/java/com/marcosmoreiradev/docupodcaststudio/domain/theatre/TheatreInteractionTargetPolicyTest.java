package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheatreInteractionTargetPolicyTest {
    @Test void legacyGroupAliasesBecomeTheCanonicalCollectiveTarget() {
        assertEquals(TheatreInteractionTargetPolicy.ALL_REMAINING,
                TheatreInteractionTargetPolicy.canonicalize("GRUPO"));
        assertEquals(TheatreInteractionTargetPolicy.ALL_REMAINING,
                TheatreInteractionTargetPolicy.canonicalize("todos_los_presentes"));
        assertTrue(TheatreInteractionTargetPolicy.isAllRemaining("El grupo"));
    }

    @Test void publicAndNamedCharactersRemainDistinctTargets() {
        assertEquals("PUBLICO", TheatreInteractionTargetPolicy.canonicalize("PUBLICO"));
        assertEquals("CONCHA", TheatreInteractionTargetPolicy.canonicalize("CONCHA"));
        assertFalse(TheatreInteractionTargetPolicy.isAllRemaining("PUEBLO"));
    }
}
