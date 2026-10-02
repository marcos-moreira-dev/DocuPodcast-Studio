package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadDecision;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Filesystem and download operations confined to the configured writable runtime root. */
final class SafeRuntimeOperations {
    private static final long PARALLEL_DOWNLOAD_THRESHOLD = 512L * 1024L * 1024L;
    private static final int PARALLEL_DOWNLOAD_SEGMENTS = 4;

    @FunctionalInterface
    interface PathValidator {
        void validate(Path path) throws IOException;
    }

    private final Path runtimeRoot;

    SafeRuntimeOperations(Path runtimeRoot) {
        this.runtimeRoot = runtimeRoot.toAbsolutePath().normalize();
    }

    Path runtimeRoot() {
        return runtimeRoot;
    }

    Path target(String relative) throws IOException {
        if (relative == null || relative.isBlank()) throw new IOException("Falta el destino administrado.");
        Path target = runtimeRoot.resolve(relative).normalize();
        if (!target.startsWith(runtimeRoot) || target.equals(runtimeRoot)) {
            throw new IOException("El destino sale del runtime administrado.");
        }
        return target;
    }

    void download(String sourceUrl, String relativeTarget, String owner, ExecutionContext context)
            throws IOException, InterruptedException {
        downloadVerified(sourceUrl, relativeTarget, owner, context, ignored -> { });
    }

    void downloadVerified(String sourceUrl, String relativeTarget, String owner,
                          ExecutionContext context, PathValidator validator)
            throws IOException, InterruptedException {
        downloadVerified(sourceUrl, relativeTarget, owner, context,
                ManagedDownloadDecision.USE_EXISTING, validator);
    }

    void downloadVerified(String sourceUrl, String relativeTarget, String owner,
                          ExecutionContext context, ManagedDownloadDecision decision,
                          PathValidator validator)
            throws IOException, InterruptedException {
        URI source = URI.create(required(sourceUrl, "URL de origen"));
        if (!"https".equalsIgnoreCase(source.getScheme()) && !"http".equalsIgnoreCase(source.getScheme())) {
            throw new IOException("Solo se admiten descargas HTTP/HTTPS.");
        }
        Path target = target(relativeTarget);
        Path partial = staging(owner).resolve(target.getFileName() + ".partial");
        ManagedDownloadDecision currentDecision = decision == null
                ? ManagedDownloadDecision.CANCEL : decision;
        if (currentDecision == ManagedDownloadDecision.CANCEL) {
            throw new IOException("La descarga administrada fue cancelada.");
        }
        if (Files.isRegularFile(target) && validates(target, validator)) {
            if (currentDecision == ManagedDownloadDecision.USE_EXISTING) {
                context.progress().report("REUSING", 1.0,
                        "El recurso instalado es válido; se reutiliza sin acceder a la red.");
                return;
            }
        } else if (Files.exists(target) && currentDecision != ManagedDownloadDecision.REDOWNLOAD) {
            throw new IOException("El recurso local existe pero no es válido. "
                    + "Se requiere la decisión explícita de reparar o volver a descargar.");
        }
        if (Files.exists(partial) && currentDecision != ManagedDownloadDecision.REDOWNLOAD) {
            throw new IOException("Existe una descarga parcial. "
                    + "Se requiere la decisión explícita de reparar o volver a descargar.");
        }
        Files.createDirectories(partial.getParent());
        context.progress().report("DOWNLOADING", 0.02, "Descargando recurso al staging administrado.");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        long expected = contentLength(client, source);
        if (expected >= PARALLEL_DOWNLOAD_THRESHOLD) {
            if (nativeCurlAvailable()) {
                downloadRangesWithCurl(source, partial, expected, context);
            } else {
                downloadRangesWithJava(client, source, partial, expected, context);
            }
        } else {
            downloadSingle(client, source, partial, expected, context);
        }
        validator.validate(partial);
        context.cancellation().throwIfCancellationRequested();
        if (Files.isRegularFile(target) && validates(target, validator)
                && currentDecision != ManagedDownloadDecision.REDOWNLOAD) {
            Files.deleteIfExists(partial);
            context.progress().report("REUSING", 1.0,
                    "Otro proceso publicó un recurso válido; se conserva y reutiliza.");
            return;
        }
        promote(partial, target);
    }

    private static long contentLength(HttpClient client, URI source)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(source)
                .timeout(Duration.ofMinutes(2))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return -1L;
        return response.headers().firstValueAsLong("Content-Length").orElse(-1L);
    }

    private static void downloadSingle(
            HttpClient client,
            URI source,
            Path partial,
            long expected,
            ExecutionContext context) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(source).timeout(Duration.ofHours(2)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("La descarga respondió HTTP " + response.statusCode() + ".");
        }
        long length = expected > 0
                ? expected : response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        try (InputStream input = response.body(); var output = Files.newOutputStream(partial)) {
            copyDownload(input, output, new AtomicLong(), new AtomicLong(-1L), length, context);
        }
    }

    private static void downloadRangesWithJava(
            HttpClient client,
            URI source,
            Path partial,
            long expected,
            ExecutionContext context) throws IOException, InterruptedException {
        ArrayList<Path> segments = new ArrayList<>();
        for (int index = 0; index < PARALLEL_DOWNLOAD_SEGMENTS; index++) {
            segments.add(partial.resolveSibling(partial.getFileName() + ".segment-" + index));
        }
        AtomicLong copied = new AtomicLong();
        AtomicLong lastReportedBlock = new AtomicLong(-1L);
        try (var executor = Executors.newFixedThreadPool(PARALLEL_DOWNLOAD_SEGMENTS)) {
            ArrayList<Future<Void>> futures = new ArrayList<>();
            long base = expected / PARALLEL_DOWNLOAD_SEGMENTS;
            for (int index = 0; index < PARALLEL_DOWNLOAD_SEGMENTS; index++) {
                int current = index;
                long start = base * current;
                long end = current + 1 == PARALLEL_DOWNLOAD_SEGMENTS
                        ? expected - 1 : base * (current + 1) - 1;
                Path segment = segments.get(current);
                futures.add(executor.submit(() -> {
                    HttpRequest request = HttpRequest.newBuilder(source)
                            .timeout(Duration.ofHours(2))
                            .header("Range", "bytes=" + start + "-" + end)
                            .GET()
                            .build();
                    HttpResponse<InputStream> response =
                            client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 206) {
                        throw new IOException("La descarga segmentada respondió HTTP "
                                + response.statusCode() + " para el segmento " + current + ".");
                    }
                    try (InputStream input = response.body();
                         var output = Files.newOutputStream(segment)) {
                        copyDownload(input, output, copied, lastReportedBlock, expected, context);
                    }
                    long segmentLength = end - start + 1;
                    if (Files.size(segment) != segmentLength) {
                        throw new IOException("El segmento " + current + " quedó incompleto.");
                    }
                    return null;
                }));
            }
            awaitDownloads(futures);
            context.cancellation().throwIfCancellationRequested();
            try (var output = Files.newOutputStream(partial)) {
                for (Path segment : segments) Files.copy(segment, output);
            }
            if (Files.size(partial) != expected) {
                throw new IOException("La descarga segmentada no coincide con el tamaño anunciado.");
            }
        } finally {
            for (Path segment : segments) Files.deleteIfExists(segment);
        }
    }

    private static boolean nativeCurlAvailable() {
        try {
            Process probe = new ProcessBuilder("curl", "--version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            Instant started = OwnedProcessDiagnostics.started("CURL_PROBE", probe,
                    List.of("curl", "--version"), "SafeRuntimeOperations",
                    "capability probe completion", "process-handle");
            boolean completed = probe.waitFor(10, TimeUnit.SECONDS);
            if (!completed) probe.destroyForcibly();
            OwnedProcessDiagnostics.stopped("CURL_PROBE", probe, "SafeRuntimeOperations",
                    started, completed ? "NORMAL_COMPLETION" : "TIMEOUT",
                    probe.isAlive() ? null : probe.exitValue(), !completed);
            return completed && probe.exitValue() == 0;
        } catch (IOException ignored) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void downloadRangesWithCurl(
            URI source,
            Path partial,
            long expected,
            ExecutionContext context) throws IOException, InterruptedException {
        ArrayList<Path> segments = new ArrayList<>();
        ArrayList<Process> processes = new ArrayList<>();
        long base = expected / PARALLEL_DOWNLOAD_SEGMENTS;
        try {
            for (int index = 0; index < PARALLEL_DOWNLOAD_SEGMENTS; index++) {
                long start = base * index;
                long end = index + 1 == PARALLEL_DOWNLOAD_SEGMENTS
                        ? expected - 1 : base * (index + 1) - 1;
                Path segment = partial.resolveSibling(partial.getFileName() + ".segment-" + index);
                segments.add(segment);
                processes.add(new ProcessBuilder(
                        "curl",
                        "--location",
                        "--fail",
                        "--silent",
                        "--show-error",
                        "--retry", "3",
                        "--connect-timeout", "30",
                        "--max-time", "7200",
                        "--range", start + "-" + end,
                        "--output", segment.toString(),
                        source.toString())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start());
            }
            long lastReported = -1L;
            boolean running;
            do {
                context.cancellation().throwIfCancellationRequested();
                running = processes.stream().anyMatch(Process::isAlive);
                long copied = 0L;
                for (Path segment : segments) {
                    if (Files.isRegularFile(segment)) copied += Files.size(segment);
                }
                long block = copied / (16L * 1024L * 1024L);
                if (block > lastReported) {
                    lastReported = block;
                    double ratio = Math.min(0.85, 0.02 + copied / (double) expected * 0.8);
                    context.progress().report("DOWNLOADING", ratio,
                            "Descargados " + copied + " de " + expected
                                    + " bytes mediante transporte segmentado.");
                }
                if (running) Thread.sleep(1_000L);
            } while (running);
            for (int index = 0; index < processes.size(); index++) {
                if (processes.get(index).exitValue() != 0) {
                    throw new IOException("El transporte nativo falló en el segmento " + index + ".");
                }
                long start = base * index;
                long end = index + 1 == PARALLEL_DOWNLOAD_SEGMENTS
                        ? expected - 1 : base * (index + 1) - 1;
                if (Files.size(segments.get(index)) != end - start + 1) {
                    throw new IOException("El segmento " + index + " quedó incompleto.");
                }
            }
            try (var output = Files.newOutputStream(partial)) {
                for (Path segment : segments) Files.copy(segment, output);
            }
            if (Files.size(partial) != expected) {
                throw new IOException("La descarga segmentada no coincide con el tamaño anunciado.");
            }
        } finally {
            for (Process process : processes) {
                if (process.isAlive()) process.destroyForcibly();
            }
            for (Path segment : segments) Files.deleteIfExists(segment);
        }
    }

    private static void awaitDownloads(List<Future<Void>> futures)
            throws IOException, InterruptedException {
        try {
            for (Future<Void> future : futures) future.get();
        } catch (ExecutionException failure) {
            for (Future<Void> future : futures) future.cancel(true);
            Throwable cause = failure.getCause();
            if (cause instanceof IOException io) throw io;
            if (cause instanceof InterruptedException interrupted) throw interrupted;
            throw new IOException("Falló la descarga segmentada.", cause);
        }
    }

    private static void copyDownload(
            InputStream input,
            java.io.OutputStream output,
            AtomicLong copied,
            AtomicLong lastReportedBlock,
            long expected,
            ExecutionContext context) throws IOException, InterruptedException {
        byte[] buffer = new byte[128 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            context.cancellation().throwIfCancellationRequested();
            output.write(buffer, 0, read);
            long total = copied.addAndGet(read);
            long block = total / (16L * 1024L * 1024L);
            long previous = lastReportedBlock.get();
            if (block > previous && lastReportedBlock.compareAndSet(previous, block)) {
                double ratio = expected > 0
                        ? Math.min(0.85, 0.02 + total / (double) expected * 0.8) : 0.4;
                context.progress().report("DOWNLOADING", ratio,
                        "Descargados " + total + " de " + expected + " bytes.");
            }
        }
    }

    void importFile(Path source, String relativeTarget, ExecutionContext context)
            throws IOException, InterruptedException {
        importFileVerified(source, relativeTarget, context, ignored -> { });
    }

    void importFileVerified(Path source, String relativeTarget, ExecutionContext context,
                            PathValidator validator)
            throws IOException, InterruptedException {
        Path normalizedSource = requireExisting(source);
        if (!Files.isRegularFile(normalizedSource, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El origen debe ser un archivo regular.");
        }
        Path target = target(relativeTarget);
        context.cancellation().throwIfCancellationRequested();
        Path partial = staging("file-import").resolve(target.getFileName() + ".partial");
        Files.createDirectories(partial.getParent());
        Files.copy(normalizedSource, partial, StandardCopyOption.REPLACE_EXISTING);
        validator.validate(partial);
        context.cancellation().throwIfCancellationRequested();
        promote(partial, target);
    }

    void importDirectory(Path source, String relativeTarget, ExecutionContext context)
            throws IOException, InterruptedException {
        importDirectoryVerified(source, relativeTarget, context, ignored -> { });
    }

    void importDirectoryVerified(Path source, String relativeTarget, ExecutionContext context,
                                 PathValidator validator)
            throws IOException, InterruptedException {
        Path normalizedSource = requireExisting(source);
        if (!Files.isDirectory(normalizedSource, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El origen debe ser un directorio.");
        }
        Path target = target(relativeTarget);
        Path partial = staging("directory-import").resolve(target.getFileName() + "-partial");
        deleteOwnedTree(partial, staging("directory-import"));
        copyDirectory(normalizedSource, partial, context);
        validator.validate(partial);
        context.cancellation().throwIfCancellationRequested();
        promoteDirectoryWithRollback(partial, target, staging("directory-import"));
    }

    void installZipDirectoryVerified(Path archive,
                                     String relativeTarget,
                                     String owner,
                                     ExecutionContext context,
                                     PathValidator validator)
            throws IOException, InterruptedException {
        Path source = requireExisting(archive);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El paquete del nodo debe ser un ZIP regular.");
        }
        Path target = target(relativeTarget);
        Path stagingRoot = staging(owner);
        Path partial = stagingRoot.resolve("expanded-" + target.getFileName());
        deleteOwnedTree(partial, stagingRoot);
        Files.createDirectories(partial);
        context.progress().report("INSTALLING", 0.86, "Validando y expandiendo el nodo administrado.");
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                context.cancellation().throwIfCancellationRequested();
                String normalizedName = entry.getName().replace('\\', '/');
                int slash = normalizedName.indexOf('/');
                String stripped = slash < 0 ? normalizedName : normalizedName.substring(slash + 1);
                if (stripped.isBlank()) continue;
                Path destination = partial.resolve(stripped).normalize();
                if (!destination.startsWith(partial)) {
                    throw new IOException("El ZIP contiene una ruta fuera del destino administrado.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException | InterruptedException ex) {
            deleteOwnedTree(partial, stagingRoot);
            throw ex;
        }
        validator.validate(partial);
        Path backup = stagingRoot.resolve("previous-" + target.getFileName());
        deleteOwnedTree(backup, stagingRoot);
        if (Files.exists(target)) Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.createDirectories(target.getParent());
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            deleteOwnedTree(backup, stagingRoot);
        } catch (IOException failure) {
            if (!Files.exists(target) && Files.exists(backup)) {
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
            }
            throw failure;
        }
    }

    void installZipDirectoryPreservingRootVerified(Path archive,
                                                   String relativeTarget,
                                                   String owner,
                                                   ExecutionContext context,
                                                   PathValidator validator)
            throws IOException, InterruptedException {
        Path source = requireExisting(archive);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El paquete debe ser un ZIP regular.");
        }
        Path target = target(relativeTarget);
        Path stagingRoot = staging(owner);
        Path partial = stagingRoot.resolve("expanded-" + target.getFileName());
        deleteOwnedTree(partial, stagingRoot);
        Files.createDirectories(partial);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                context.cancellation().throwIfCancellationRequested();
                Path destination = partial.resolve(entry.getName().replace('\\', '/')).normalize();
                if (!destination.startsWith(partial)) {
                    throw new IOException("El ZIP contiene una ruta fuera del destino administrado.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException | InterruptedException failure) {
            deleteOwnedTree(partial, stagingRoot);
            throw failure;
        }
        validator.validate(partial);
        context.cancellation().throwIfCancellationRequested();
        Path backup = stagingRoot.resolve("previous-" + target.getFileName());
        deleteOwnedTree(backup, stagingRoot);
        if (Files.exists(target)) Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.createDirectories(target.getParent());
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            deleteOwnedTree(backup, stagingRoot);
        } catch (IOException failure) {
            if (!Files.exists(target) && Files.exists(backup)) {
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
            }
            throw failure;
        }
    }

    /** Expands several flat/complementary ZIPs into one staging tree and promotes it atomically. */
    void installZipOverlayDirectoryVerified(List<Path> archives,
                                            String relativeTarget,
                                            String owner,
                                            ExecutionContext context,
                                            PathValidator validator)
            throws IOException, InterruptedException {
        if (archives == null || archives.isEmpty()) {
            throw new IOException("Faltan los paquetes del runtime.");
        }
        Path target = target(relativeTarget);
        Path stagingRoot = staging(owner);
        Path partial = stagingRoot.resolve("expanded-" + target.getFileName());
        deleteOwnedTree(partial, stagingRoot);
        Files.createDirectories(partial);
        try {
            for (Path archive : archives) {
                Path source = requireExisting(archive);
                if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Cada paquete del runtime debe ser un ZIP regular.");
                }
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        context.cancellation().throwIfCancellationRequested();
                        Path destination = partial.resolve(
                                entry.getName().replace('\\', '/')).normalize();
                        if (!destination.startsWith(partial)) {
                            throw new IOException("El ZIP contiene una ruta fuera del destino administrado.");
                        }
                        if (entry.isDirectory()) {
                            Files.createDirectories(destination);
                        } else {
                            Files.createDirectories(destination.getParent());
                            Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            validator.validate(partial);
            context.cancellation().throwIfCancellationRequested();
            promoteDirectoryWithRollback(partial, target, stagingRoot);
        } catch (IOException | InterruptedException failure) {
            deleteOwnedTree(partial, stagingRoot);
            throw failure;
        }
    }

    private static void promoteDirectoryWithRollback(Path partial, Path target, Path stagingRoot)
            throws IOException {
        Path backup = stagingRoot.resolve("previous-" + target.getFileName());
        deleteOwnedTree(backup, stagingRoot);
        if (Files.exists(target)) Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.createDirectories(target.getParent());
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            deleteOwnedTree(backup, stagingRoot);
        } catch (IOException failure) {
            if (!Files.exists(target) && Files.exists(backup)) {
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
            }
            throw failure;
        }
    }

    void repair(String owner, ExecutionContext context) throws IOException, InterruptedException {
        Path staging = staging(owner);
        context.progress().report("REPAIRING", 0.3, "Limpiando staging administrado del motor.");
        deleteOwnedTree(staging, target(".staging"));
    }

    Path staging(String owner) throws IOException {
        String safeOwner = owner == null ? "engine" : owner.replaceAll("[^a-zA-Z0-9._-]", "-");
        return target(".staging/" + safeOwner);
    }

    private void copyDirectory(Path source, Path target, ExecutionContext context)
            throws IOException, InterruptedException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                context.cancellation().throwIfCancellationRequested();
                if (Files.isSymbolicLink(path)) throw new IOException("No se importan enlaces simbolicos.");
                Path destination = target.resolve(source.relativize(path)).normalize();
                if (!destination.startsWith(target)) throw new IOException("Ruta de importacion invalida.");
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(destination);
                else Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path requireExisting(Path source) throws IOException {
        if (source == null) throw new IOException("Falta el recurso de origen.");
        Path normalized = source.toAbsolutePath().normalize();
        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("No existe el recurso de origen: " + normalized);
        }
        return normalized;
    }

    private static boolean validates(Path path, PathValidator validator) {
        try {
            validator.validate(path);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static void deleteOwnedTree(Path root, Path ownerRoot) throws IOException {
        if (root == null || ownerRoot == null || !root.normalize().startsWith(ownerRoot.normalize())
                || root.normalize().equals(ownerRoot.normalize()) || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void promote(Path partial, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String required(String value, String label) {
        String current = value == null ? "" : value.strip();
        if (current.isBlank()) throw new IllegalArgumentException("Falta " + label + ".");
        return current;
    }
}
