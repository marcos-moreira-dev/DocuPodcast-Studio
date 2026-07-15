package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectPdfSourcePolicyTest {
    private final ProjectPdfSourcePolicy policy = new ProjectPdfSourcePolicy();

    @Test
    void documentaryStudioTreatsPdfAsFirstClassVisualStudySource() {
        ProjectPdfSourcePolicy.PdfSourceUse use = policy.resolve(ProjectMode.DOCUMENTARY_STUDIO);

        assertTrue(use.visualRenderingAvailable());
        assertTrue(use.advancedStudyWorkflow());
        assertFalse(use.recommendEditableScriptSource());
        assertTrue(use.guidance().contains("seleccion por regiones"));
    }

    @Test
    void creativeModesKeepPdfAsReadOnlyReferenceAndRecommendEditableScripts() {
        ProjectPdfSourcePolicy.PdfSourceUse theatre = policy.resolve(ProjectMode.THEATRE_PRODUCTION);
        ProjectPdfSourcePolicy.PdfSourceUse narrative = policy.resolve(ProjectMode.NARRATIVE_VIDEO);

        assertTrue(theatre.visualRenderingAvailable());
        assertFalse(theatre.advancedStudyWorkflow());
        assertTrue(theatre.recommendEditableScriptSource());
        assertTrue(theatre.guidance().contains("DOCX o Markdown"));

        assertTrue(narrative.visualRenderingAvailable());
        assertFalse(narrative.advancedStudyWorkflow());
        assertTrue(narrative.recommendEditableScriptSource());
        assertTrue(narrative.guidance().contains("DOCX o Markdown"));
    }
}
