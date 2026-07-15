package com.marcosmoreiradev.docupodcaststudio.application.engines;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Repo-local preflight for Piper and FFmpeg.
 *
 * <p>The product must stay self-contained: Piper and FFmpeg are resolved from the application
 * folder, not from PATH. This use case is deliberately cheap and does not download artifacts.</p>
 */
public final class InspectLocalVoiceMediaToolsUseCase {
    public static final String DEFAULT_PIPER_EXE = "tools/piper/piper.exe";
    public static final String DEFAULT_PIPER_VOICES = "models/tts/piper/voices";
    public static final String DEFAULT_FFMPEG_EXE = "tools/ffmpeg/bin/ffmpeg.exe";
    public static final String DEFAULT_FFPROBE_EXE = "tools/ffmpeg/bin/ffprobe.exe";

    public LocalToolReadinessReport inspect(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        ArrayList<LocalToolReadinessItem> items = new ArrayList<>();
        Path piperExe = paths.piperExecutable();
        Path piperVoices = paths.piperVoicesDirectory();
        Path piperModel = firstPiperVoice(piperVoices);
        Path ffmpegExe = paths.ffmpegExecutable();
        Path ffprobeExe = paths.ffprobeExecutable();

        items.add(file("piper-exe", "Piper local", true, piperExe,
                "Falta Piper local para el fallback rápido de voz.",
                "Coloca piper.exe en tools\\piper\\piper.exe."));
        items.add(file("piper-model", "Voz Piper ONNX", true, piperModel,
                "Falta una voz Piper .onnx para el fallback rápido.",
                "Coloca una voz .onnx en models\\tts\\piper\\voices."));
        items.add(file("ffmpeg-exe", "FFmpeg local", true, ffmpegExe,
                "Falta FFmpeg local para preparar audio/video.",
                "Coloca ffmpeg.exe en tools\\ffmpeg\\bin\\ffmpeg.exe."));
        items.add(file("ffprobe-exe", "FFprobe local", false, ffprobeExe,
                "FFprobe no está disponible; se omite validación avanzada de media.",
                "Coloca ffprobe.exe en tools\\ffmpeg\\bin\\ffprobe.exe si deseas diagnóstico avanzado."));
        return new LocalToolReadinessReport(items);
    }

    private static LocalToolReadinessItem file(String id, String name, boolean required, Path path,
                                               String missingMessage, String action) {
        boolean exists = path != null && Files.isRegularFile(path);
        return new LocalToolReadinessItem(id, name, required,
                exists ? AiEngineReadinessStatus.READY : (required ? AiEngineReadinessStatus.MISSING_RUNTIME : AiEngineReadinessStatus.OPTIONAL_NOT_CONFIGURED),
                exists ? name + " listo." : missingMessage,
                exists ? "Sin acción pendiente." : action,
                path,
                List.of("Ruta repo-local esperada: " + path,
                        "No se usa PATH ni instalaciones globales para este componente."));
    }

    private static Path firstPiperVoice(Path voicesDir) {
        if (!Files.isDirectory(voicesDir)) {
            return voicesDir.resolve("es_ES-default-medium.onnx").normalize();
        }
        try (Stream<Path> stream = Files.walk(voicesDir, 3)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".onnx"))
                    .findFirst()
                    .orElse(voicesDir.resolve("es_ES-default-medium.onnx"))
                    .normalize();
        } catch (IOException ex) {
            return voicesDir.resolve("es_ES-default-medium.onnx").normalize();
        }
    }
}
