package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentLayerAssignmentPresentationTest {
    @Test
    void presentsProjectLayerWithoutWritingIntoWord() {
        NarrativeLayerAssignment assignment = new NarrativeLayerAssignment(
                "NLA-VOICE-SEG-001-0-20",
                NarrativeLayerKind.VOICE,
                new ScriptTextRange("SEG-001", 0, 20),
                new DocumentTextRange("BLK-001", 5, 25),
                "VOC-NARRATOR",
                "Narrador prediseñado",
                "Guardada como capa del proyecto");

        DocumentLayerAssignmentPresentation presentation = DocumentLayerAssignmentPresentation.from(assignment);

        assertEquals("Voz principal", presentation.kindLabel());
        assertEquals("Narrador prediseñado", presentation.displayName());
        assertTrue(presentation.primaryLayer());
        assertTrue(presentation.rangeLabel().contains("BLK-001"));
        assertTrue(presentation.cssClass().contains("primary"));
    }
}
