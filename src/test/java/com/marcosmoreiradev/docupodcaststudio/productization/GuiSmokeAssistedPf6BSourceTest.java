package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF6B starts the real GUI smoke as an assisted in-app checklist, not a placeholder. */
final class GuiSmokeAssistedPf6BSourceTest {
    @Test
    void applicationBuildsGuiSmokeChecklistReport() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildGuiSmokeChecklistUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/GuiSmokeChecklistReport.java"));
        assertTrue(useCase.contains("PF6B_GUI_SMOKE_CHECKLIST.md"));
        assertTrue(useCase.contains("Vista / Voces"));
        assertTrue(useCase.contains("Generar prueba de voz real"));
        assertTrue(useCase.contains("Documento / Playbar"));
        assertTrue(report.contains("readyForManualGuiSmoke"));
    }

    @Test
    void guiSmokeChecklistStaysAsReleaseSupportNotNormalSettings() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String script = Files.readString(Path.of("scripts/36-smoke-gui-asistido.bat"));
        String rc = Files.readString(Path.of("scripts/16-release-candidate.bat"));
        assertFalse(settings.contains("Smoke real desde GUI"));
        assertFalse(settings.contains("Generar checklist smoke GUI"));
        assertTrue(script.contains("PF6B_GUI_SMOKE_CHECKLIST.md"));
        assertTrue(rc.contains("36-smoke-gui-asistido.bat"));
    }
}
