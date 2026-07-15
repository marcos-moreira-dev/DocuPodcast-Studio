package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HumanEngineConfigurationT89SourceTest {
    @Test
    void settingsUsesRealPreflightForVisibleProductEnginesOnly() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String preflight = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/InspectAiEnginesPreflightUseCase.java"));
        String docs = Files.readString(Path.of("docs/productizacion/T89_CONFIGURACION_HUMANA_MOTORES.md"));

        assertTrue(settings.contains("inspectAiEnginesPreflight"));
        assertTrue(settings.contains("engineOperationalSummary"));
        assertTrue(settings.contains("Resumen operativo"));
        assertFalse(settings.contains("page.addNode(engineStatusCard"));
        assertTrue(settings.contains("Voz IA avanzada"));
        assertTrue(settings.contains("Voz local simple"));
        assertTrue(settings.contains("video local"));
        assertFalse(settings.contains("Whisper"));
        assertFalse(settings.contains("Audio a texto"));
        assertFalse(settings.contains("new ModelInstallAssistantView"));
        assertFalse(preflight.contains("inspectWhisper"));
        assertTrue(docs.contains("no muestra botones falsos"));
        assertTrue(docs.contains("Coqui/XTTS"));
        assertTrue(docs.contains("Piper"));
        assertTrue(docs.contains("FFmpeg"));
    }

    @Test
    void modelAssistantDoesNotCreateDecorativeButtons() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));

        assertFalse(view.contains("new Button"));
        assertFalse(view.contains("ui-model-action-button"));
        assertFalse(css.contains("ui-model-action-button"));
    }
}
