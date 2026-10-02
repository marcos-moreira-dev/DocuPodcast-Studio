package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and prepares the local video runtime under {@code tools/ffmpeg/bin}.
 *
 * <p>The app never depends on global PATH for the final product. This use case is deliberately
 * application-level: it downloads a ZIP configured in operational settings, extracts it into a
 * temporary app-owned folder, copies {@code ffmpeg.exe} and {@code ffprobe.exe} into the embedded
 * runtime layout, then probes encoder support for final video.</p>
 */
public final class DownloadFfmpegPortableRuntimeUseCase {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(30);
    private static final long PROGRESS_STEP_BYTES = 8L * 1024L * 1024L;
    private static final int MAX_SEARCH_DEPTH = 8;

    private final HttpClient httpClient;
    private final FfmpegRuntimeProbeUseCase probe;

    public DownloadFfmpegPortableRuntimeUseCase() {
        this(new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("DownloadFfmpegPortableRuntimeUseCase")));
    }

    public DownloadFfmpegPortableRuntimeUseCase(FfmpegRuntimeProbeUseCase probe) {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(30))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                probe);
    }

    public DownloadFfmpegPortableRuntimeUseCase(HttpClient httpClient, FfmpegRuntimeProbeUseCase probe) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    public FfmpegRuntimeDownloadReport download(OperationalSettings settings, Path applicationRoot) {
        return download(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public FfmpegRuntimeDownloadReport download(OperationalSettings settings, Path applicationRoot,
                                                ModelSetupProgressListener listener) {
        return download(settings, applicationRoot, listener, false);
    }

    public FfmpegRuntimeDownloadReport download(OperationalSettings settings, Path applicationRoot,
                                                ModelSetupProgressListener listener, boolean forceRedownload) {
        ModelSetupProgressListener progress = listener == null ? ModelSetupProgressListener.noop() : listener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path runtimeRoot = paths.ffmpegRoot();
        Path targetBin = paths.ffmpegBinDirectory();
        Path downloads = runtimeRoot.resolve("downloads").normalize();
        Path zip = downloads.resolve("video-local-runtime.zip").normalize();
        Path extracted = downloads.resolve("extracted").normalize();
        ArrayList<String> downloaded = new ArrayList<>();
        ArrayList<String> copied = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        try {
            progress.onProgress("Verificando si Video local ya está preparado...");
            FfmpegToolDiscovery existing = new EmbeddedFfmpegLocator().locate(root, null);
            FfmpegRuntimeReport existingReport = probe.inspect(existing);
            if (!forceRedownload && existingReport.readyForFinalVideo()) {
                progress.onProgress("Video local ya está listo dentro del programa.");
                return new FfmpegRuntimeDownloadReport(true, zip, extracted, targetBin,
                        existingReport.ffmpegExecutable(), existingReport.ffprobeExecutable(), existingReport,
                        List.of(), List.of("runtime existente"), List.of(),
                        "Video local ya está listo dentro del programa.");
            }

            Files.createDirectories(downloads);
            Files.createDirectories(targetBin);
            URI source = downloadUri(settings);
            progress.onProgress("Descargando componente de video local desde la URL configurada...");
            downloadFile(source, zip, "componente de video local", progress);
            downloaded.add("paquete de video local");
            progress.onProgress("Extrayendo componente de video local dentro del programa...");
            recreateDirectory(extracted);
            extractZip(zip, extracted);
            Path ffmpeg = findTool(extracted, "ffmpeg.exe");
            Path ffprobe = findTool(extracted, "ffprobe.exe");
            if (ffmpeg == null || ffprobe == null) {
                failed.add("No se encontraron los dos componentes de video requeridos dentro del ZIP.");
                return FfmpegRuntimeDownloadReport.failed(zip, extracted, targetBin,
                        "El paquete descargado no contiene los componentes de video necesarios.", failed);
            }
            progress.onProgress("Copiando componentes de video a la carpeta del programa...");
            Path targetFfmpeg = targetBin.resolve("ffmpeg.exe").normalize();
            Path targetFfprobe = targetBin.resolve("ffprobe.exe").normalize();
            Files.copy(ffmpeg, targetFfmpeg, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            Files.copy(ffprobe, targetFfprobe, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            copied.add("bin/ffmpeg.exe");
            copied.add("bin/ffprobe.exe");
            copied.addAll(copyAdjacentLegalFiles(ffmpeg.getParent(), runtimeRoot, progress));
            progress.onProgress("Verificando que Video local sirva para exportación final...");
            FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, null);
            FfmpegRuntimeReport report = probe.inspect(discovery);
            boolean success = report.readyForFinalVideo();
            String message;
            if (success) {
                message = "Video local quedó preparado dentro del programa y está listo para exportar video final.";
            } else {
                failed.addAll(report.warnings());
                message = "Video local fue descargado, pero la verificación final quedó incompleta: "
                        + String.join(" · ", report.warnings());
            }
            progress.onProgress(message);
            return new FfmpegRuntimeDownloadReport(success, zip, extracted, targetBin,
                    report.ffmpegExecutable(), report.ffprobeExecutable(), report, downloaded, copied, failed, message);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failed.add("interrumpido");
            return FfmpegRuntimeDownloadReport.failed(zip, extracted, targetBin,
                    "La preparación de Video local fue interrumpida.", failed);
        } catch (IOException | RuntimeException ex) {
            failed.add(Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()));
            return FfmpegRuntimeDownloadReport.failed(zip, extracted, targetBin,
                    "No se pudo preparar Video local: " + Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()), failed);
        }
    }

    private static URI downloadUri(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String value = current.video().ffmpegDownloadUrl();
        if (value == null || value.isBlank()) {
            value = OperationalSettings.VideoRenderSettings.DEFAULT_FFMPEG_DOWNLOAD_URL;
        }
        return URI.create(value.strip());
    }

    private void downloadFile(URI uri, Path destination, String label, ModelSetupProgressListener progress)
            throws IOException, InterruptedException {
        Files.createDirectories(destination.getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".download");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " al descargar " + label);
        }
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        copyWithProgress(response.body(), temp, label, total, progress);
        if (!Files.isRegularFile(temp) || Files.size(temp) == 0L) {
            Files.deleteIfExists(temp);
            throw new IOException("descarga vacía para " + label);
        }
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyWithProgress(InputStream source, Path target, String label, long total,
                                         ModelSetupProgressListener progress) throws IOException {
        Files.createDirectories(target.getParent());
        long copied = 0L;
        long lastReported = 0L;
        byte[] buffer = new byte[1024 * 1024];
        try (InputStream in = source; OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                out.write(buffer, 0, read);
                copied += read;
                if (copied - lastReported >= PROGRESS_STEP_BYTES || copied == total) {
                    progress.onProgress("Descargando " + label + ": " + formatBytes(copied)
                            + (total > 0 ? " de " + formatBytes(total) : "") + ".");
                    lastReported = copied;
                }
            }
        }
    }

    private static void extractZip(Path zip, Path targetFolder) throws IOException {
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                Path destination = targetFolder.resolve(entry.getName()).normalize();
                if (!destination.startsWith(targetFolder)) {
                    throw new IOException("entrada ZIP fuera de carpeta destino");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(zipInput, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInput.closeEntry();
            }
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
                progress.onProgress("Copiado archivo informativo/legal de Video local: " + name + ".");
            }
        }
        return List.copyOf(copied);
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(directory);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0L) {
            return "tamaño desconocido";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (value >= 1024.0 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}
