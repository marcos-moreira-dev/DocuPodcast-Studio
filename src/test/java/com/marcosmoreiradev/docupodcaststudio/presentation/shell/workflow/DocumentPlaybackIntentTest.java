package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPlaybackIntentTest {
    @Test
    void fullDocumentIntentRequestsPlaybackWithoutForcingAStartSegment() {
        DocumentPlaybackIntent intent = DocumentPlaybackIntent.fromBeginning();

        assertEquals(DocumentAudioAction.FAST_LISTEN, intent.action());
        assertEquals(DocumentProcessingScope.FULL_DOCUMENT, intent.scope());
        assertTrue(intent.autoPlay());
        assertEquals("", intent.preferredSegmentId());
        assertFalse(intent.singleCue());
    }

    @Test
    void selectedIntentNormalizesItsSegmentAndCanLimitPlaybackToOneCue() {
        DocumentPlaybackIntent intent = DocumentPlaybackIntent.fromSegment("  SEG-002  ", true);

        assertEquals(DocumentProcessingScope.SINGLE_FRAGMENT, intent.scope());
        assertTrue(intent.autoPlay());
        assertEquals("SEG-002", intent.preferredSegmentId());
        assertTrue(intent.singleCue());
    }

    @Test
    void generationOnlyIntentCannotRetainSingleCuePlayback() {
        DocumentPlaybackIntent intent = new DocumentPlaybackIntent(false, "SEG-003", true);

        assertFalse(intent.autoPlay());
        assertFalse(intent.singleCue());
    }
}
