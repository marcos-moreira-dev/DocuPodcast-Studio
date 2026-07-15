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

/** Imports a user-selected image model package into models/image. */
public final class ImportLocalTheatreImagePackageUseCase {
    private static final int MAX_REPORTED_FILES = 60;
    private final InspectLocalModelFolderUseCase inspector;
    private final InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector;

    public ImportLocalTheatreImagePackageUseCase() {
        this(new InspectLocalModelFolderUseCase(), new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public ImportLocalTheatreImagePackageUseCase(InspectLocalModelFolderUseCase inspector) {
        this(inspector, new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public ImportLocalTheatreImagePackageUseCase(InspectLocalModelFolderUseCase inspector,
                                                 InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.artifactInspector = Objects.requireNonNull(artifactInspector, "artifactInspector");
    }

    public LocalTheatreImagePackageImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot) {
        return importFrom(sourceFolder, settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public LocalTheatreImagePackageImportReport importFrom(Path sourceFolder, OperationalSettings settings, Path applicationRoot,
                                                          ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path target = root.resolve("models/image").normalize();
        if (sourceFolder == null) {
            return failed(null, target, "No se selecciono una carpeta de paquete de Imagen IA teatral.");
        }
        Path source = sourceFolder.toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return failed(source, target, "La carpeta seleccionada no existe o no es accesible.");
        }
        try {
            progress.onProgress("Verificando paquete seleccionado...");
            ModelInspectionResult sourceInspection = inspector.inspect(ModelFolderContract.localTheatreImage(), source);
            if (!sourceInspection.usable()) {
                return new LocalTheatreImagePackageImportReport(false, source, target, sourceInspection, List.of(),
                        "El paquete seleccionado no contiene los archivos requeridos: "
                                + String.join(" - ", sourceInspection.missingRequirements()));
            }
            Files.createDirectories(target);
            progress.onProgress("Copiando paquete a models/image...");
            List<String> copied = copyFolder(source, target, progress);
            ModelInspectionResult targetInspection = inspector.inspect(ModelFolderContract.localTheatreImage(), target);
            ImageEngineArtifactInspectionReport artifacts = artifactInspector.inspect(settings, root);
            boolean success = artifacts.modelReady();
            String message = success
                    ? "Paquete de Imagen IA teatral importado con modelo y workflow reales. Puedes probar el motor local."
                    : "Se copiaron archivos, pero falta completar el paquete real: "
                    + String.join(" - ", modelMissingRequirements(artifacts));
            return new LocalTheatreImagePackageImportReport(success, source, target, targetInspection, copied, message);
        } catch (IOException ex) {
            return failed(source, target, "No se pudo importar Imagen IA teatral: " + ex.getMessage());
        }
    }

    private static LocalTheatreImagePackageImportReport failed(Path source, Path target, String message) {
        return new LocalTheatreImagePackageImportReport(false, source, target, null, List.of(), message);
    }

    private static List<String> copyFolder(Path source, Path target, ModelSetupProgressListener progress) throws IOException {
        ArrayList<String> copied = new ArrayList<>();
        int[] count = {0};
        try (var stream = Files.walk(source)) {
            for (Path item : stream.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(item);
                if (relative.toString().isBlank()) {
                    continue;
                }
                Path destination = target.resolve(relative).normalize();
                if (!destination.startsWith(target)) {
                    throw new IOException("Ruta no segura dentro del paquete de imagen: " + relative);
                }
                if (Files.isDirectory(item)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(item)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    count[0]++;
                    if (copied.size() < MAX_REPORTED_FILES) {
                        copied.add(relative.toString().replace('\\', '/'));
                    }
                    if (count[0] == 1 || count[0] % 5 == 0) {
                        progress.onProgress("Copiados " + count[0] + " archivo(s) de Imagen IA teatral.");
                    }
                }
            }
        }
        return List.copyOf(copied);
    }

    private static List<String> modelMissingRequirements(ImageEngineArtifactInspectionReport artifacts) {
        if (artifacts == null) {
            return List.of("No se pudo inspeccionar el paquete importado.");
        }
        ArrayList<String> missing = new ArrayList<>();
        if (!artifacts.checkpointStatus().ready()) {
            missing.add("checkpoint real");
        }
        if (!artifacts.workflowStatus().ready()) {
            missing.add("workflow ComfyUI real");
        }
        if (missing.isEmpty()) {
            missing.addAll(artifacts.missingRequirements());
        }
        return missing;
    }
}
