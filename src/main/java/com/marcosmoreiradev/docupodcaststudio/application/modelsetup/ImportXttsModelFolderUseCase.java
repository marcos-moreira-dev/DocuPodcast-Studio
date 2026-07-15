package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Imports a user-selected local XTTS model folder into {@code models/tts/xtts} without command line use. */
public final class ImportXttsModelFolderUseCase {
    private static final int MAX_REPORTED_FILES = 60;
    private final InspectLocalModelFolderUseCase inspector;

    public ImportXttsModelFolderUseCase() {
        this(new InspectLocalModelFolderUseCase());
    }

    public ImportXttsModelFolderUseCase(InspectLocalModelFolderUseCase inspector) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public XttsModelImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot) {
        return importFrom(sourceFolder, settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsModelImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot,
                                            ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path target = XttsModelPathPolicy.modelDirectoryFromSettings(settings, root);
        if (sourceFolder == null) {
            return XttsModelImportReport.failed(null, target, "No se seleccionó una carpeta de modelo.");
        }
        Path source = XttsModelPathPolicy.normalizeUserSelectedModelPath(sourceFolder, root);
        if (!Files.isDirectory(source)) {
            return XttsModelImportReport.failed(source, target,
                    "La carpeta seleccionada no existe o no es accesible. "
                            + XttsModelPathPolicy.explain(sourceFolder, source));
        }
        try {
            progress.onProgress("Verificando carpeta seleccionada: " + source + ".");
            ModelInspectionResult sourceInspection = inspector.inspect(ModelFolderContract.xttsHighQuality(), source);
            if (!sourceInspection.usable()) {
                return new XttsModelImportReport(false, source, target, sourceInspection, List.of(),
                        "La carpeta seleccionada no cumple el contrato de Voz IA avanzada: "
                                + String.join(" · ", sourceInspection.missingRequirements()));
            }
            if (source.equals(target)) {
                progress.onProgress("El modelo ya está en la carpeta del programa; verificando contrato local.");
                ModelInspectionResult targetInspection = inspector.inspect(ModelFolderContract.xttsHighQuality(), target);
                return new XttsModelImportReport(targetInspection.usable(), source, target, targetInspection, List.of(),
                        "El modelo ya está en la carpeta del programa; no fue necesario copiar archivos.");
            }
            Files.createDirectories(target);
            progress.onProgress("Copiando modelo a: " + target + ".");
            List<String> copied = copyFolder(source, target, progress);
            progress.onProgress("Verificando modelo importado...");
            ModelInspectionResult targetInspection = inspector.inspect(ModelFolderContract.xttsHighQuality(), target);
            String message = targetInspection.usable()
                    ? "Modelo de Voz IA avanzada importado al programa. Puedes verificar y usar el motor."
                    : "Se copiaron archivos, pero el modelo aún no quedó usable: "
                    + String.join(" · ", targetInspection.missingRequirements());
            progress.onProgress(message);
            return new XttsModelImportReport(targetInspection.usable(), source, target, targetInspection, copied, message);
        } catch (IOException ex) {
            return XttsModelImportReport.failed(source, target, "No se pudo importar el modelo: " + ex.getMessage());
        }
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
                    throw new IOException("Ruta no segura dentro del modelo: " + relative);
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
                    if (copiedCount[0] == 1 || copiedCount[0] % 5 == 0) {
                        progress.onProgress("Copiados " + copiedCount[0] + " archivo(s); último: " + relative + ".");
                    }
                }
            }
        }
        return List.copyOf(copied);
    }

}
