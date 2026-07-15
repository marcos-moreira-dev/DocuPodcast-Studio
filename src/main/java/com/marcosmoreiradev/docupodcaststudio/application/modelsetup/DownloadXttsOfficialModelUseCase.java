package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Downloads the official XTTS-v2 model artifact set into {@code models/tts/xtts} after user consent.
 *
 * <p>The use case is not invoked at startup and never runs silently. The presentation layer must ask
 * the user before calling it because the model is large and comes from an external source/license.</p>
 */
public final class DownloadXttsOfficialModelUseCase {
    private static final String OFFICIAL_MODEL_REPOSITORY_URL = "https://huggingface.co/coqui/XTTS-v2";
    private static final String COQUI_TTS_SOURCE_REPOSITORY_URL = "https://github.com/coqui-ai/TTS";
    private static final String DEFAULT_BASE_URL = OperationalSettings.TtsEngineSettings.DEFAULT_XTTS_DOWNLOAD_BASE_URL;
    private static final String DEFAULT_BRANCH = "main";
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(45);
    private static final long PROGRESS_STEP_BYTES = 8L * 1024L * 1024L;

    private static final List<ModelFile> FILES = List.of(
            new ModelFile("config.json", false, true),
            new ModelFile("model.pth", true, true),
            new ModelFile("vocab.json", false, true),
            new ModelFile("speakers_xtts.pth", false, true),
            new ModelFile("dvae.pth", true, true),
            new ModelFile("mel_stats.pth", false, true),
            new ModelFile("LICENSE.txt", false, false),
            new ModelFile("README.md", false, false),
            new ModelFile("hash.md5", false, false)
    );

    private final HttpClient httpClient;
    private final InspectLocalModelFolderUseCase inspector;

    public DownloadXttsOfficialModelUseCase() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build(),
                new InspectLocalModelFolderUseCase());
    }

    public DownloadXttsOfficialModelUseCase(HttpClient httpClient, InspectLocalModelFolderUseCase inspector) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public XttsModelDownloadReport download(OperationalSettings settings, Path applicationRoot) {
        return download(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsModelDownloadReport download(OperationalSettings settings, Path applicationRoot,
                                            ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path target = targetDirectory(current, root);
        Path diagnosticReport = diagnosticReportPath(target);
        ArrayList<String> downloaded = new ArrayList<>();
        ArrayList<String> skipped = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        ArrayList<String> diagnostics = new ArrayList<>();
        ModelInspectionResult inspection = null;
        String finalMessage = "Descarga de Voz IA avanzada iniciada.";
        diagnostics.add("Inicio: " + Instant.now());
        diagnostics.add("Carpeta destino: " + target);
        diagnostics.add("Fuente configurada: " + Objects.toString(current.tts().xttsDownloadBaseUrl(), "predeterminada"));
        try {
            Files.createDirectories(target);
            progress.onProgress("Carpeta de modelo: " + target + ".");
            String configuredRepository = current.tts().xttsDownloadBaseUrl();
            String normalizedRepository = normalizeRepositoryUrlForDisplay(configuredRepository);
            String resolveBase = normalizeDownloadResolveBaseUrl(configuredRepository);
            diagnostics.add("Fuente normalizada: " + normalizedRepository);
            diagnostics.add("Base técnica de descarga: " + resolveBase);
            diagnostics.add("Recursos esperados: " + expectedFileNames());
            if (configuredRepository != null && !configuredRepository.isBlank()
                    && !trimTrailingSlash(stripQueryAndFragment(configuredRepository)).equals(normalizedRepository)) {
                progress.onProgress("Fuente de modelo normalizada a página oficial de voz: " + normalizedRepository + ".");
            }
            for (ModelFile file : FILES) {
                Path destination = target.resolve(file.name()).normalize();
                diagnostics.add("---");
                diagnostics.add("Recurso: " + file.name() + " | requerido=" + file.required() + " | grande=" + file.large());
                diagnostics.add("Destino: " + destination);
                if (Files.isRegularFile(destination) && Files.size(destination) > 0L) {
                    long size = Files.size(destination);
                    skipped.add(file.name());
                    diagnostics.add("Estado: ya existía localmente | tamaño=" + size + " bytes");
                    progress.onProgress("Ya existe " + file.name() + "; se conserva el archivo local.");
                    continue;
                }
                URI uri = modelFileUri(settings, file.name());
                diagnostics.add("URL técnica: " + uri);
                Path temp = destination.resolveSibling(destination.getFileName() + ".download");
                long existingPartialBytes = Files.isRegularFile(temp) ? Files.size(temp) : 0L;
                diagnostics.add("Parcial previo: " + existingPartialBytes + " bytes");
                progress.onProgress(downloadStartMessage(file, existingPartialBytes));
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                        .timeout(file.large() ? REQUEST_TIMEOUT : Duration.ofMinutes(5))
                        .header("User-Agent", "DocuPodcast-Studio model-setup")
                        .header("Accept", "application/octet-stream, application/json;q=0.9, */*;q=0.8")
                        .GET();
                if (existingPartialBytes > 0L) {
                    requestBuilder.header("Range", "bytes=" + existingPartialBytes + "-");
                }
                HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                diagnostics.add("HTTP: " + response.statusCode());
                diagnostics.add("Content-Length: " + response.headers().firstValue("Content-Length").orElse("no informado"));
                diagnostics.add("Content-Type: " + response.headers().firstValue("Content-Type").orElse("no informado"));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String detail = humanFileName(file.name()) + " (" + file.name() + ", HTTP " + response.statusCode() + ", URL " + uri + ")";
                    diagnostics.add("Estado: fallo HTTP");
                    if (file.required()) {
                        failed.add(detail);
                        progress.onProgress("Falta un recurso obligatorio de voz: " + humanFileName(file.name()) + " (HTTP " + response.statusCode() + ").");
                    } else {
                        skipped.add(file.name() + " opcional no disponible");
                        progress.onProgress("Recurso opcional no disponible; se continuará sin bloquear la voz.");
                    }
                    continue;
                }
                boolean appendPartial = existingPartialBytes > 0L && response.statusCode() == 206;
                if (existingPartialBytes > 0L && !appendPartial) {
                    Files.deleteIfExists(temp);
                    diagnostics.add("Reanudación rechazada por servidor; se reinicia este recurso.");
                    progress.onProgress("El servidor no reanudó " + file.name() + "; se reinicia esa descarga sin perder los otros recursos.");
                    existingPartialBytes = 0L;
                }
                long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (appendPartial && total > 0L) {
                    total += existingPartialBytes;
                }
                copyWithProgress(response.body(), temp, file.name(), existingPartialBytes, total, appendPartial, progress);
                if (!Files.isRegularFile(temp) || Files.size(temp) == 0L) {
                    Files.deleteIfExists(temp);
                    diagnostics.add("Estado: descarga vacía o archivo temporal inexistente.");
                    if (file.required()) {
                        failed.add(humanFileName(file.name()) + " (" + file.name() + ") llegó vacío");
                        progress.onProgress("Un recurso obligatorio de voz llegó vacío; se descartó.");
                    } else {
                        skipped.add(file.name() + " opcional vacío");
                        progress.onProgress("Un recurso opcional llegó vacío; se continuará sin bloquear la voz.");
                    }
                    continue;
                }
                moveDownloadedFile(temp, destination);
                long finalSize = Files.size(destination);
                downloaded.add(file.name());
                diagnostics.add("Estado: descargado | tamaño=" + finalSize + " bytes");
                progress.onProgress("Descargado " + file.name() + " (" + formatBytes(finalSize) + ").");
            }
            progress.onProgress("Verificando contrato del modelo descargado...");
            inspection = inspector.inspect(ModelFolderContract.xttsHighQuality(), target);
            diagnostics.add("---");
            diagnostics.add("Inspección: " + inspection.status() + " | usable=" + inspection.usable());
            diagnostics.add("Mensaje de inspección: " + inspection.userMessage());
            diagnostics.add("Faltantes de inspección: " + inspection.missingRequirements());
            diagnostics.add("Archivos descubiertos: " + inspection.discoveredFiles());
            diagnostics.add("Manifiesto checksum presente: " + inspection.checksumManifestPresent());
            boolean success = failed.isEmpty() && inspection.usable();
            finalMessage = success
                    ? "Voz IA avanzada descargada y verificada dentro del programa."
                    : "La descarga terminó con recursos obligatorios pendientes. Revisa el detalle mostrado en esta ventana.";
            diagnostics.add("Resultado final: success=" + success);
            diagnostics.add("Descargados: " + downloaded);
            diagnostics.add("Ya disponibles/u opcionales omitidos: " + skipped);
            diagnostics.add("Fallidos: " + failed);
            writeDiagnosticReportQuietly(diagnosticReport, diagnostics);
            progress.onProgress("Reporte técnico guardado en: " + diagnosticReport + ".");
            progress.onProgress(finalMessage);
            return new XttsModelDownloadReport(success, target, downloaded, skipped, failed, inspection, finalMessage, diagnosticReport);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failed.add("interrumpido");
            diagnostics.add("Interrumpido: " + readableException(ex));
            writeDiagnosticReportQuietly(diagnosticReport, diagnostics);
            return XttsModelDownloadReport.failed(target, "La descarga del modelo fue interrumpida.", failed, diagnosticReport);
        } catch (IOException | RuntimeException ex) {
            failed.add(readableException(ex));
            diagnostics.add("Excepción: " + ex.getClass().getName() + ": " + readableException(ex));
            writeDiagnosticReportQuietly(diagnosticReport, diagnostics);
            return XttsModelDownloadReport.failed(target, "No se pudo descargar Voz IA avanzada: " + readableException(ex), failed, diagnosticReport);
        }
    }

    public static Path diagnosticReportPath(Path targetFolder) {
        Path target = targetFolder == null ? Path.of("models/tts/xtts") : targetFolder;
        return target.resolve("download-diagnostics.txt").normalize();
    }

    private static void writeDiagnosticReport(Path report, List<String> lines) throws IOException {
        if (report == null) {
            return;
        }
        Files.createDirectories(report.getParent());
        ArrayList<String> output = new ArrayList<>();
        output.add("DocuPodcast Studio - Diagnóstico de descarga de Voz IA avanzada");
        output.add("Generado: " + Instant.now());
        output.add("");
        output.addAll(lines == null ? List.of() : lines);
        Files.write(report, output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeDiagnosticReportQuietly(Path report, List<String> lines) {
        try {
            writeDiagnosticReport(report, lines);
        } catch (IOException ignored) {
            // The original download/preparation failure is more important than a secondary report-write failure.
        }
    }

    public static List<String> expectedFileNames() {
        return FILES.stream().map(ModelFile::name).toList();
    }

    private static URI modelFileUri(OperationalSettings settings, String fileName) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String base = normalizeDownloadResolveBaseUrl(current.tts().xttsDownloadBaseUrl());
        String suffix = fileName + "?download=1";
        return URI.create(base + suffix);
    }

    /**
     * Returns the human-facing repository URL.
     *
     * <p>Users tend to paste/open the visible URL from Settings. A raw /resolve/main endpoint is not
     * a browsable page and looks like a 404 when opened without a file name, even though file downloads
     * may work. The UI therefore stores/displays the repository page while this downloader internally
     * expands it to per-file /resolve/main URLs.</p>
     */
    public static String normalizeRepositoryUrlForDisplay(String configured) {
        String value = configured == null || configured.isBlank() ? DEFAULT_BASE_URL : configured.strip();
        value = stripQueryAndFragment(value);
        value = trimTrailingSlash(value);
        value = stripKnownFileName(value);
        if (isCoquiTtsCodeRepository(value)) {
            return OFFICIAL_MODEL_REPOSITORY_URL;
        }
        int resolve = value.indexOf("/resolve/");
        if (resolve >= 0) {
            return value.substring(0, resolve);
        }
        int tree = value.indexOf("/tree/");
        if (tree >= 0) {
            return value.substring(0, tree);
        }
        int blob = value.indexOf("/blob/");
        if (blob >= 0) {
            return value.substring(0, blob);
        }
        return value.isBlank() ? OFFICIAL_MODEL_REPOSITORY_URL : value;
    }

    static String normalizeDownloadResolveBaseUrl(String configured) {
        String repository = normalizeRepositoryUrlForDisplay(configured);
        if (repository.contains("/resolve/")) {
            repository = normalizeRepositoryUrlForDisplay(repository);
        }
        if (repository.isBlank()) {
            repository = OFFICIAL_MODEL_REPOSITORY_URL;
        }
        return trimTrailingSlash(repository) + "/resolve/" + DEFAULT_BRANCH + "/";
    }

    private static boolean isCoquiTtsCodeRepository(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals(COQUI_TTS_SOURCE_REPOSITORY_URL.toLowerCase(java.util.Locale.ROOT))
                || normalized.equals((COQUI_TTS_SOURCE_REPOSITORY_URL + ".git").toLowerCase(java.util.Locale.ROOT))
                || normalized.startsWith((COQUI_TTS_SOURCE_REPOSITORY_URL + "/").toLowerCase(java.util.Locale.ROOT))
                || normalized.startsWith((COQUI_TTS_SOURCE_REPOSITORY_URL + ".git/").toLowerCase(java.util.Locale.ROOT));
    }

    private static String stripQueryAndFragment(String value) {
        String result = value == null ? "" : value.strip();
        int query = result.indexOf('?');
        if (query >= 0) {
            result = result.substring(0, query);
        }
        int fragment = result.indexOf('#');
        if (fragment >= 0) {
            result = result.substring(0, fragment);
        }
        return result.strip();
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.strip();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String stripKnownFileName(String value) {
        String result = trimTrailingSlash(value);
        for (ModelFile file : FILES) {
            String marker = "/" + file.name();
            if (result.endsWith(marker)) {
                return result.substring(0, result.length() - marker.length());
            }
        }
        return result;
    }

    private static void moveDownloadedFile(Path temp, Path destination) throws IOException {
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyWithProgress(InputStream source, Path target, String fileName, long alreadyCopied,
                                         long total, boolean append,
                                         ModelSetupProgressListener progress) throws IOException {
        Files.createDirectories(target.getParent());
        long copied = Math.max(0L, alreadyCopied);
        long lastReported = copied;
        byte[] buffer = new byte[1024 * 1024];
        StandardOpenOption[] options = append
                ? new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.APPEND }
                : new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };
        try (InputStream in = source; OutputStream out = Files.newOutputStream(target, options)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                out.write(buffer, 0, read);
                copied += read;
                if (copied - lastReported >= PROGRESS_STEP_BYTES || copied == total) {
                    progress.onProgress("Descargando " + fileName + ": " + formatBytes(copied)
                            + (total > 0 ? " de " + formatBytes(total) : "") + ".");
                    lastReported = copied;
                }
            }
        }
    }

    private static String downloadStartMessage(ModelFile file, long existingPartialBytes) {
        String base = "Descargando " + file.name() + (file.large() ? " (archivo grande)" : "");
        if (existingPartialBytes > 0L) {
            return base + ": se reanudará desde " + formatBytes(existingPartialBytes) + ".";
        }
        return base + "...";
    }

    private static String humanFileName(String fileName) {
        return switch (fileName) {
            case "config.json" -> "configuración del modelo";
            case "model.pth" -> "archivo principal de voz";
            case "vocab.json" -> "vocabulario";
            case "speakers_xtts.pth" -> "referencias internas de voz";
            case "dvae.pth" -> "codificador de audio";
            case "mel_stats.pth" -> "estadísticas de audio";
            default -> "recurso opcional";
        };
    }

    private static String readableException(Throwable error) {
        if (error == null) {
            return "error desconocido";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.strip();
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

    private static Path targetDirectory(OperationalSettings current, Path root) {
        Path portableTarget = XttsModelPathPolicy.portableModelDirectory(root);
        if (XttsModelPathPolicy.usableXttsModelFolder(portableTarget)) {
            return portableTarget;
        }
        return XttsModelPathPolicy.modelDirectoryFromSettings(current, root);
    }

    private record ModelFile(String name, boolean large, boolean required) {
    }
}
