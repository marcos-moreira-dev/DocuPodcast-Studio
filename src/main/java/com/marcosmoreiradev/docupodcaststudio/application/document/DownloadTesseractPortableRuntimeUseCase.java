package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
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
import java.util.Comparator;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads a user-configured Tesseract ZIP and imports it into {@code tools/tesseract}. */
public final class DownloadTesseractPortableRuntimeUseCase {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(20);
    private static final long PROGRESS_STEP_BYTES = 4L * 1024L * 1024L;

    private final HttpClient httpClient;
    private final ImportTesseractRuntimeFolderUseCase importer;

    public DownloadTesseractPortableRuntimeUseCase() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build(),
                new ImportTesseractRuntimeFolderUseCase());
    }

    public DownloadTesseractPortableRuntimeUseCase(HttpClient httpClient, ImportTesseractRuntimeFolderUseCase importer) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.importer = Objects.requireNonNull(importer, "importer");
    }

    public TesseractRuntimeImportReport download(OperationalSettings settings, Path applicationRoot) {
        return download(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public TesseractRuntimeImportReport download(OperationalSettings settings, Path applicationRoot,
                                                 ModelSetupProgressListener listener) {
        ModelSetupProgressListener progress = listener == null ? ModelSetupProgressListener.noop() : listener;
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String rawUrl = current.ocr().tesseractRuntimeZipUrl();
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        if (rawUrl == null || rawUrl.isBlank()) {
            return TesseractRuntimeImportReport.failed(null, paths.tesseractRoot(),
                    "No hay URL configurada para descargar Tesseract. Importa una carpeta portable.");
        }
        Path downloads = paths.tesseractRoot().resolve("downloads").normalize();
        Path zip = downloads.resolve("tesseract-runtime.download.zip").normalize();
        Path extracted = downloads.resolve("extracted").normalize();
        try {
            Files.createDirectories(downloads);
            progress.onProgress("Descargando runtime OCR configurado...");
            downloadFile(URI.create(rawUrl.strip()), zip, "runtime OCR", progress);
            progress.onProgress("Extrayendo runtime OCR descargado...");
            recreateDirectory(extracted);
            extractZip(zip, extracted);
            return importer.importFrom(extracted, root, progress);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return TesseractRuntimeImportReport.failed(extracted, paths.tesseractRoot(),
                    "La descarga OCR fue interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return TesseractRuntimeImportReport.failed(extracted, paths.tesseractRoot(),
                    "No se pudo descargar Tesseract OCR: " + ex.getMessage());
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
            throw new IOException("descarga vacia para " + label);
        }
        Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyWithProgress(InputStream source, Path target, String label, long total,
                                         ModelSetupProgressListener progress) throws IOException {
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
                    progress.onProgress("Descargando " + label + ": " + copied + " bytes"
                            + (total > 0 ? " de " + total : "") + ".");
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
                    throw new IOException("ZIP OCR contiene ruta insegura: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(zipInput, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
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
}
