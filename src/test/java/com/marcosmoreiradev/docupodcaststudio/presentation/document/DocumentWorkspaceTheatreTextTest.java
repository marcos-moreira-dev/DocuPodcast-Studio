package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentWorkspaceTheatreTextTest {
    @Test
    void restoresTheSpeakerInTheVisibleScriptWithoutChangingStoredSpeech() {
        DocumentBlock block = new DocumentBlock("B0001", DocumentBlockType.PARAGRAPH,
                "Vecinos, acérquense.", "theatre-dialogue", Map.of("characterName", "CONCHA"));

        assertEquals("CONCHA: Vecinos, acérquense.", DocumentWorkspaceView.theatreDisplayText(block));
        assertEquals("Vecinos, acérquense.", block.text());
    }

    @Test
    void leavesOrdinaryDocumentTextUntouched() {
        DocumentBlock block = new DocumentBlock("B0002", DocumentBlockType.PARAGRAPH,
                "Texto normal.", "Normal", Map.of("characterName", "CONCHA"));

        assertEquals("Texto normal.", DocumentWorkspaceView.theatreDisplayText(block));
    }
}
