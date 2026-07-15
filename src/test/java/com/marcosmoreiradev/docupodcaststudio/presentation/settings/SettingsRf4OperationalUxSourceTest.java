package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF4: Configuración se organiza por intención operativa, no por scaffolding técnico. */
final class SettingsRf4OperationalUxSourceTest {
    @Test
    void sidebarContainsExactlySevenOperationalSections() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String enumBlock = between(dialog, "private enum SettingsSection", "private final String label");

        assertEquals(7, enumBlock.lines().filter(line -> line.contains("(\"")).count());
        assertTrue(enumBlock.contains("READING(\"Lectura\")"));
        assertTrue(enumBlock.contains("PLAYBACK(\"Reproducción\")"));
        assertTrue(enumBlock.contains("ENGINES(\"Motores y dependencias\")"));
        assertTrue(enumBlock.contains("PERFORMANCE(\"Rendimiento\")"));
        assertTrue(enumBlock.contains("VIDEO_FINAL(\"Video final\")"));
        assertTrue(enumBlock.contains("STORAGE(\"Almacenamiento\")"));
        assertTrue(enumBlock.contains("SUPPORT(\"Soporte y diagnóstico\")"));
        assertFalse(enumBlock.contains("AUDIO("));
        assertFalse(enumBlock.contains("TTS_VOICE("));
    }

    @Test
    void supportDiagnosticsExposeConcreteActionsThroughShellAdapter() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsSupportActions.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertTrue(dialog.contains("Soporte y diagnóstico"));
        assertTrue(dialog.contains("Validar proyecto"));
        assertTrue(dialog.contains("Ver estado de exportación"));
        assertTrue(dialog.contains("Exportar reporte de soporte"));
        assertTrue(dialog.contains("Exportar paquete de soporte"));
        assertTrue(dialog.contains("Abrir carpeta del proyecto"));
        assertTrue(dialog.contains("Abrir exportaciones"));
        assertTrue(actions.contains("available(AppCommandId commandId)"));
        assertTrue(shell.contains("SettingsSupportActions.of(commandDispatcher::canDispatch, this::dispatchCommand)"));
    }

    @Test
    void performanceUsesHumanLabelsAndDetectedEncoderChoices() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String formModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java");
        String performancePage = between(dialog, "private Node computeDevicePage", "private void prepareXttsPytorchCuda");

        assertTrue(performancePage.contains("applyComputePresentationLabels"));
        assertFalse(performancePage.contains("PREFER_GPU"));
        assertFalse(performancePage.contains("gpu-nvidia-0"));
        assertTrue(dialog.contains("humanComputePolicyLabel"));
        assertTrue(dialog.contains("humanComputeDeviceLabel"));
        assertTrue(dialog.contains("humanVideoEncoderLabel"));
        assertTrue(formModel.contains("deviceLooksLike(device, \"nvidia\")"));
        assertTrue(formModel.contains("deviceLooksLike(device, \"amd\")"));
        assertTrue(formModel.contains("videoEncoderPolicy.setValue(choices.contains(current) ? current : VideoEncoderPolicy.AUTO.name())"));
    }

    @Test
    void videoFinalDoesNotDuplicateFfmpegPreparationUrl() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String videoFinal = between(dialog, "private Node storyboardVideoPage", "private Node storagePage");
        String engines = between(dialog, "private Node ffmpegSetupActions", "private void confirmAndPrepareVideoLocal");

        assertFalse(videoFinal.contains("URL para preparar video local"));
        assertTrue(videoFinal.contains("La preparación de FFmpeg vive en Motores y dependencias"));
        assertTrue(engines.contains("URL para preparar video local"));
    }

    @Test
    void rf4IsRecordedInCurrentMarkdownLog() throws Exception {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");

        assertTrue(bitacora.contains("Paso RF4-01"));
        assertTrue(bitacora.contains("Configuración por intención operativa"));
        assertTrue(bitacora.contains("Soporte y diagnóstico"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF4"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }

    private static String between(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        int end = text.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "No se encontró marcador inicial: " + startMarker);
        assertTrue(end > start, "No se encontró marcador final: " + endMarker);
        return text.substring(start, end);
    }
}
