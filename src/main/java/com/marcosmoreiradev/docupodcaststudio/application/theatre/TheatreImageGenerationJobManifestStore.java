package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Persists a small, restart-safe theatre image job history inside the project. */
public final class TheatreImageGenerationJobManifestStore {
    private static final String RELATIVE_DIRECTORY = "generated/teatro-ia/jobs";

    public List<TheatreImageGenerationJob> load(Path projectRoot) throws IOException {
        Path directory = directory(projectRoot);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        ArrayList<Path> manifests;
        try (var stream = Files.list(directory)) {
            manifests = stream.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        ArrayList<TheatreImageGenerationJob> result = new ArrayList<>();
        for (Path manifest : manifests) {
            read(manifest).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public void save(Path projectRoot, Collection<TheatreImageGenerationJob> jobs) throws IOException {
        Path directory = directory(projectRoot);
        Files.createDirectories(directory);
        Set<String> expected = jobs == null ? Set.of() : jobs.stream()
                .map(job -> fileName(job.id()))
                .collect(Collectors.toSet());
        if (jobs != null) {
            for (TheatreImageGenerationJob job : jobs) {
                write(directory.resolve(fileName(job.id())), job);
            }
        }
        try (var stream = Files.list(directory)) {
            for (Path path : stream.filter(item -> item.getFileName().toString().endsWith(".properties")).toList()) {
                if (!expected.contains(path.getFileName().toString())) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private java.util.Optional<TheatreImageGenerationJob> read(Path path) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            String id = properties.getProperty("id", "").strip();
            if (id.isBlank()) {
                return java.util.Optional.empty();
            }
            TheatreFrameGenerationScope.Kind kind = enumValue(
                    TheatreFrameGenerationScope.Kind.class,
                    properties.getProperty("scope.kind"),
                    TheatreFrameGenerationScope.Kind.ALL);
            TheatreFrameGenerationScope scope = new TheatreFrameGenerationScope(kind, properties.getProperty("scope.id", ""));
            FrameGenerationMode mode = enumValue(FrameGenerationMode.class, properties.getProperty("mode"), FrameGenerationMode.SINGLE);
            TheatreImageGenerationPreset preset = enumValue(
                    TheatreImageGenerationPreset.class,
                    properties.getProperty("preset"),
                    TheatreImageGenerationPreset.TEST_4GB_SD15);
            String output = properties.getProperty("output", "").strip();
            Path outputPath = output.isBlank() ? null : Path.of(output);
            String persistedStatus = properties.getProperty("status", "");
            String status = Boolean.parseBoolean(properties.getProperty("running", "false"))
                    ? persistedStatus + " (interrumpido al cerrar la aplicacion)"
                    : persistedStatus;
            return java.util.Optional.of(new TheatreImageGenerationJob(
                    id, scope, mode, preset, outputPath, List.of(),
                    splitLines(properties.getProperty("references", "")), status, false));
        } catch (IOException | RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    private void write(Path path, TheatreImageGenerationJob job) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", job.id());
        properties.setProperty("scope.kind", job.scope().kind().name());
        properties.setProperty("scope.id", job.scope().id());
        properties.setProperty("mode", job.mode().name());
        properties.setProperty("preset", job.preset().name());
        properties.setProperty("output", job.outputDirectory() == null ? "" : job.outputDirectory().toString());
        properties.setProperty("references", String.join("\n", job.appliedReferences()));
        properties.setProperty("status", job.status());
        properties.setProperty("running", Boolean.toString(job.running()));
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "DocuPodcast Studio theatre image job");
        }
    }

    private static Path directory(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Se requiere un proyecto guardado para persistir trabajos de imagen.");
        }
        return projectRoot.resolve(RELATIVE_DIRECTORY);
    }

    private static String fileName(String id) {
        String safe = (id == null ? "job" : id).replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + ".properties";
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value == null ? "" : value.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::strip).filter(line -> !line.isBlank()).distinct().toList();
    }
}
