package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DIAGNOSTIC-TO-UI-HF1 makes diagnostic output visible as an operational decision, not only a status message. */
final class DiagnosticToUiHf1SourceTest {
    @Test
    void diagnosticReportExportShowsUserVisibleDecision() throws Exception {
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DiagnosticUserDecisionFactory.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(factory.contains("diagnosticReportExported"));
        assertTrue(factory.contains("Reporte diagnóstico exportado"));
        assertTrue(factory.contains("DecisionSeverity.INFORMATION"));
        assertTrue(factory.contains("true);"));
        assertTrue(factory.contains("estado técnico, motores, audio, visuales y exportación"));
        assertTrue(shell.contains("DiagnosticUserDecisionFactory.diagnosticReportExported"));
        assertTrue(shell.contains("alertPresenter.showDecision"));
    }
}
