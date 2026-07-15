package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF11: la configuración inicial de primer uso deja de vivir como orquestación larga en SettingsDialog. */
final class PresentationOrchestrationRf11InitialSetupCoordinatorSourceTest {
    @Test
    void settingsDialogDelegatesInitialSetupToDomainCoordinator() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java");

        assertTrue(dialog.contains("private final InitialSetupSettingsOperations initialSetupOperations"));
        assertTrue(dialog.contains("initialSetupOperations.prompt"));
        assertTrue(dialog.contains("initialSetupOperations.run"));
        assertFalse(dialog.contains("DownloadPiperPortableRuntimeUseCase"));
        assertFalse(dialog.contains("PiperRuntimeDownloadReport"));

        assertTrue(coordinator.contains("DownloadPiperPortableRuntimeUseCase"));
        assertTrue(coordinator.contains("PiperRuntimeDownloadReport"));
        assertTrue(coordinator.contains("RuntimePathResolver.defaultResolver().resolve().applicationRoot()"));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"configuracion-inicial-voz-local-simple\""));
        assertTrue(coordinator.contains("piperOperations.selectAndSave(form, services)"));
    }

    @Test
    void settingsDialogStaysBelowNewRf11Limit() throws IOException {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java")
                .lines()
                .count();
        assertTrue(lines <= 920, "SettingsDialog debe quedar en <= 920 líneas tras RF11; actual=" + lines);
    }

    @Test
    void rf11IsRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF11-01: coordinador de configuración inicial"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF11"));
        assertTrue(audit.contains("Avance RF11"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
