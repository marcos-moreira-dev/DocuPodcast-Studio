package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF12: las acciones de muestras de Voces salen del workspace principal sin cambiar UX. */
final class PresentationOrchestrationRf12VoiceSampleActionsSourceTest {
    @Test
    void voiceWorkspaceDelegatesSampleActionsToDedicatedCoordinator() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String engineControls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java");

        assertTrue(view.contains("VoiceSampleActions"));
        assertTrue(view.contains("VoiceEngineSettingsControls"));
        assertTrue(view.contains("sampleActions.actionGroups(includeExport)"));
        assertTrue(actions.contains("chooseAndImportOwnVoiceSample"));
        assertTrue(actions.contains("startOwnVoiceRecording"));
        assertTrue(actions.contains("deleteSelectedToneSample"));
        assertTrue(engineControls.contains("selectionSection"));
        assertTrue(engineControls.contains("saveComputeDeviceSelection"));
        assertFalse(view.contains("new FileChooser()"));
        assertFalse(view.contains("viewModel.importVoiceSample(voice, file.toPath(), plan.tone())"));
    }

    @Test
    void voiceWorkspaceKeepsTransitionalLineBudget() throws Exception {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java")
                .lines()
                .count();

        assertTrue(lines <= 1100, "VoiceLibraryWorkspaceView debe quedar en <= 1100 líneas tras RF12; actual=" + lines);
    }

    @Test
    void refactorLogRecordsRf12Step() throws Exception {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF12-01"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF12"));
        assertTrue(audit.contains("Avance RF12"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
