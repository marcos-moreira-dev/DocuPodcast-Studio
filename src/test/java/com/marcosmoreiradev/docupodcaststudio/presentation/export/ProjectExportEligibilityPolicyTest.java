package com.marcosmoreiradev.docupodcaststudio.presentation.export;

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
    void pdfDocumentsCannotOpenExportCenter() {
        ReadableDocument document = new ReadableDocument("Libro", SourceDocumentFormat.PDF, Path.of("libro.pdf"), List.of());

        ProjectExportEligibilityPolicy.ExportEligibility eligibility = policy.evaluate(document);

        assertFalse(eligibility.eligible());
        assertTrue(eligibility.message().contains("Los PDF quedan para lectura"));
    }

    @Test
    void docxDocumentsCanOpenExportCenter() {
        ReadableDocument document = new ReadableDocument("Guion", SourceDocumentFormat.DOCX, Path.of("guion.docx"), List.of());

        ProjectExportEligibilityPolicy.ExportEligibility eligibility = policy.evaluate(document);

        assertTrue(eligibility.eligible());
    }
}
