package com.marcosmoreiradev.docupodcaststudio.application.engines;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectLocalModelFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelFolderContract;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelInspectionResult;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelInspectionStatus;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsDocumentGenerationReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsDocumentGenerationReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsSmokeTestReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractLanguageDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Operational preflight for the small, explicit voice/media engine shortlist.
 *
 * <p>The use case does not download model files. It tells the application what is ready, what is
 * missing, and which folders/executables must be configured so the UI can guide the user
 * without exposing command-line details on the reader workspace.</p>
 */
public final class InspectAiEnginesPreflightUseCase {
    private final InspectLocalModelFolderUseCase inspectModels;

    public InspectAiEnginesPreflightUseCase() {
        this(new InspectLocalModelFolderUseCase());
    }

    public InspectAiEnginesPreflightUseCase(InspectLocalModelFolderUseCase inspectModels) {
        this.inspectModels = inspectModels == null ? new InspectLocalModelFolderUseCase() : inspectModels;
    }

    public AiEnginePreflightReport inspect(OperationalSettings settings, Path projectRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = projectRoot == null ? Path.of(".") : projectRoot;
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path modelsRoot = resolveModelsRoot(root, current.storage().modelsDirectory());
        ArrayList<AiEnginePreflightItem> items = new ArrayList<>();
        items.add(inspectCoqui(current, modelsRoot, root, paths));
        items.add(inspectPiper(current, modelsRoot, paths));
        items.add(inspectOcr(current, paths));
        items.add(inspectFfmpeg(current, paths));
        return new AiEnginePreflightReport(items);
    }

    private AiEnginePreflightItem inspectCoqui(OperationalSettings settings, Path modelsRoot, Path applicationRoot,
                                               RuntimeArtifactPaths paths) {
        Path folder = modelsRoot.resolve("tts/xtts");
        ModelInspectionResult model = inspectModelFolder(ModelFolderContract.xttsHighQuality(), folder);
        boolean selected = "xtts".equalsIgnoreCase(settings.tts().engineMode())
                || "coqui".equalsIgnoreCase(settings.tts().engineMode())
                || settings.tts().displayName().toLowerCase().contains("coqui")
                || settings.tts().displayName().toLowerCase().contains("xtts");
        Path xttsWrapper = paths.xttsWrapperScript();
        Path xttsScript = paths.xttsPowerShellScript();
        Path defaultSpeaker = folder.resolve("speakers/voz-por-defecto.wav").normalize();
        boolean autoRuntimeAvailable = Files.isRegularFile(xttsWrapper) && Files.isRegularFile(xttsScript);
        boolean defaultSpeakerAvailable = Files.isRegularFile(defaultSpeaker);
        boolean runtimeConfigured = !settings.tts().commandTemplate().isBlank() || autoRuntimeAvailable;
        XttsDocumentGenerationReadinessReport generationGate = new InspectXttsDocumentGenerationReadinessUseCase()
                .inspect(settings, applicationRoot);
        XttsSmokeTestReport smoke = generationGate.smoke();
        ArrayList<String> diagnostics = new ArrayList<>();
        diagnostics.add("Motor obligatorio para calidad alta: Coqui/XTTS no es opcional en el producto objetivo.");
        diagnostics.add("Carpeta esperada: " + folder);
        diagnostics.add("Wrapper XTTS esperado: " + xttsWrapper);
        diagnostics.add("Script puente esperado: " + xttsScript);
        diagnostics.add("Muestra de voz por defecto esperada: " + defaultSpeaker);
        diagnostics.add("Muestra de voz por defecto disponible: " + (defaultSpeakerAvailable ? "sí" : "no"));
        diagnostics.add("Modelo local: " + model.status().name());
        diagnostics.add("Wrapper/comando configurado: " + (runtimeConfigured ? "sí" : "no"));
        diagnostics.add("Prueba WAV real generada: " + (smoke.generatedWavProof() ? "sí" : "no"));
        diagnostics.add("Reproducción de prueba confirmada: " + (smoke.playbackConfirmed() ? "sí" : "no"));
        if (!selected) {
            diagnostics.add("TTS activo actual: " + settings.tts().engineMode() + " — Coqui queda pendiente como motor de calidad alta.");
        }
        AiEngineReadinessStatus status;
        String message;
        String action;
        if (!runtimeConfigured) {
            status = AiEngineReadinessStatus.MISSING_RUNTIME;
            message = "Voz IA avanzada requiere preparar componentes locales antes de generar voz real.";
            action = "Pulsa Preparar automáticamente y luego prueba una oración corta.";
        } else if (!defaultSpeakerAvailable) {
            status = AiEngineReadinessStatus.MISSING_MODEL;
            message = "Voz IA avanzada requiere una voz neutral autorizada.";
            action = "Agrega o importa una muestra de voz autorizada desde la vista Voces.";
        } else if (model.status() == ModelInspectionStatus.MISSING_FOLDER || model.status() == ModelInspectionStatus.MISSING_REQUIRED_FILES) {
            status = AiEngineReadinessStatus.MISSING_MODEL;
            message = "Voz IA avanzada requiere descargar o importar recursos de voz.";
            action = "Pulsa Descargar o Importar y deja que la app verifique los recursos.";
        } else if (!smoke.generatedWavProof()) {
            status = AiEngineReadinessStatus.NEEDS_VERIFICATION;
            message = "Voz IA avanzada está descargada/configurada, pero aún no es usable para documentos: falta generar una prueba WAV real.";
            action = "Pulsa Probar en Configuración antes de generar fragmentos de audio del documento.";
        } else if (!smoke.playbackConfirmed()) {
            status = AiEngineReadinessStatus.READY_WITH_WARNINGS;
            message = "Voz IA avanzada puede generar fragmentos de audio porque ya produjo un WAV real; falta confirmar reproducción dentro de la app.";
            action = "Pulsa Reproducir prueba en Configuración para cerrar la verificación antes de trabajos largos.";
        } else if (model.status() == ModelInspectionStatus.CHECKSUM_NOT_PROVIDED) {
            status = AiEngineReadinessStatus.READY_WITH_WARNINGS;
            message = "Voz IA avanzada probada con WAV real; falta validación final de procedencia para empaquetado.";
            action = "Puedes usarla, pero conviene verificar checksum antes de RC final.";
        } else {
            status = AiEngineReadinessStatus.READY;
            message = "Voz IA avanzada lista, probada y confirmada como motor de calidad alta.";
            action = "Úsala como motor principal.";
        }
        return new AiEnginePreflightItem(
                "tts-xtts-coqui",
                "Voz IA avanzada",
                AiEnginePurpose.TTS_HIGH_QUALITY,
                true,
                status,
                message,
                action,
                folder,
                diagnostics
        );
    }

    private AiEnginePreflightItem inspectPiper(OperationalSettings settings, Path modelsRoot, RuntimeArtifactPaths paths) {
        Path folder = modelsRoot.resolve("tts/piper/voices");
        ModelInspectionResult model = inspectModelFolder(ModelFolderContract.piperLightweight(), folder);
        boolean selected = "piper".equalsIgnoreCase(settings.tts().engineMode());
        Path piperExecutable = paths.piperExecutable();
        Path piperWrapper = paths.piperPowerShellScript();
        boolean autoRuntimeAvailable = Files.isRegularFile(piperExecutable) && Files.isRegularFile(piperWrapper);
        boolean runtimeConfigured = selected && (!settings.tts().commandTemplate().isBlank() || autoRuntimeAvailable);
        ArrayList<String> diagnostics = new ArrayList<>();
        diagnostics.add("Motor intermedio/liviano para primera demo funcional y respaldo local.");
        diagnostics.add("Carpeta esperada: " + folder);
        diagnostics.add("Ejecutable Piper esperado: " + piperExecutable);
        diagnostics.add("Wrapper de archivo a WAV esperado: " + piperWrapper);
        diagnostics.add("Runtime automático disponible: " + (autoRuntimeAvailable ? "sí" : "no"));
        diagnostics.add("Modelo local: " + model.status().name());
        AiEngineReadinessStatus status;
        String message;
        String action;
        if (!runtimeConfigured && !model.usable()) {
            status = AiEngineReadinessStatus.OPTIONAL_NOT_CONFIGURED;
            message = "Voz local simple aún no está preparada.";
            action = "Pulsa Preparar Voz local simple o importa una voz local.";
        } else if (!runtimeConfigured) {
            status = AiEngineReadinessStatus.MISSING_RUNTIME;
            message = "Voz local simple tiene voz, pero falta preparar el componente de ejecución.";
            action = "Pulsa Preparar Voz local simple para completar el runtime.";
        } else if (!model.usable()) {
            status = AiEngineReadinessStatus.MISSING_MODEL;
            message = "Voz local simple requiere descargar o importar una voz.";
            action = "Importa una carpeta de voz local válida o pulsa Preparar Voz local simple.";
        } else if (model.status() == ModelInspectionStatus.CHECKSUM_NOT_PROVIDED) {
            status = AiEngineReadinessStatus.READY_WITH_WARNINGS;
            message = "Voz local simple lista para probar lectura; falta validación final de procedencia para empaquetado.";
            action = "Genera una prueba corta antes de usarla en documentos largos.";
        } else {
            status = AiEngineReadinessStatus.READY;
            message = "Voz local simple lista como motor liviano.";
            action = "Úsala cuando quieras una lectura más rápida y liviana.";
        }
        return new AiEnginePreflightItem(
                "tts-piper",
                "Voz local simple",
                AiEnginePurpose.TTS_LIGHTWEIGHT,
                false,
                status,
                message,
                action,
                folder,
                diagnostics
        );
    }

    private AiEnginePreflightItem inspectFfmpeg(OperationalSettings settings, RuntimeArtifactPaths paths) {
        String configured = settings.video().ffmpegExecutable();
        Path embedded = paths.ffmpegExecutable();
        boolean configuredExists = pathExists(configured);
        boolean embeddedExists = Files.isRegularFile(embedded);
        ArrayList<String> diagnostics = new ArrayList<>();
        diagnostics.add("FFmpeg se usa para extraer audio desde video y preparar video simple.");
        diagnostics.add("FFmpeg configurado: " + blankAs(configured, "no"));
        diagnostics.add("FFmpeg embebido esperado: " + embedded);
        AiEngineReadinessStatus status;
        String message;
        String action;
        if (configuredExists || embeddedExists) {
            status = AiEngineReadinessStatus.READY;
            message = "Video local listo para media y video simple.";
            action = "Prueba con un video corto antes de exportaciones largas.";
        } else {
            status = AiEngineReadinessStatus.MISSING_RUNTIME;
            message = "Video local requiere preparación para extraer audio y exportar video.";
            action = "Pulsa Importar video local y deja que la app copie los componentes necesarios.";
        }
        return new AiEnginePreflightItem(
                "ffmpeg",
                "Video local",
                AiEnginePurpose.MEDIA_AUDIO_EXTRACTION,
                true,
                status,
                message,
                action,
                embedded.getParent(),
                diagnostics
        );
    }

    private AiEnginePreflightItem inspectOcr(OperationalSettings settings, RuntimeArtifactPaths paths) {
        TesseractRuntimeLocator locator = new TesseractRuntimeLocator();
        TesseractToolDiscovery tesseract = locator.locate(settings, paths.applicationRoot());
        TesseractLanguageDiscovery languages = locator.inspectLanguages(tesseract, settings.ocr().languages());
        ArrayList<String> diagnostics = new ArrayList<>(tesseract.diagnostics());
        diagnostics.add("Idiomas OCR configurados: " + settings.ocr().languages());
        diagnostics.add("Idiomas OCR faltantes: " + (languages.missingLanguages().isEmpty() ? "ninguno" : languages.missingLabel()));
        diagnostics.add("Carpetas tessdata revisadas: " + languages.checkedDirectories());
        diagnostics.add("DPI OCR configurado: " + settings.ocr().dpi());
        diagnostics.add("Cache OCR: " + (settings.ocr().cacheEnabled() ? "sÃ­" : "no"));
        diagnostics.add("Runtime OCR esperado: " + paths.tesseractRoot());
        AiEngineReadinessStatus status = !tesseract.ready()
                ? AiEngineReadinessStatus.MISSING_RUNTIME
                : !languages.ready()
                ? AiEngineReadinessStatus.MISSING_MODEL
                : AiEngineReadinessStatus.READY;
        String message = status == AiEngineReadinessStatus.READY
                ? "OCR local listo para convertir PDF escaneados en fragmentos narrables."
                : status == AiEngineReadinessStatus.MISSING_MODEL
                ? "OCR local encontro Tesseract, pero faltan datos de idioma: " + languages.missingLabel() + "."
                : "OCR local requiere Tesseract portable o un ejecutable configurado para leer PDF escaneados.";
        String action = status == AiEngineReadinessStatus.READY
                ? "Importa o abre un PDF escaneado y prepara lectura."
                : status == AiEngineReadinessStatus.MISSING_MODEL
                ? "Importa una carpeta Tesseract portable que contenga tessdata/spa.traineddata y eng.traineddata."
                : "Importa una carpeta Tesseract portable en Configuracion o configura ocr.tesseractExecutable.";
        return new AiEnginePreflightItem(
                "ocr-tesseract",
                "OCR PDF local",
                AiEnginePurpose.OCR_TEXT_EXTRACTION,
                false,
                status,
                message,
                action,
                tesseract.expectedFolder(),
                diagnostics
        );
    }

    private ModelInspectionResult inspectModelFolder(ModelFolderContract contract, Path folder) {
        try {
            return inspectModels.inspect(contract, folder);
        } catch (IOException ex) {
            return new ModelInspectionResult(
                    contract.engineId(),
                    folder,
                    ModelInspectionStatus.MISSING_FOLDER,
                    List.of("No se pudo leer carpeta: " + ex.getMessage()),
                    List.of(),
                    false,
                    "No se pudo inspeccionar la carpeta local de " + contract.displayName() + "."
            );
        }
    }

    private static Path resolveModelsRoot(Path projectRoot, String configuredModelsDirectory) {
        String configured = configuredModelsDirectory == null ? "" : configuredModelsDirectory.strip();
        if (configured.isBlank()) {
            configured = "models";
        }
        Path path = Path.of(configured);
        return path.isAbsolute() ? path : projectRoot.resolve(path).normalize();
    }

    private static boolean pathExists(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(text.strip())) || Files.isDirectory(Path.of(text.strip()));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
