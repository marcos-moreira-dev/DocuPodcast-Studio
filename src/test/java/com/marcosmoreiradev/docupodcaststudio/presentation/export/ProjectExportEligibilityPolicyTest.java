package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectExportEligibilityPolicyTest {
    private final ProjectExportEligibilityPolicy policy = new ProjectExportEligibilityPolicy();

    @Test
    void preparedPdfCanOpenExportCenterWhenItsProjectionExists() {
        PreparedPdfSource source = new PreparedPdfSource(new PreparedPdfWorkspaceRef(
                Path.of("proyecto"), Path.of("proyecto/source/libro.pdf"), "a".repeat(64)), "Libro");

        ProjectExportEligibilityPolicy.ExportEligibility eligibility = policy.evaluate(source, true);

        assertTrue(eligibility.eligible());
    }

    @Test
    void preparedPdfWithoutProjectionExplainsTheMissingCapability() {
        PreparedPdfSource source = new PreparedPdfSource(new PreparedPdfWorkspaceRef(
                Path.of("proyecto"), Path.of("proyecto/source/libro.pdf"), "a".repeat(64)), "Libro");

        ProjectExportEligibilityPolicy.ExportEligibility eligibility = policy.evaluate(source, false);

        assertFalse(eligibility.eligible());
        assertTrue(eligibility.message().contains("proyección audiovisual"));
    }

    @Test
    void docxCanOpenExportCenter() {
        ReadableDocument document = new ReadableDocument("Guion", SourceDocumentFormat.DOCX,
                Path.of("guion.docx"), List.of());

        ProjectExportEligibilityPolicy.ExportEligibility eligibility =
                policy.evaluate(new BlockDocumentSource(document));

        assertTrue(eligibility.eligible());
    }
}
