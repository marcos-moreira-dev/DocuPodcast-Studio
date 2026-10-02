package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentWorkspaceSelectionTest {
    @Test
    void treatsPdfRegionAsARealDocumentSelection() {
        assertTrue(DocumentWorkspaceView.hasDocumentSelection(
                "", null, new PdfRegionSelectionRef(1, "P000001-R-1", 0, 10)));
    }

    @Test
    void reportsNoSelectionWhenAllSelectionKindsAreEmpty() {
        assertFalse(DocumentWorkspaceView.hasDocumentSelection("", null, null));
    }
}
