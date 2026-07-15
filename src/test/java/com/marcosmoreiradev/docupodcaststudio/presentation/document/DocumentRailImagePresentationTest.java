package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentRailImagePresentationTest {
    @Test
    void labelsAssignedAndUnassignedImagesForTheDocumentRail() {
        DocumentRailImagePresentation unassigned = new DocumentRailImagePresentation(
                "IMG-001", "bosque.png", "media/images/bosque.png", "file:///tmp/bosque.png", "", "", "");
        assertFalse(unassigned.assignedToText());
        assertEquals("Sin texto asignado", unassigned.assignmentLabel());
        assertTrue(unassigned.previewLabel().contains("Selecciona texto"));

        DocumentRailImagePresentation assigned = new DocumentRailImagePresentation(
                "IMG-002", "casa.png", "media/images/casa.png", "file:///tmp/casa.png", "SEG-004", "BLK-010", "La casa estaba vacía.");
        assertTrue(assigned.assignedToText());
        assertEquals("Asignada a SEG-004", assigned.assignmentLabel());
        assertEquals("La casa estaba vacía.", assigned.previewLabel());
    }
}
