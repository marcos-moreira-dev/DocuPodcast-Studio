package com.marcosmoreiradev.docupodcaststudio.application.video;

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
import java.util.Objects;

/** Imports a user-selected FFmpeg distribution into {@code tools/ffmpeg/bin}. */
public final class ImportFfmpegRuntimeFolderUseCase {
    private static final int MAX_SEARCH_DEPTH = 5;

    public FfmpegRuntimeImportReport importFrom(Path sourceFolder, Path applicationRoot) {
        return importFrom(sourceFolder, applicationRoot, ModelSetupProgressListener.noop());
    }

    public FfmpegRuntimeImportReport importFrom(Path sourceFolder, Path applicationRoot,
                                                ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path targetBin = paths.ffmpegBinDirectory();
        if (sourceFolder == null) {
            return FfmpegRuntimeImportReport.failed(null, targetBin, "No se seleccionó una carpeta de FFmpeg.");
        }
        Path source = sourceFolder.toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return FfmpegRuntimeImportReport.failed(source, targetBin, "La carpeta seleccionada no existe o no es accesible.");
        }
        try {
            progress.onProgress("Buscando ffmpeg.exe y ffprobe.exe dentro de: " + source + ".");
            Path ffmpeg = findTool(source, "ffmpeg.exe");
            Path ffprobe = findTool(source, "ffprobe.exe");
            if (ffmpeg == null || ffprobe == null) {
                return FfmpegRuntimeImportReport.failed(source, targetBin,
                        "La carpeta seleccionada debe contener ffmpeg.exe y ffprobe.exe.");
            }
            Files.createDirectories(targetBin);
            Path targetFfmpeg = targetBin.resolve("ffmpeg.exe");
            Path targetFfprobe = targetBin.resolve("ffprobe.exe");
            progress.onProgress("Copiando binarios FFmpeg a: " + targetBin + ".");
            Files.copy(ffmpeg, targetFfmpeg, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            progress.onProgress("Copiado ffmpeg.exe.");
            Files.copy(ffprobe, targetFfprobe, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            progress.onProgress("Copiado ffprobe.exe.");
            List<String> copied = copyAdjacentLegalFiles(ffmpeg.getParent(), paths.ffmpegRoot(), progress);
            ArrayList<String> reported = new ArrayList<>();
            reported.add("bin/ffmpeg.exe");
            reported.add("bin/ffprobe.exe");
            reported.addAll(copied);
            progress.onProgress("Verificando FFmpeg importado desde tools/ffmpeg/bin...");
            FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, null);
            boolean success = discovery.ready() && Files.isRegularFile(targetFfprobe);
            String message = success
                    ? "FFmpeg y FFprobe quedaron importados en la carpeta del programa. Puedes verificarlos desde Configuración."
                    : "Se copiaron binarios, pero la app todavía no pudo localizarlos como runtime embebido.";
            progress.onProgress(message);
            return new FfmpegRuntimeImportReport(success, source, targetBin, targetFfmpeg, targetFfprobe, reported, message);
        } catch (IOException ex) {
            return FfmpegRuntimeImportReport.failed(source, targetBin, "No se pudo importar FFmpeg: " + ex.getMessage());
        }
    }

    private static Path findTool(Path source, String fileName) throws IOException {
        String expected = fileName.toLowerCase(Locale.ROOT);
        try (var stream = Files.walk(source, MAX_SEARCH_DEPTH)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).equals(expected))
                    .sorted(Comparator.comparing(path -> source.relativize(path).toString()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static List<String> copyAdjacentLegalFiles(Path sourceBin, Path targetRoot,
                                                       ModelSetupProgressListener progress) throws IOException {
        if (sourceBin == null || sourceBin.getParent() == null) {
            return List.of();
        }
        Path sourceRoot = sourceBin.getParent();
        ArrayList<String> copied = new ArrayList<>();
        for (String name : List.of("LICENSE", "LICENSE.txt", "COPYING", "README", "README.txt")) {
            Path candidate = sourceRoot.resolve(name);
            if (Files.isRegularFile(candidate)) {
                Path destination = targetRoot.resolve(name).normalize();
                if (!destination.startsWith(targetRoot)) {
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(candidate, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                copied.add(name);
                progress.onProgress("Copiado archivo informativo/legal de FFmpeg: " + name + ".");
            }
        }
        return List.copyOf(copied);
    }
}
