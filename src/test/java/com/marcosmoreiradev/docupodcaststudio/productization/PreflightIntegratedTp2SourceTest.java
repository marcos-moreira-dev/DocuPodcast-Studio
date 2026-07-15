package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreflightIntegratedTp2SourceTest {
    @Test
    void tp2AddsHumanPreflightSummaryAndSettingsIntegration() throws Exception {
        String summaryUseCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/BuildHumanEnginePreflightSummaryUseCase.java");
        String summary = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/HumanEnginePreflightSummary.java");
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/HumanEnginePreflightState.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");

        assertTrue(summaryUseCase.contains("fromAiEngineReport"));
        assertTrue(summaryUseCase.contains("fromStartupReport"));
        assertTrue(summary.contains("statusBarLabel"));
        assertTrue(state.contains("REQUIRES_PREPARATION"));
        assertTrue(services.contains("AppStartupEnginePreflightUseCase"));
        assertTrue(services.contains("BuildHumanEnginePreflightSummaryUseCase"));
        assertTrue(settings.contains("Estado"));
        assertTrue(settings.contains("Siguiente paso"));
        assertTrue(settings.contains("preflight integrado") || settings.contains("HumanEnginePreflightSummary"));
    }

    @Test
    void tp2DocumentsProductizationStepAndKeepsHistoricalSourceGuards() throws Exception {
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");
        String validation = read("VALIDATION.md");
        String registry = read("docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md");

        assertTrue(readme.contains("Base vigente: TP2"));
        assertTrue(readme.contains("T97 — Componentes GUI transversales"));
        assertTrue(handoff.contains("Base vigente: TP2"));
        assertTrue(handoff.contains("T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));
        assertTrue(validation.contains("Validación TP2"));
        assertTrue(validation.contains("Piper genera un WAV real"));
        assertTrue(registry.contains("TP2 — Preflight integrado"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
