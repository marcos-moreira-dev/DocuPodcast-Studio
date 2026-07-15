package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutomaticBrainSmokeSourceTest {
    @Test
    void automaticSmokeIsCoreScenarioAndNotManualUiChecklist() throws Exception {
        String smoke = read("src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/BrainSmokeScenarioTest.java");
        assertTrue(smoke.contains("DocumentSourceImportService"));
        assertTrue(smoke.contains("BuildNarrationScriptUseCase"));
        assertTrue(smoke.contains("MockAudioGenerationGateway"));
        assertTrue(smoke.contains("ProjectRoundTripUseCase"));
        assertTrue(smoke.contains("InspectProjectIntegrityUseCase"));
        assertTrue(smoke.contains("InspectExportReadinessUseCase"));
        assertTrue(smoke.contains("FileSystemProjectBundleExporter"));
        assertTrue(smoke.contains("ExportSimpleVideoPackageUseCase"));
        assertTrue(smoke.contains("target/docupodcast-smoke"));
        assertFalse(smoke.contains("import javafx"));
    }

    @Test
    void smokeScriptRunsFocusedScenarioAndWritesEvidence() throws Exception {
        String script = read("scripts/18-smoke-automatico-cerebro.bat");
        assertTrue(script.contains("-Dtest=BrainSmokeScenarioTest"));
        assertTrue(script.contains("target\\docupodcast-smoke\\SMOKE_REPORT.md"));
        assertTrue(script.contains("EnableDelayedExpansion"));
    }

    @Test
    void documentationKeepsT79AsBrainSmokeBeforeCpuGpuAndFrontend() throws Exception {
        String doc = read("docs/108_TANDA_79_SMOKE_AUTOMATICO_CEREBRO.md");
        assertTrue(doc.contains("sin JavaFX"));
        assertTrue(doc.contains("DOCX, TXT, Markdown y PDF nativo"));
        assertTrue(doc.contains("PDF escaneado"));
        assertTrue(doc.contains("T80A"));
        assertTrue(doc.contains("T80B"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }
}
