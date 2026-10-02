package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportControllerSegmentIdentityTest {

    @Test
    void acousticUnitBelongsToItsSelectedNarrationSegment() {
        Set<String> selected = Set.of("SEG-001", "SEG-094", "SEG-1344");

        assertTrue(ExportController.belongsToNarrationSelection("SEG-001-U001", selected));
        assertTrue(ExportController.belongsToNarrationSelection("SEG-094-U003", selected));
        assertTrue(ExportController.belongsToNarrationSelection("SEG-1344-U003", selected));
    }

    @Test
    void exactSegmentIdentityRemainsSupported() {
        assertTrue(ExportController.belongsToNarrationSelection("SEG-001", Set.of("SEG-001")));
    }

    @Test
    void unrelatedOrMalformedUnitDoesNotEnterTheSelection() {
        Set<String> selected = Set.of("SEG-001");

        assertFalse(ExportController.belongsToNarrationSelection("SEG-002-U001", selected));
        assertFalse(ExportController.belongsToNarrationSelection("SEG-001-USER", selected));
        assertFalse(ExportController.belongsToNarrationSelection("SEG-001-U", selected));
    }
}
