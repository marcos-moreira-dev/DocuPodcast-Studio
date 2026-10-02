package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentProcessingIntervalPolicyTest {
    private final DocumentProcessingIntervalPolicy policy =
            new DocumentProcessingIntervalPolicy();

    @Test
    void initialIntervalUsesCurrentPageAndOnePageLookAhead() {
        assertEquals(4, policy.initial(4, 10).start());
        assertEquals(5, policy.initial(4, 10).end());
        assertEquals(10, policy.initial(10, 10).start());
        assertEquals(10, policy.initial(10, 10).end());
    }

    @Test
    void changingStartPreservesValidEndAndRepairsOnlyInvalidEnd() {
        assertEquals(8, policy.afterStartChange(3, 8, 10).end());
        var repaired = policy.afterStartChange(7, 3, 10);
        assertEquals(7, repaired.start());
        assertEquals(8, repaired.end());
        var atEnd = policy.afterStartChange(10, 3, 10);
        assertEquals(10, atEnd.start());
        assertEquals(10, atEnd.end());
    }
}
