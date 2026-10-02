package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import com.marcosmoreiradev.docupodcaststudio.application.observability.SupportBundleExporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes a bounded, sanitized support archive. No project or generated media is copied. */
public final class FileSupportBundleExporter implements SupportBundleExporter {
    static final long MAX_BUNDLE_INPUT_BYTES = 50L * 1024 * 1024;
    private static final int MAX_LOG_FILES = 12;

    @Override public ExportResult export(ExportRequest request) throws IOException {
        if (request == null || request.destination() == null) {
            throw new IllegalArgumentException("support bundle destination is required");
        }
        Path target = zipPath(request.destination()).toAbsolutePath().normalize();
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Path staging = target.resolveSibling(target.getFileName() + ".tmp");
        Files.deleteIfExists(staging);
        DiagnosticSanitizer sanitizer = new DiagnosticSanitizer(request.projectRoot());
        ArrayList<String> omitted = new ArrayList<>();
        long[] consumed = {0L};
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(staging), StandardCharsets.UTF_8)) {
            writeText(zip, "diagnostics.json", diagnosticsJson(request.diagnostics(), sanitizer));
            for (Path log : recentLogs(request.logDirectory())) {
                addSanitizedFile(zip, log, "logs/" + safeName(log), sanitizer, consumed, omitted);
            }
            int manifestIndex = 0;
            for (Path manifest : request.manifests()) {
                if (!allowedManifest(manifest, request.projectRoot())) {
                    omitted.add(label(manifest));
                    continue;
                }
                addSanitizedFile(zip, manifest,
                        "manifests/" + String.format("%02d-", ++manifestIndex) + safeName(manifest),
                        sanitizer, consumed, omitted);
            }
            writeText(zip, "omitted.json", stringArray(omitted));
        } catch (IOException failure) {
            Files.deleteIfExists(staging);
            throw failure;
        }
        move(staging, target);
        return new ExportResult(target, Files.size(target), omitted);
    }

    private static List<Path> recentLogs(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) return List.of();
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(FileSupportBundleExporter::allowedLog)
                    .sorted(Comparator.comparingLong(FileSupportBundleExporter::lastModified).reversed())
                    .limit(MAX_LOG_FILES).toList();
        }
    }

    private static boolean allowedLog(Path path) {
        String name = safeName(path).toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".log") || name.endsWith(".txt") || name.endsWith(".jsonl");
    }

    private static boolean allowedManifest(Path path, Path projectRoot) {
        if (path == null || !Files.isRegularFile(path)) return false;
        String name = safeName(path).toLowerCase(java.util.Locale.ROOT);
        if (!(name.endsWith(".json") || name.endsWith(".jsonl"))) return false;
        if (projectRoot == null) return true;
        return path.toAbsolutePath().normalize().startsWith(projectRoot.toAbsolutePath().normalize());
    }

    private static void addSanitizedFile(ZipOutputStream zip, Path source, String entry,
                                         DiagnosticSanitizer sanitizer, long[] consumed,
                                         List<String> omitted) throws IOException {
        long size = Files.size(source);
        if (size > MAX_BUNDLE_INPUT_BYTES - consumed[0]) {
            omitted.add(label(source));
            return;
        }
        consumed[0] += size;
        writeText(zip, entry, sanitizer.sanitize(Files.readString(source, StandardCharsets.UTF_8)));
    }

    private static String diagnosticsJson(Map<String, String> diagnostics, DiagnosticSanitizer sanitizer) {
        LinkedHashMap<String, String> safe = new LinkedHashMap<>();
        diagnostics.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> safe.put(entry.getKey(), sanitizer.sanitize(entry.getValue())));
        StringBuilder out = new StringBuilder("{\n");
        int index = 0;
        for (var entry : safe.entrySet()) {
            if (index++ > 0) out.append(",\n");
            out.append("  ").append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
        }
        return out.append("\n}\n").toString();
    }

    private static String stringArray(List<String> values) {
        return values.stream().map(FileSupportBundleExporter::quote)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static void writeText(ZipOutputStream zip, String entryName, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName.replace('\\', '/')));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static Path zipPath(Path destination) {
        String name = destination.getFileName().toString();
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                ? destination : destination.resolveSibling(name + ".zip");
    }

    private static String safeName(Path path) {
        return path == null ? "missing" : path.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String label(Path path) { return path == null ? "<missing>" : safeName(path); }
    private static long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return 0L; }
    }
    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
