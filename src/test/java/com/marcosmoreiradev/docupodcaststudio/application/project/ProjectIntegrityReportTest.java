package com.marcosmoreiradev.docupodcaststudio.application.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectIntegrityReportTest {
    @Test
    void statusIsOkWithoutIssues() {
        ProjectIntegrityReport report = ProjectIntegrityReport.from("Proyecto", List.of());

        assertEquals(ProjectIntegrityStatus.OK, report.status());
        assertTrue(report.ok());
        assertFalse(report.requiresRepair());
    }

    @Test
    void warningsDoNotBecomeBlockingRepair() {
        ProjectIntegrityReport report = ProjectIntegrityReport.from("Proyecto", List.of(
                ProjectIntegrityIssue.warning("AUDIO_JOB_RESUMABLE", "JOB-001", "Job reanudable", "Reanudar")
        ));

        assertEquals(ProjectIntegrityStatus.CON_ADVERTENCIAS, report.status());
        assertTrue(report.withWarnings());
        assertFalse(report.requiresRepair());
        assertEquals(1, report.warningCount());
    }

    @Test
    void errorsRequireRepair() {
        ProjectIntegrityReport report = ProjectIntegrityReport.from("Proyecto", List.of(
                ProjectIntegrityIssue.error("ASSET_FILE_MISSING", "IMG-001", "Falta imagen", "Reimportar")
        ));

        assertEquals(ProjectIntegrityStatus.REQUIERE_REPARACION, report.status());
        assertTrue(report.requiresRepair());
        assertEquals(1, report.errorCount());
        assertTrue(report.summary().contains("requiere reparación"));
    }
}
