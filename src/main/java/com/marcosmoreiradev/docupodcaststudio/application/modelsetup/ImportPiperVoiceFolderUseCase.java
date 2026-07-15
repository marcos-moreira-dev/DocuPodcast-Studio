package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.PiperVoiceModelPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Imports a user-selected Piper voice folder into {@code models/tts/piper/voices}. */
public final class ImportPiperVoiceFolderUseCase {
    private static final int MAX_REPORTED_FILES = 40;
    private final InspectLocalModelFolderUseCase inspector;

    public ImportPiperVoiceFolderUseCase() {
        this(new InspectLocalModelFolderUseCase());
    }

    public ImportPiperVoiceFolderUseCase(InspectLocalModelFolderUseCase inspector) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public PiperVoiceImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot) {
        return importFrom(sourceFolder, settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public PiperVoiceImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot,
                                             ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path target = voiceDirectory(root, settings);
        if (sourceFolder == null) {
            return PiperVoiceImportReport.failed(null, target, "No se seleccionó una carpeta de Voz local simple.");
        }
        Path source = sourceFolder.toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return PiperVoiceImportReport.failed(source, target, "La carpeta seleccionada no existe o no es accesible.");
        }
        try {
            progress.onProgress("Verificando carpeta seleccionada de Voz local simple: " + source + ".");
            ModelInspectionResult sourceInspection = inspector.inspect(ModelFolderContract.piperLightweight(), source);
            if (!sourceInspection.usable()) {
                return new PiperVoiceImportReport(false, source, target, "", sourceInspection, List.of(),
                        "La carpeta seleccionada no contiene una voz local simple usable: "
                                + String.join(" · ", sourceInspection.missingRequirements()));
            }
            Path primaryVoicePath = discoverPrimaryOnnx(source);
            if (primaryVoicePath == null) {
                return PiperVoiceImportReport.failed(source, target,
                        "La carpeta seleccionada no contiene un archivo .onnx de voz local simple con su metadato .onnx.json.");
            }
            String primaryVoice = primaryVoicePath.getFileName().toString();
            Files.createDirectories(target);
            progress.onProgress("Copiando voz local simple a: " + target + ".");
            List<String> copied = copyFolder(source, target, progress);
            copyPrimaryVoiceToTargetRoot(primaryVoicePath, target, progress);
            progress.onProgress("Verificando voz local simple importada...");
            ModelInspectionResult targetInspection = inspector.inspect(ModelFolderContract.piperLightweight(), target);
            boolean success = targetInspection.usable();
            String message = success
                    ? "Voz local simple importada. Puedes usarla como modo liviano y guardar la configuración."
                    : "Se copiaron archivos, pero la voz local simple aún no quedó usable: "
                    + String.join(" · ", targetInspection.missingRequirements());
            progress.onProgress(message);
            return new PiperVoiceImportReport(success, source, target, primaryVoice, targetInspection, copied, message);
        } catch (IOException ex) {
            return PiperVoiceImportReport.failed(source, target, "No se pudo importar la voz local simple: " + ex.getMessage());
        }
    }

    private static Path discoverPrimaryOnnx(Path source) throws IOException {
        try (var stream = Files.walk(source, 3)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".onnx"))
                    .sorted(Comparator.comparing(path -> source.relativize(path).toString()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static void copyPrimaryVoiceToTargetRoot(Path primaryVoicePath, Path target,
                                                    ModelSetupProgressListener progress) throws IOException {
        Path rootVoice = target.resolve(primaryVoicePath.getFileName().toString()).normalize();
        Files.copy(primaryVoicePath, rootVoice, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        Path metadata = Path.of(primaryVoicePath.toString() + ".json").normalize();
        if (Files.isRegularFile(metadata)) {
            Files.copy(metadata, target.resolve(metadata.getFileName().toString()).normalize(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        progress.onProgress("Voz principal disponible en la raíz de models/tts/piper/voices: "
                + primaryVoicePath.getFileName() + ".");
    }

    private static List<String> copyFolder(Path source, Path target, ModelSetupProgressListener progress) throws IOException {
        ArrayList<String> copied = new ArrayList<>();
        int[] copiedCount = {0};
        try (var stream = Files.walk(source)) {
            for (Path item : stream.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(item);
                if (relative.toString().isBlank()) {
                    continue;
                }
                Path destination = target.resolve(relative).normalize();
                if (!destination.startsWith(target)) {
                    throw new IOException("Ruta no segura dentro de la voz local simple: " + relative);
                }
                if (Files.isDirectory(item)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(item)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    copiedCount[0]++;
                    if (copied.size() < MAX_REPORTED_FILES) {
                        copied.add(relative.toString().replace('\\', '/'));
                    }
                    if (copiedCount[0] == 1 || copiedCount[0] % 3 == 0) {
                        progress.onProgress("Copiados " + copiedCount[0] + " archivo(s) de Voz local simple; último: " + relative + ".");
                    }
                }
            }
        }
        return List.copyOf(copied);
    }

    private static Path voiceDirectory(Path applicationRoot, OperationalSettings settings) {
        return PiperVoiceModelPathPolicy.voiceDirectory(applicationRoot, settings);
    }
}
