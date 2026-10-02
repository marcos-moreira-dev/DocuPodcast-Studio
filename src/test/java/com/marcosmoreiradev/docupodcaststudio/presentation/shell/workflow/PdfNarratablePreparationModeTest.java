package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationPriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNarratablePreparationModeTest {
    @Test
    void fastListenStartsOnFirstNarratablePageWithoutSecondaryEnrichment() {
        var mode = PdfNarratablePreparationMode.FAST_LISTEN;

        assertEquals(PdfPreparationPriority.URGENT, mode.priority());
        assertTrue(mode.startsWhenFirstPageIsNarratable());
        assertFalse(mode.requiresSecondarySemanticPreparation());
    }

    @Test
    void completeBatchWaitsForSecondaryContentAtNonInteractivePriority() {
        var mode = PdfNarratablePreparationMode.COMPLETE_BATCH;

        assertEquals(PdfPreparationPriority.NORMAL, mode.priority());
        assertFalse(mode.startsWhenFirstPageIsNarratable());
        assertTrue(mode.requiresSecondarySemanticPreparation());
    }

    @Test
    void lookAheadUsesTheSameSchedulerAtBackgroundPriority() {
        var mode = PdfNarratablePreparationMode.BACKGROUND_LOOK_AHEAD;

        assertEquals(PdfPreparationPriority.BACKGROUND, mode.priority());
        assertTrue(mode.background());
        assertFalse(mode.requiresSecondarySemanticPreparation());
    }
}
