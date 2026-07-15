package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/** Imports a real local ComfyUI-compatible runtime into {@code tools/image}. */
public final class ImportLocalTheatreImageRuntimeUseCase {
    private final InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector;

    public ImportLocalTheatreImageRuntimeUseCase() {
        this(new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    ImportLocalTheatreImageRuntimeUseCase(InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector) {
        this.artifactInspector = artifactInspector;
    }

    public LocalTheatreImagePreparationReport importFrom(Path sourceFolder,
                                                         OperationalSettings settings,
                                                         Path applicationRoot) {
        return importFrom(sourceFolder, settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public LocalTheatreImagePreparationReport importFrom(Path sourceFolder,
                                                         OperationalSettings settings,
                                                         Path applicationRoot,
                                                         ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path runtime = root.resolve("tools/image").normalize();
        Path models = root.resolve("models/image").normalize();
        if (sourceFolder == null) {
            return new LocalTheatreImagePreparationReport(false, runtime, models,
                    "No se selecciono una carpeta de runtime.");
        }
        Path source = sourceFolder.toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return new LocalTheatreImagePreparationReport(false, runtime, models,
                    "La carpeta seleccionada no existe o no es accesible.");
        }
        if (!InspectLocalTheatreImageEngineArtifactsUseCase.hasCompatibleLauncher(source)) {
            return new LocalTheatreImagePreparationReport(false, runtime, models,
                    "La carpeta seleccionada no contiene un runtime compatible. Debe traer start-image-engine.bat, run.bat, ComfyUI.bat o ComfyUI/main.py con Python local.");
        }
        try {
            if (source.equals(runtime)) {
                progress.onProgress("El runtime ya esta en tools/image; verificando lanzador.");
            } else {
                progress.onProgress("Copiando runtime local a tools/image...");
                Files.createDirectories(runtime);
                copyTree(source, runtime, progress);
            }
            ensureManagedLauncher(runtime, progress);
            ImageEngineArtifactInspectionReport report = artifactInspector.inspect(settings, root);
            if (!report.runtimeReady()) {
                return new LocalTheatreImagePreparationReport(false, runtime, models,
                        "Se copio el runtime, pero no se encontro un lanzador ejecutable compatible en tools/image.");
            }
            return new LocalTheatreImagePreparationReport(true, runtime, models,
                    "Runtime local de Imagen IA teatral importado. Siguiente paso: verificar modelo/workflow y ejecutar Probar generacion.");
        } catch (IOException ex) {
            return new LocalTheatreImagePreparationReport(false, runtime, models,
                    "No se pudo importar el runtime local de Imagen IA: " + ex.getMessage());
        }
    }

    private static void copyTree(Path source, Path target, ModelSetupProgressListener progress) throws IOException {
        try (var stream = Files.walk(source)) {
            for (Path path : stream.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(path);
                if (relative.toString().isBlank()) {
                    continue;
                }
                Path destination = target.resolve(relative).normalize();
                if (!destination.startsWith(target)) {
                    continue;
                }
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(path)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        progress.onProgress("Runtime copiado a tools/image.");
    }

    static Path ensureManagedLauncher(Path runtime, ModelSetupProgressListener progress) throws IOException {
        ModelSetupProgressListener safeProgress = progress == null ? ModelSetupProgressListener.noop() : progress;
        Path launcher = runtime.resolve("start-image-engine.bat");
        Path main = Files.isRegularFile(runtime.resolve("main.py"))
                ? runtime.resolve("main.py")
                : runtime.resolve("ComfyUI/main.py");
        Path python = firstExisting(runtime,
                "python_embeded/python.exe", "python_embedded/python.exe", "venv/Scripts/python.exe", ".venv/Scripts/python.exe");
        if (Files.isRegularFile(main) && python != null) {
            String pythonRelative = runtime.relativize(python).toString().replace('/', '\\');
            String mainRelative = runtime.relativize(main).toString().replace('/', '\\');
            int separator = mainRelative.lastIndexOf('\\');
            String mainDirectory = separator < 0 ? "" : mainRelative.substring(0, separator);
            String mainFile = separator < 0 ? mainRelative : mainRelative.substring(separator + 1);
            String workingDirectory = mainDirectory.isBlank() ? "%~dp0" : "%~dp0" + mainDirectory;
            String script = "@echo off\r\n"
                    + "setlocal\r\n"
                    + "cd /d \"" + workingDirectory + "\"\r\n"
                    + "if \"%~1\"==\"\" (\r\n"
                    + "  \"%~dp0" + pythonRelative + "\" \"" + mainFile + "\" --lowvram --disable-auto-launch --port 8188\r\n"
                    + ") else (\r\n"
                    + "  \"%~dp0" + pythonRelative + "\" \"" + mainFile + "\" --disable-auto-launch %*\r\n"
                    + ")\r\n";
            Files.writeString(launcher, script, StandardCharsets.UTF_8);
            safeProgress.onProgress("Creado start-image-engine.bat administrado para el runtime importado.");
            return launcher;
        }
        return Files.isRegularFile(launcher) ? launcher : null;
    }

    private static Path firstExisting(Path root, String... relativePaths) {
        for (String relativePath : relativePaths) {
            Path candidate = root.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
