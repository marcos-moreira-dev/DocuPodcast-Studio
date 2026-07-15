package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.PiperVoiceModelPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Verifies the simple local voice runtime and voice model layout without relying on the system PATH. */
public final class InspectPiperSetupReadinessUseCase {
    private final InspectLocalModelFolderUseCase inspectLocalModelFolder;

    public InspectPiperSetupReadinessUseCase() {
        this(new InspectLocalModelFolderUseCase());
    }

    public InspectPiperSetupReadinessUseCase(InspectLocalModelFolderUseCase inspectLocalModelFolder) {
        this.inspectLocalModelFolder = Objects.requireNonNull(inspectLocalModelFolder, "inspectLocalModelFolder");
    }

    public PiperSetupReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path wrapper = paths.piperPowerShellScript();
        Path piper = paths.piperExecutable();
        Path voiceDirectory = PiperVoiceModelPathPolicy.voiceDirectory(root, current);
        Path voiceModel = PiperVoiceModelPathPolicy.voiceModelPath(root, current);
        Path voiceMetadata = PiperVoiceModelPathPolicy.voiceMetadataPath(root, current);

        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        requireFile(wrapper, "Runtime: wrapper Voz local simple", missing);
        requireFile(piper, "Runtime: ejecutable Voz local simple", missing);
        requireFile(voiceModel, "Voz local simple: modelo ONNX", missing);
        requireFile(voiceMetadata, "Voz local simple: metadatos .onnx.json", missing);

        ModelInspectionResult modelInspection;
        try {
            modelInspection = inspectLocalModelFolder.inspect(ModelFolderContract.piperLightweight(), voiceDirectory);
        } catch (IOException ex) {
            modelInspection = new ModelInspectionResult(
                    ModelFolderContract.piperLightweight().engineId(),
                    voiceDirectory,
                    ModelInspectionStatus.MISSING_FOLDER,
                    List.of("No se pudo inspeccionar la carpeta Voz local simple: " + ex.getMessage()),
                    List.of(),
                    false,
                    "No se pudo verificar la Voz local simple.");
        }
        if (!modelInspection.usable()) {
            missing.add("Modelo Voz local simple: " + modelInspection.userMessage());
        }
        if (modelInspection.status() == ModelInspectionStatus.CHECKSUM_NOT_PROVIDED) {
            warnings.add("La Voz local simple parece completa, pero no incluye checksum local. Conviene validarla con un manifiesto antes de RC final.");
        }
        String message = missing.isEmpty()
                ? "Voz local simple está lista como modo de lectura liviano. No habilita clonación, emociones ni voces por muestra humana."
                : "Voz local simple requiere preparación antes de generar voz real en modo liviano.";
        return new PiperSetupReadinessReport(root, wrapper, piper, voiceDirectory, voiceModel, voiceMetadata,
                modelInspection, missing, warnings, message);
    }

    private static void requireFile(Path file, String label, List<String> missing) {
        if (file == null || !Files.isRegularFile(file)) {
            missing.add(label + " faltante: " + Objects.toString(file, "sin ruta"));
        }
    }
}
