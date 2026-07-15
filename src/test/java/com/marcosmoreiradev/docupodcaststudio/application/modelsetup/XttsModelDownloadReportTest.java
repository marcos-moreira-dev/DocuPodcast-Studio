package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsModelDownloadReportTest {
    @Test
    void failedReportKeepsDiagnosticPathForSupport() {
        Path target = Path.of("models/tts/xtts");
        Path diagnostics = target.resolve("download-diagnostics.txt");

        XttsModelDownloadReport report = XttsModelDownloadReport.failed(
                target,
                "No se pudo descargar Voz IA avanzada.",
                List.of("archivo principal de voz (HTTP 404)"),
                diagnostics);

        assertEquals(diagnostics, report.diagnosticReport());
        assertEquals(List.of("archivo principal de voz (HTTP 404)"), report.failedFiles());
    }

    @Test
    void diagnosticReportPathLivesNextToTheDownloadedModel() {
        Path target = Path.of("C:/DocuPodcast/models/tts/xtts");
        Path diagnostics = DownloadXttsOfficialModelUseCase.diagnosticReportPath(target);

        assertTrue(diagnostics.toString().endsWith("models/tts/xtts/download-diagnostics.txt")
                || diagnostics.toString().endsWith("models\\tts\\xtts\\download-diagnostics.txt"));
    }
}
