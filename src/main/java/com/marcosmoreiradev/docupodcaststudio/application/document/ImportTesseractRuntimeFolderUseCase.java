package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Imports a user-selected Tesseract distribution into {@code tools/tesseract}. */
public final class ImportTesseractRuntimeFolderUseCase {
    private static final int MAX_SEARCH_DEPTH = 6;
    private static final List<String> REQUIRED_LANGUAGES = List.of("spa", "eng");

    public TesseractRuntimeImportReport importFrom(Path sourceFolder, Path applicationRoot) {
        return importFrom(sourceFolder, applicationRoot, ModelSetupProgressListener.noop());
    }

    public TesseractRuntimeImportReport importFrom(Path sourceFolder, Path applicationRoot,
                                                   ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path targetRoot = paths.tesseractRoot();
        if (sourceFolder == null) {
            return TesseractRuntimeImportReport.failed(null, targetRoot, "No se selecciono una carpeta Tesseract.");
        }
        Path source = sourceFolder.toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return TesseractRuntimeImportReport.failed(source, targetRoot, "La carpeta seleccionada no existe o no es accesible.");
        }
        try {
            progress.onProgress("Buscando tesseract.exe dentro de: " + source + ".");
            Path executable = findFile(source, "tesseract.exe");
            if (executable == null) {
                return TesseractRuntimeImportReport.failed(source, targetRoot,
                        "La carpeta seleccionada debe contener tesseract.exe.");
            }
            Path sourceBin = executable.getParent();
            Path sourceTessdata = findTessdata(source, sourceBin);
            ArrayList<String> missingLanguages = missingLanguages(sourceTessdata);
            Files.createDirectories(paths.tesseractBinDirectory());
            Files.createDirectories(targetRoot.resolve("tessdata"));
            ArrayList<String> copied = new ArrayList<>();
            progress.onProgress("Copiando runtime Tesseract a: " + targetRoot + ".");
            try (var stream = Files.list(sourceBin)) {
                for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                    Path target = paths.tesseractBinDirectory().resolve(file.getFileName().toString()).normalize();
                    if (target.startsWith(paths.tesseractBinDirectory())) {
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                        copied.add("bin/" + target.getFileName());
                    }
                }
            }
            if (sourceTessdata != null) {
                for (String language : REQUIRED_LANGUAGES) {
                    Path trainedData = sourceTessdata.resolve(language + ".traineddata");
                    if (!Files.isRegularFile(trainedData)) {
                        continue;
                    }
                    Path target = targetRoot.resolve("tessdata").resolve(trainedData.getFileName().toString()).normalize();
                    Files.copy(trainedData, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    copied.add("tessdata/" + target.getFileName());
                }
            }
            Path targetExecutable = paths.tesseractExecutable();
            boolean success = Files.isRegularFile(targetExecutable) && missingLanguages.isEmpty();
            String message = success
                    ? "Tesseract OCR quedo importado con idiomas spa y eng dentro del programa."
                    : "Tesseract se copio, pero faltan idiomas OCR: " + String.join(", ", missingLanguages) + ".";
            progress.onProgress(message);
            return new TesseractRuntimeImportReport(success, source, targetRoot, targetExecutable,
                    copied, missingLanguages, message);
        } catch (IOException ex) {
            return TesseractRuntimeImportReport.failed(source, targetRoot,
                    "No se pudo importar Tesseract: " + ex.getMessage());
        }
    }

    private static Path findTessdata(Path source, Path sourceBin) throws IOException {
        for (Path candidate : List.of(
                source.resolve("tessdata"),
                sourceBin.resolve("tessdata"),
                sourceBin.getParent() == null ? source.resolve("tessdata") : sourceBin.getParent().resolve("tessdata"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        try (var stream = Files.walk(source, MAX_SEARCH_DEPTH)) {
            return stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("tessdata"))
                    .sorted(Comparator.comparing(path -> source.relativize(path).toString()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static ArrayList<String> missingLanguages(Path tessdata) {
        ArrayList<String> missing = new ArrayList<>();
        for (String language : REQUIRED_LANGUAGES) {
            if (tessdata == null || !Files.isRegularFile(tessdata.resolve(language + ".traineddata"))) {
                missing.add(language);
            }
        }
        return missing;
    }

    private static Path findFile(Path source, String fileName) throws IOException {
        String expected = fileName.toLowerCase(Locale.ROOT);
        try (var stream = Files.walk(source, MAX_SEARCH_DEPTH)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).equals(expected))
                    .sorted(Comparator.comparing(path -> source.relativize(path).toString()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
