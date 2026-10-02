package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentAudioActionTest {
    @Test
    void fullDocumentListeningPreparesWordSemanticsWhileSelectionPlaybackStaysImmediate() {
        assertFalse(DocumentAudioAction.FAST_LISTEN.requiresWordSemanticPreparation());
        assertFalse(DocumentAudioAction.PLAY_SELECTION.requiresWordSemanticPreparation());
        assertFalse(DocumentAudioAction.FAST_LISTEN.forceGeneration());
    }

    @Test
    void processingActionsPrepareAuthorizedSecondaryContent() {
        assertTrue(DocumentAudioAction.PROCESS_COMPLETE.requiresWordSemanticPreparation());
        assertTrue(DocumentAudioAction.GENERATE_ALL.requiresWordSemanticPreparation());
        assertTrue(DocumentAudioAction.GENERATE_SELECTION.requiresWordSemanticPreparation());
        assertTrue(DocumentAudioAction.PROCESS_FRAGMENT.requiresWordSemanticPreparation());
        assertTrue(DocumentAudioAction.PROCESS_INTERVAL.requiresWordSemanticPreparation());
        assertFalse(DocumentAudioAction.PROCESS_COMPLETE.forceGeneration());
        assertTrue(DocumentAudioAction.PROCESS_FRAGMENT.forceGeneration());
        assertTrue(DocumentAudioAction.PROCESS_INTERVAL.forceGeneration());
    }
}
