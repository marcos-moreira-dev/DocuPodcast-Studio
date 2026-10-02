package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Validates settings without making the reader screen technical. */
public final class ValidateOperationalSettingsUseCase {
    public OperationalSettingsValidationReport validate(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ArrayList<String> errors = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<String> info = new ArrayList<>();

        validatePlayback(current, warnings, info);
        validateTts(current.tts(), warnings, info);
        validateVideo(current.video(), warnings, info);
        validateCompute(current.compute(), warnings, info);
        validateOcr(current.ocr(), warnings, info);
        validateStorage(current.storage(), warnings, info);

        info.add("Configuración operativa cargada; la pantalla Documento conserva la lectura simple.");
        return new OperationalSettingsValidationReport(errors.isEmpty(), errors, warnings, info);
    }

    private static void validatePlayback(OperationalSettings current, List<String> warnings, List<String> info) {
        var buffer = current.playbackBuffer();
        info.add("Buffer: inicia con " + buffer.initialReadySegments() + " fragmentos y mantiene " + buffer.lookaheadSegments() + " por delante.");
        if (buffer.lookaheadSegments() < buffer.initialReadySegments()) {
            warnings.add("El lookahead no debe ser menor que el buffer inicial.");
        }
    }

    private static void validateTts(OperationalSettings.TtsEngineSettings tts, List<String> warnings, List<String> info) {
        if (tts.externalProcessRequested() && tts.commandTemplate().isBlank()) {
            warnings.add("TTS externo solicitado sin línea de comandos; se usará diagnóstico/mock hasta configurar motor local.");
        } else if (!tts.commandTemplate().isBlank()) {
            info.add("TTS externo configurado como proceso local: " + tts.displayName());
        } else {
            info.add("TTS mock/diagnóstico activo para pruebas sin depender de modelos.");
        }
    }

    private static void validateVideo(OperationalSettings.VideoRenderSettings video, List<String> warnings, List<String> info) {
        if (!video.ffmpegExecutable().isBlank()) {
            verifyPath(video.ffmpegExecutable(), "FFmpeg", warnings);
            info.add("FFmpeg configurado para render de video simple en " + video.resolutionPreset() + ".");
        } else if (video.preferEmbeddedFfmpeg()) {
            warnings.add("FFmpeg se buscará embebido en tools/ffmpeg; si no existe se generará paquete renderizable/auditable.");
        }
    }

    private static void validateCompute(OperationalSettings.ComputeSettings compute, List<String> warnings, List<String> info) {
        if (compute.policy() == com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.SPECIFIC_DEVICE
                && compute.selectedDeviceId().isBlank()) {
            warnings.add("Se eligió dispositivo específico, pero no hay identificador de CPU/GPU configurado.");
        }
        if (compute.policy().canUseGpu()
                && !compute.allowGpuForTts()
                && !compute.allowGpuForVideo()
                && !compute.allowGpuForContentAnalysis()) {
            warnings.add("La política de cómputo permite GPU, pero voz, video y análisis documental "
                    + "tienen GPU desactivada.");
        }
        if (compute.videoEncoderPolicy().hardwareAccelerated() && !compute.allowGpuForVideo()) {
            warnings.add("El encoder de video solicita GPU, pero el uso de GPU para video está desactivado.");
        }
        info.add("Dispositivo de inferencia: " + compute.policy().label()
                + " · Voz GPU=" + yesNo(compute.allowGpuForTts())
                + " · Video GPU=" + yesNo(compute.allowGpuForVideo())
                + " · Análisis GPU=" + yesNo(compute.allowGpuForContentAnalysis())
                + " · RAM de apoyo=" + yesNo(compute.allowRamOffloadForContentAnalysis()) + ".");
        info.add("Encoder video: " + compute.videoEncoderPolicy().label() + ".");
    }

    private static void validateStorage(OperationalSettings.StorageSettings storage, List<String> warnings, List<String> info) {
        if (storage.modelsDirectory().isBlank()) {
            warnings.add("La carpeta de modelos no debe quedar vacía.");
        }
        if (storage.exportsDirectory().isBlank()) {
            warnings.add("La carpeta de exportaciones no debe quedar vacía.");
        }
        info.add("Modelos: " + storage.modelsDirectory() + " · Exportaciones: " + storage.exportsDirectory());
    }

    private static void validateOcr(OperationalSettings.OcrSettings ocr, List<String> warnings, List<String> info) {
        if (!ocr.tesseractExecutable().isBlank()) {
            verifyPath(ocr.tesseractExecutable(), "Tesseract OCR", warnings);
        } else {
            warnings.add("OCR PDF buscara Tesseract en tools/tesseract/bin o PATH.");
        }
        info.add("OCR PDF: " + ocr.engineMode() + " idiomas=" + ocr.languages() + " dpi=" + ocr.dpi() + ".");
    }

    private static String yesNo(boolean value) {
        return value ? "sí" : "no";
    }

    private static void verifyPath(String text, String label, List<String> warnings) {
        if (text == null || text.isBlank()) {
            return;
        }
        Path path = Path.of(text);
        if ((text.contains("/") || text.contains("\\") || text.endsWith(".exe")) && !Files.exists(path)) {
            warnings.add("No se encontró " + label + ": " + text);
        }
    }
}
