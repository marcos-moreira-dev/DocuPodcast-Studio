package com.marcosmoreiradev.docupodcaststudio.application.engines;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cheap startup preflight for the local, self-contained voice/media runtime.
 *
 * <p>This use case intentionally checks only repository/app-local paths. Coqui/XTTS must never be
 * resolved from a global Python installation or the system PATH. A future GUI assistant can call
 * this before exposing voice generation and offer a guided setup when items are missing.</p>
 */
public final class AppStartupEnginePreflightUseCase {
    public static final String LOCAL_PYTHON = "tools/xtts-wrapper/.venv/Scripts/python.exe";
    public static final String XTTS_WRAPPER = "tools/xtts-wrapper/synthesize_xtts.py";
    public static final String XTTS_BRIDGE_SCRIPT = "scripts/tts/xtts-file-to-wav.ps1";
    public static final String DEFAULT_XTTS_MODEL_DIR = "models/tts/xtts";
    public static final String DEFAULT_XTTS_SPEAKER_WAV = "models/tts/xtts/speakers/voz-por-defecto.wav";
    public static final String DEFAULT_PIPER_EXE = "tools/piper/piper.exe";
    public static final String DEFAULT_FFMPEG_EXE = "tools/ffmpeg/bin/ffmpeg.exe";

    private final InspectAiEnginesPreflightUseCase enginePreflight;

    public AppStartupEnginePreflightUseCase() {
        this(new InspectAiEnginesPreflightUseCase());
    }

    public AppStartupEnginePreflightUseCase(InspectAiEnginesPreflightUseCase enginePreflight) {
        this.enginePreflight = enginePreflight == null ? new InspectAiEnginesPreflightUseCase() : enginePreflight;
    }

    public StartupEnginePreflightReport inspect(OperationalSettings settings, Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ArrayList<StartupEnginePreflightItem> items = new ArrayList<>();
        items.add(requiredFile(root, "coqui-python-local", "Python local Coqui/XTTS", paths.xttsPythonExecutable(),
                "Falta el Python local de Coqui/XTTS.",
                "Ejecuta scripts\\20-preparar-python-portable-coqui.bat. No instales Python global."));
        items.add(requiredFile(root, "coqui-wrapper", "Wrapper Coqui/XTTS", paths.xttsWrapperScript(),
                "Falta el wrapper de síntesis Coqui/XTTS.",
                "Restaura tools\\xtts-wrapper\\synthesize_xtts.py desde el repositorio."));
        items.add(requiredFile(root, "coqui-script", "Script puente Coqui/XTTS", paths.xttsPowerShellScript(),
                "Falta el script puente de Coqui/XTTS.",
                "Restaura scripts\\tts\\xtts-file-to-wav.ps1 desde el repositorio."));
        items.add(requiredFolder(root, "coqui-model", "Modelo local Coqui/XTTS", modelDir(root, current, paths),
                "Falta la carpeta local del modelo Coqui/XTTS.",
                "Coloca el modelo en models\\tts\\xtts o configura la carpeta local de modelos."));
        items.add(requiredFile(root, "coqui-speaker", "Voz por defecto", speakerWav(root, current, paths),
                "Falta la voz por defecto para Coqui/XTTS.",
                "Coloca models\\tts\\xtts\\speakers\\voz-por-defecto.wav."));
        items.add(optionalFile(root, "piper-exe", "Piper local", paths.piperExecutable(),
                "Piper no está disponible como fallback rápido.",
                "Coloca tools\\piper\\piper.exe y una voz ONNX si deseas fallback local."));
        items.add(optionalFile(root, "ffmpeg-exe", "FFmpeg local", paths.ffmpegExecutable(),
                "FFmpeg no está disponible para preparar audio/video.",
                "Coloca tools\\ffmpeg\\bin\\ffmpeg.exe."));

        AiEnginePreflightReport report = enginePreflight.inspect(current, root);
        items.add(new StartupEnginePreflightItem(
                "engine-preflight-summary",
                "Resumen de motores",
                false,
                report.fullAiDemoReady() ? AiEngineReadinessStatus.READY : AiEngineReadinessStatus.READY_WITH_WARNINGS,
                report.summary(),
                report.fullAiDemoReady() ? "Mantener evidencias de smoke." : "Completar los elementos faltantes antes del smoke real completo.",
                root,
                report.items().stream().map(item -> item.displayName() + ": " + item.status() + " — " + item.userMessage()).toList()
        ));
        return new StartupEnginePreflightReport(items);
    }

    private static StartupEnginePreflightItem requiredFile(Path root, String id, String name, String relativePath,
                                                           String missingMessage, String action) {
        return requiredFile(root, id, name, root.resolve(relativePath).normalize(), missingMessage, action);
    }

    private static StartupEnginePreflightItem requiredFile(Path root, String id, String name, Path path,
                                                           String missingMessage, String action) {
        boolean exists = Files.isRegularFile(path);
        return new StartupEnginePreflightItem(id, name, true,
                exists ? AiEngineReadinessStatus.READY : AiEngineReadinessStatus.MISSING_RUNTIME,
                exists ? name + " listo." : missingMessage,
                exists ? "Sin acción pendiente." : action,
                path,
                List.of("Ruta repo-local esperada: " + path, "Python global/PATH no se usa para Coqui/XTTS."));
    }

    private static StartupEnginePreflightItem requiredFolder(Path root, String id, String name, Path path,
                                                             String missingMessage, String action) {
        boolean exists = Files.isDirectory(path);
        return new StartupEnginePreflightItem(id, name, true,
                exists ? AiEngineReadinessStatus.READY : AiEngineReadinessStatus.MISSING_MODEL,
                exists ? name + " listo." : missingMessage,
                exists ? "Sin acción pendiente." : action,
                path,
                List.of("Ruta repo-local esperada: " + path));
    }

    private static StartupEnginePreflightItem optionalFile(Path root, String id, String name, String relativePath,
                                                           String missingMessage, String action) {
        return optionalFile(root, id, name, root.resolve(relativePath).normalize(), missingMessage, action);
    }

    private static StartupEnginePreflightItem optionalFile(Path root, String id, String name, Path path,
                                                           String missingMessage, String action) {
        boolean exists = Files.isRegularFile(path);
        return new StartupEnginePreflightItem(id, name, false,
                exists ? AiEngineReadinessStatus.READY : AiEngineReadinessStatus.OPTIONAL_NOT_CONFIGURED,
                exists ? name + " listo." : missingMessage,
                exists ? "Sin acción pendiente." : action,
                path,
                List.of("Ruta repo-local esperada: " + path));
    }

    private static Path modelDir(Path root, OperationalSettings settings, RuntimeArtifactPaths paths) {
        String configured = settings.storage().modelsDirectory() == null ? "" : settings.storage().modelsDirectory().strip();
        if (configured.isBlank()) {
            return paths.xttsModelDirectory();
        }
        Path modelsRoot = Path.of(configured);
        if (!modelsRoot.isAbsolute()) {
            modelsRoot = root.resolve(modelsRoot).normalize();
        }
        return modelsRoot.resolve("tts/xtts").normalize();
    }

    private static Path speakerWav(Path root, OperationalSettings settings, RuntimeArtifactPaths paths) {
        String voice = settings.tts().voiceProfileId() == null ? "" : settings.tts().voiceProfileId().strip();
        if (voice.isBlank() || "VOC-NARRATOR".equalsIgnoreCase(voice)) {
            return paths.xttsDefaultSpeakerWav();
        }
        Path direct = Path.of(voice);
        if (direct.isAbsolute() || voice.contains("/") || voice.contains("\\")) {
            return direct.normalize();
        }
        String fileName = voice.toLowerCase(Locale.ROOT).endsWith(".wav") ? voice : voice + ".wav";
        return modelDir(root, settings, paths).resolve("speakers").resolve(fileName).normalize();
    }
}
