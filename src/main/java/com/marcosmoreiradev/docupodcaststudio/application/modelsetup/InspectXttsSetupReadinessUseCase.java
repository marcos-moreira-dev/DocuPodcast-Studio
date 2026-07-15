package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Verifies the local advanced AI voice runtime and model layout without using global Python or PATH. */
public final class InspectXttsSetupReadinessUseCase {
    private final InspectLocalModelFolderUseCase inspectLocalModelFolder;

    public InspectXttsSetupReadinessUseCase() {
        this(new InspectLocalModelFolderUseCase());
    }

    public InspectXttsSetupReadinessUseCase(InspectLocalModelFolderUseCase inspectLocalModelFolder) {
        this.inspectLocalModelFolder = Objects.requireNonNull(inspectLocalModelFolder, "inspectLocalModelFolder");
    }

    public XttsSetupReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path setupScript = paths.xttsPortableSetupScript();
        Path python = paths.xttsPythonExecutable();
        Path wrapper = paths.xttsWrapperScript();
        Path configuredModelDirectory = XttsModelPathPolicy.modelDirectoryFromSettings(current, root);
        ModelInspectionResult modelInspection = inspectModel(configuredModelDirectory);
        Path modelDirectory = configuredModelDirectory;
        Path portableModelDirectory = XttsModelPathPolicy.portableModelDirectory(root);
        if (!modelInspection.usable() && !portableModelDirectory.equals(configuredModelDirectory)) {
            ModelInspectionResult portableInspection = inspectModel(portableModelDirectory);
            if (portableInspection.usable()) {
                modelDirectory = portableModelDirectory;
                modelInspection = portableInspection;
            }
        }
        Path speakerWav = speakerWavPath(modelDirectory, current, root);

        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        requireFile(setupScript, "Runtime: script de preparación Python local", missing);
        requireFile(python, "Runtime: Python local portable", missing);
        requireFile(wrapper, "Runtime: wrapper Voz IA avanzada", missing);
        requireFile(speakerWav, "Runtime: voz neutral o muestra WAV", missing);

        if (!modelInspection.usable()) {
            missing.add("Modelo: " + modelInspection.userMessage());
        } else if (!modelDirectory.equals(configuredModelDirectory)) {
            warnings.add("Se detectó una carpeta models/ local trasplantada y usable: " + modelDirectory + ".");
        }
        if (modelInspection.status() == ModelInspectionStatus.CHECKSUM_NOT_PROVIDED) {
            warnings.add("El modelo XTTS parece completo, pero no incluye checksum local. Conviene validarlo con un manifiesto antes de RC final.");
        }

        String message = missing.isEmpty()
                ? "Voz IA avanzada está lista para usarse como motor principal de lectura de calidad alta."
                : "Voz IA avanzada requiere preparación antes de generar voz real.";
        return new XttsSetupReadinessReport(root, setupScript, python, wrapper, modelDirectory, speakerWav,
                modelInspection, missing, warnings, message);
    }

    private ModelInspectionResult inspectModel(Path modelDirectory) {
        try {
            return inspectLocalModelFolder.inspect(ModelFolderContract.xttsHighQuality(), modelDirectory);
        } catch (IOException ex) {
            return new ModelInspectionResult(
                    ModelFolderContract.xttsHighQuality().engineId(),
                    modelDirectory,
                    ModelInspectionStatus.MISSING_FOLDER,
                    List.of("No se pudo inspeccionar la carpeta XTTS: " + ex.getMessage()),
                    List.of(),
                    false,
                    "No se pudo verificar el modelo local de Voz IA avanzada.");
        }
    }

    private static Path speakerWavPath(Path modelDirectory, OperationalSettings settings, Path applicationRoot) {
        Path modelRoot = modelDirectory == null
                ? RuntimeArtifactPaths.fromRoot(applicationRoot).xttsModelDirectory()
                : modelDirectory;
        String voice = advancedSpeakerVoiceId(settings);
        Path voicePath = Path.of(voice);
        if (voicePath.isAbsolute() || voice.contains("/") || voice.contains("\\")) {
            return voicePath.normalize();
        }
        if (!voice.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) {
            voice = voice + ".wav";
        }
        return modelRoot.resolve("speakers").resolve(voice).normalize();
    }

    private static String advancedSpeakerVoiceId(OperationalSettings settings) {
        String mode = settings == null || settings.tts() == null ? "" : Objects.toString(settings.tts().engineMode(), "");
        String voice = settings == null || settings.tts() == null ? "" : Objects.toString(settings.tts().voiceProfileId(), "");
        String normalizedMode = mode.strip().toLowerCase(java.util.Locale.ROOT);
        String normalizedVoice = voice.strip();
        if (!"xtts".equals(normalizedMode) && !"coqui".equals(normalizedMode)) {
            return "voz-por-defecto.wav";
        }
        if (normalizedVoice.isBlank()
                || "VOC-NARRATOR".equalsIgnoreCase(normalizedVoice)
                || "VOC-OWN-PLACEHOLDER".equalsIgnoreCase(normalizedVoice)
                || "voz-local-simple".equalsIgnoreCase(normalizedVoice)
                || OfficialAdvancedVoicePresetCatalog.voiceIds().stream().anyMatch(id -> id.equalsIgnoreCase(normalizedVoice))) {
            return "voz-por-defecto.wav";
        }
        return normalizedVoice;
    }

    private static void requireFile(Path file, String label, List<String> missing) {
        if (file == null || !Files.isRegularFile(file)) {
            missing.add(label + " faltante: " + Objects.toString(file, "sin ruta"));
        }
    }
}
