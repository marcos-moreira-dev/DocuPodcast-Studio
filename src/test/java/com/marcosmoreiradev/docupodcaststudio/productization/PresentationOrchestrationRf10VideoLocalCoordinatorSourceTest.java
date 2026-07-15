package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF10: Video local/FFmpeg deja de vivir como orquestación larga dentro de SettingsDialog. */
final class PresentationOrchestrationRf10VideoLocalCoordinatorSourceTest {
    @Test
    void settingsDialogDelegatesVideoLocalOperationsToDomainCoordinator() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java");

        assertTrue(dialog.contains("private final VideoLocalSettingsOperations videoLocalOperations"));
        assertTrue(dialog.contains("videoLocalOperations.verify"));
        assertTrue(dialog.contains("videoLocalOperations.confirmAndPrepare"));
        assertTrue(dialog.contains("videoLocalOperations.downloadPortableRuntime"));
        assertTrue(dialog.contains("videoLocalOperations.importRuntimeFolder"));
        assertFalse(dialog.contains("DownloadFfmpegPortableRuntimeUseCase"));
        assertFalse(dialog.contains("ImportFfmpegRuntimeFolderUseCase"));
        assertFalse(dialog.contains("FfmpegRuntimeDownloadReport"));
        assertFalse(dialog.contains("FfmpegRuntimeImportReport"));
        assertFalse(dialog.contains("EmbeddedFfmpegLocator"));

        assertTrue(coordinator.contains("DownloadFfmpegPortableRuntimeUseCase"));
        assertTrue(coordinator.contains("ImportFfmpegRuntimeFolderUseCase"));
        assertTrue(coordinator.contains("EmbeddedFfmpegLocator"));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"preparando-video-local\""));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"importando-video-local\""));
    }

    @Test
    void settingsDialogStaysBelowNewRf10Limit() throws IOException {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java")
                .lines()
                .count();
        assertTrue(lines <= 980, "SettingsDialog debe quedar en <= 980 líneas tras RF10; actual=" + lines);
    }

    @Test
    void rf10AndRibbonTextVideoFeatureAreRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF10-01: coordinador de Video local y FFmpeg"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF10"));
        assertTrue(audit.contains("Avance RF10"));
        assertTrue(bitacora.contains("Ribbon `Exportar`"));
        assertTrue(bitacora.contains("imagen de fondo opcional"));
        assertTrue(bitacora.contains("borde sólido"));
        assertTrue(bitacora.contains("sombreado"));
        assertTrue(bitacora.contains("región del frame"));
        assertTrue(bitacora.contains("sidebar derecho"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
