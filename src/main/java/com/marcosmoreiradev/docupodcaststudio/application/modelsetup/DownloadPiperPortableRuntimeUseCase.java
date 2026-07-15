package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.PiperVoiceModelPathPolicy;
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
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the lightweight local voice runtime and a default Spanish voice into the app folder.
 *
 * <p>This use case only runs after explicit user consent from Settings/initial setup. It writes under
 * {@code tools/} and {@code models/} controlled by the application, never under PATH or global Python.</p>
 */
public final class DownloadPiperPortableRuntimeUseCase {
    private static final String OFFICIAL_RUNTIME_ZIP_URL = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip";
    private static final URI RUNTIME_ZIP = URI.create(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_RUNTIME_ZIP_URL);
    private static final URI DEFAULT_VOICE = URI.create(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_URL);
    private static final URI DEFAULT_VOICE_METADATA = URI.create(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_METADATA_URL);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(20);
    private static final long PROGRESS_STEP_BYTES = 4L * 1024L * 1024L;

    private final HttpClient httpClient;
    private final InspectPiperSetupReadinessUseCase inspector;

    public DownloadPiperPortableRuntimeUseCase() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build(),
                new InspectPiperSetupReadinessUseCase());
    }

    public DownloadPiperPortableRuntimeUseCase(HttpClient httpClient, InspectPiperSetupReadinessUseCase inspector) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public PiperRuntimeDownloadReport download(OperationalSettings settings, Path applicationRoot) {
        return download(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public PiperRuntimeDownloadReport download(OperationalSettings settings, Path applicationRoot, ModelSetupProgressListener listener) {
        ModelSetupProgressListener progress = listener == null ? ModelSetupProgressListener.noop() : listener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path piperFolder = paths.piperRoot();
        Path voiceFolder = voiceFolder(root, settings);
        ArrayList<String> downloaded = new ArrayList<>();
        ArrayList<String> skipped = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        try {
            Files.createDirectories(piperFolder);
            Files.createDirectories(voiceFolder);
            Path piperExe = piperFolder.resolve("piper.exe");
            if (Files.isRegularFile(piperExe)) {
                skipped.add("runtime local");
                progress.onProgress("Ya existe Voz local simple; se conserva el runtime local.");
            } else {
                progress.onProgress("Descargando runtime liviano de voz...");
                Path zip = piperFolder.resolve("piper-runtime.download.zip");
                downloadFile(runtimeZipUri(settings), zip, "runtime liviano", progress);
                extractZip(zip, piperFolder);
                Files.deleteIfExists(zip);
                flattenIfNested(piperFolder);
                if (Files.isRegularFile(piperExe)) {
                    downloaded.add("runtime local");
                    progress.onProgress("Runtime liviano preparado dentro del programa.");
                } else {
                    failed.add("runtime local");
                    progress.onProgress("No se encontró el runtime liviano después de descargar.");
                }
            }

            Path model = voiceFolder.resolve(PiperVoiceModelPathPolicy.DEFAULT_PIPER_VOICE);
            Path metadata = voiceFolder.resolve(PiperVoiceModelPathPolicy.DEFAULT_PIPER_VOICE + ".json");
            downloadIfMissing(defaultVoiceUri(settings), model, "voz local simple", downloaded, skipped, failed, progress);
            downloadIfMissing(defaultVoiceMetadataUri(settings), metadata, "datos de voz local simple", downloaded, skipped, failed, progress);

            OperationalSettings effective = settings == null ? OperationalSettings.defaults() : settings;
            OperationalSettings withDefaultVoice = new OperationalSettings(
                    effective.readingDocument(),
                    effective.playbackBuffer(),
                    new OperationalSettings.TtsEngineSettings(
                            effective.tts().engineMode(),
                            effective.tts().commandTemplate(),
                            effective.tts().displayName(),
                            effective.tts().language(),
                            PiperVoiceModelPathPolicy.DEFAULT_PIPER_VOICE,
                            effective.tts().timeoutSeconds(),
                            effective.tts().maxRetries(),
                            effective.tts().xttsDownloadBaseUrl(),
                            effective.tts().piperRuntimeZipUrl(),
                            effective.tts().piperDefaultVoiceUrl(),
                            effective.tts().piperDefaultVoiceMetadataUrl()),
                    effective.video(),
                    effective.imageGeneration(),
                    effective.frameGeneration(),
                    effective.compute(),
                    effective.ocr(),
                    effective.storage(),
                    effective.diagnostics());
            PiperSetupReadinessReport readiness = inspector.inspect(withDefaultVoice, root);
            boolean success = failed.isEmpty() && readiness.ready();
            String message = success
                    ? "Voz local simple descargada y verificada dentro del programa."
                    : "La preparación de Voz local simple terminó con componentes pendientes.";
            progress.onProgress(message);
            return new PiperRuntimeDownloadReport(success, piperFolder, voiceFolder, readiness, downloaded, skipped, failed, message);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failed.add("interrumpido");
            return PiperRuntimeDownloadReport.failed(piperFolder, voiceFolder, "La preparación de Voz local simple fue interrumpida.", failed);
        } catch (IOException | RuntimeException ex) {
            failed.add(ex.getMessage());
            return PiperRuntimeDownloadReport.failed(piperFolder, voiceFolder, "No se pudo preparar Voz local simple: " + ex.getMessage(), failed);
        }
    }


    private static URI runtimeZipUri(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String value = current.tts().piperRuntimeZipUrl();
        return URI.create(value == null || value.isBlank() ? RUNTIME_ZIP.toString() : value.strip());
    }

    private static URI defaultVoiceUri(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String value = current.tts().piperDefaultVoiceUrl();
        return URI.create(value == null || value.isBlank() ? DEFAULT_VOICE.toString() : value.strip());
    }

    private static URI defaultVoiceMetadataUri(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String value = current.tts().piperDefaultVoiceMetadataUrl();
        return URI.create(value == null || value.isBlank() ? DEFAULT_VOICE_METADATA.toString() : value.strip());
    }

    private void downloadIfMissing(URI uri, Path destination, String label, ArrayList<String> downloaded,
                                   ArrayList<String> skipped, ArrayList<String> failed, ModelSetupProgressListener progress)
            throws IOException, InterruptedException {
        if (Files.isRegularFile(destination) && Files.size(destination) > 0L) {
            skipped.add(label);
            progress.onProgress("Ya existe " + label + "; se conserva el archivo local.");
            return;
        }
        progress.onProgress("Descargando " + label + "...");
        downloadFile(uri, destination, label, progress);
        if (Files.isRegularFile(destination) && Files.size(destination) > 0L) {
            downloaded.add(label);
            progress.onProgress("Descargado " + label + " (" + formatBytes(Files.size(destination)) + ").");
        } else {
            failed.add(label);
            progress.onProgress("No se pudo guardar " + label + ".");
        }
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

    private static void flattenIfNested(Path folder) throws IOException {
        Path expected = folder.resolve("piper.exe");
        if (Files.isRegularFile(expected)) {
            return;
        }
        try (var stream = Files.walk(folder, 3)) {
            Path nestedExe = stream
                    .filter(path -> path.getFileName() != null && "piper.exe".equalsIgnoreCase(path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
            if (nestedExe == null || nestedExe.getParent() == null || nestedExe.getParent().equals(folder)) {
                return;
            }
            Path nestedFolder = nestedExe.getParent();
            try (var files = Files.list(nestedFolder)) {
                for (Path source : files.toList()) {
                    Files.move(source, folder.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            try (var cleanup = Files.walk(nestedFolder)) {
                cleanup.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }

    private static Path voiceFolder(Path applicationRoot, OperationalSettings settings) {
        return PiperVoiceModelPathPolicy.voiceDirectory(applicationRoot, settings);
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
        return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}
