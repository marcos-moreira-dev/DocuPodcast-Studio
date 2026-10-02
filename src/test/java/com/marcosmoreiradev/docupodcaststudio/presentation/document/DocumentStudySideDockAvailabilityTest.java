package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudySideDockAvailabilityTest {
    @Test
    void keepsVideoModuleVisibleForDocumentaryPdfProjects() {
        assertTrue(DocumentStudySideDock.supportsVideoModule(ProjectMode.DOCUMENTARY_STUDIO));
    }

    @Test
    void doesNotPublishDocumentaryVideoModuleInOtherProductModes() {
        assertFalse(DocumentStudySideDock.supportsVideoModule(ProjectMode.THEATRE_PRODUCTION));
        assertFalse(DocumentStudySideDock.supportsVideoModule(ProjectMode.NARRATIVE_VIDEO));
    }
}
