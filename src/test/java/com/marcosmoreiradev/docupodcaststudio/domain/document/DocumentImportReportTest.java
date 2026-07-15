package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentImportReportTest {
    @Test
    void countsWarningsAndErrors() {
        DocumentImportReport report = new DocumentImportReport(List.of(
                DocumentImportIssue.info("INFO", "Mensaje informativo"),
                DocumentImportIssue.warning("WARN", "Advertencia"),
                DocumentImportIssue.error("ERR", "Error")
        ));

        assertTrue(report.hasIssues());
        assertTrue(report.hasWarnings());
        assertTrue(report.hasErrors());
        assertEquals(1, report.warningCount());
        assertEquals(1, report.errorCount());
    }
}
