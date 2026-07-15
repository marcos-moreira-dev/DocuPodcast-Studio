package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T119 guards that Settings is an honest, actionable surface rather than a placeholder catalog. */
final class HonestActionableSettingsT119SourceTest {
    @Test
    void settingsShowsRealEngineActionsAndKeepsCommandLineAdvanced() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String videoLocal = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java"));
        assertTrue(dialog.contains("Voz IA avanzada") && dialog.contains("Verificar"));
        assertTrue(dialog.contains("selectXttsIfReady") || dialog.contains("Usar"));
        assertTrue(dialog.contains("Voz local simple") && dialog.contains("Verificar"));
        assertTrue(dialog.contains("selectPiperIfReady") || dialog.contains("Usar"));
        assertTrue(dialog.contains("Video local") && dialog.contains("Verificar"));
        assertTrue(videoLocal.contains("inspectFfmpegRuntime"));
        assertFalse(dialog.contains("Comando externo avanzado"));
        assertFalse(dialog.contains("La línea de comandos externa queda reservada para Diagnóstico avanzado"));
        assertTrue(dialog.contains("Configuración inicial"));
        assertFalse(dialog.contains("addFormRow(grid, 2, \"Comando externo\""),
                "El comando externo no debe estar en el formulario principal de TTS para usuario normal.");
    }

    @Test
    void catalogCardsAreMarkedAsInformativeGuideNotFakeButtons() throws Exception {
        String card = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineSetupCard.java"));
        assertTrue(card.contains("Guía informativa, no botón"));
        assertFalse(card.contains("Preparación sugerida:"));
    }
}
