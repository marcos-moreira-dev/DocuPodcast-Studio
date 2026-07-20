package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Versioned project-local store for neutral jobs, with read-only legacy discovery. */
public final class FileGenerationJobRepository implements GenerationJobRepository {
    private static final String FILE_NAME = "job.json";
    private final Path projectRoot;
    private final Path generationRoot;

    public FileGenerationJobRepository(Path projectRoot) {
        if (projectRoot == null) throw new IllegalArgumentException("project root is required");
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.generationRoot = this.projectRoot.resolve("jobs/generation");
    }

    @Override public synchronized void save(GenerationJobSnapshot snapshot) {
        Path directory = jobDirectory(snapshot.request());
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(FILE_NAME);
            Path staging = directory.resolve(FILE_NAME + ".tmp");
            Files.writeString(staging, json(snapshot), StandardCharsets.UTF_8);
            move(staging, target);
        } catch (IOException ex) {
            throw new IllegalStateException("could not persist generation job " + snapshot.request().jobId(), ex);
        }
    }

    @Override public synchronized Optional<GenerationJobSnapshot> find(GenerationJobId id) {
        if (id == null) return Optional.empty();
        if (Files.isDirectory(generationRoot)) {
            try (var capabilities = Files.list(generationRoot)) {
                Optional<Path> file = capabilities.map(path -> path.resolve(id.value()).resolve(FILE_NAME))
                        .filter(Files::isRegularFile).findFirst();
                if (file.isPresent()) return Optional.of(read(file.get()));
            } catch (IOException ex) {
                throw new IllegalStateException("could not read generation job " + id, ex);
            }
        }
        return readLegacy(id);
    }

    @Override public synchronized List<GenerationJobSnapshot> list() {
        ArrayList<GenerationJobSnapshot> jobs = new ArrayList<>();
        if (Files.isDirectory(generationRoot)) {
            try (var files = Files.walk(generationRoot, 3)) {
                files.filter(path -> path.getFileName().toString().equals(FILE_NAME))
                        .forEach(path -> jobs.add(readUnchecked(path)));
            } catch (IOException ex) {
                throw new IllegalStateException("could not list generation jobs", ex);
            }
        }
        jobs.sort(Comparator.comparing(GenerationJobSnapshot::updatedAt).reversed());
        return List.copyOf(jobs);
    }

    @Override public GenerationArtifactStaging openStaging(GenerationJobRequest request, int attempt) {
        Path jobDirectory = jobDirectory(request);
        Path staging = jobDirectory.resolve(".staging-attempt-" + Math.max(1, attempt)).normalize();
        try {
            Files.createDirectories(jobDirectory);
            deleteTree(staging, jobDirectory, ".staging-attempt-");
            Files.createDirectories(staging);
            return new ProjectArtifactStaging(projectRoot, jobDirectory, staging);
        } catch (IOException ex) {
            throw new IllegalStateException("could not create artifact staging for " + request.jobId(), ex);
        }
    }

    private Path jobDirectory(GenerationJobRequest request) {
        return generationRoot.resolve(request.capabilityId().value()).resolve(request.jobId().value()).normalize();
    }

    private Optional<GenerationJobSnapshot> readLegacy(GenerationJobId id) {
        Path audio = projectRoot.resolve("jobs").resolve(id.value()).resolve(FILE_NAME);
        if (Files.isRegularFile(audio)) return Optional.of(readLegacyFile(audio, id, CapabilityId.VOICE_SYNTHESIS));
        Path videoDirectory = projectRoot.resolve("jobs/video").resolve(id.value());
        if (Files.isDirectory(videoDirectory)) {
            Path file = videoDirectory.resolve("video-render-job.json");
            if (Files.isRegularFile(file)) return Optional.of(readLegacyFile(file, id, CapabilityId.VIDEO_RENDERING));
        }
        return Optional.empty();
    }

    private GenerationJobSnapshot readLegacyFile(Path file, GenerationJobId id, CapabilityId capability) {
        try {
            Map<String, Object> data = object(SimpleJsonParser.parse(Files.readString(file, StandardCharsets.UTF_8)));
            GenerationJobStatus status = legacyStatus(string(data.get("status")));
            EngineId engine = new EngineId(string(data.get("engineId")).isBlank() ? "legacy" : string(data.get("engineId")));
            Map<String, String> source = Map.of("legacySource", relative(file));
            GenerationJobRequest request = new GenerationJobRequest(id, capability, engine, source,
                    instant(data.get("createdAt"), Files.getLastModifiedTime(file).toInstant()), 1,
                    EnginePresetId.AUTO, new EmptyGenerationPayload(source));
            return new GenerationJobSnapshot(request, status, "legacy", number(data.get("progress"), 0),
                    "Trabajo histórico de solo lectura.", List.of(), "", integer(data.get("attempt"), 0),
                    instant(data.get("updatedAt"), Files.getLastModifiedTime(file).toInstant()));
        } catch (IOException ex) {
            throw new IllegalStateException("could not read legacy generation job " + file, ex);
        }
    }

    private GenerationJobSnapshot read(Path file) throws IOException {
        Map<String, Object> data = object(SimpleJsonParser.parse(Files.readString(file, StandardCharsets.UTF_8)));
        int version = integer(data.get("version"), 2);
        CapabilityId capability = new CapabilityId(string(data.get("capabilityId")));
        EngineId engine = new EngineId(string(data.get("engineId")));
        GenerationJobId id = new GenerationJobId(string(data.get("jobId")));
        EnginePresetId preset = new EnginePresetId(defaultString(data.get("presetId"), EnginePresetId.AUTO.value()));
        Map<String, Object> requestData = object(data.get("request"));
        Map<String, String> parameters = stringMap(requestData.get("parameters"));
        GenerationPayload payload = payload(string(requestData.get("kind")), object(requestData.get("payload")));
        GenerationJobRequest request = new GenerationJobRequest(id, capability, engine, parameters,
                instant(data.get("createdAt"), Instant.EPOCH), version, preset, payload);
        ArrayList<GenerationArtifact> artifacts = new ArrayList<>();
        for (Object item : array(data.get("artifacts"))) {
            Map<String, Object> artifact = object(item);
            String path = string(artifact.get("path"));
            URI location = path.contains("://") ? URI.create(path) : projectRoot.resolve(path).normalize().toUri();
            artifacts.add(new GenerationArtifact(string(artifact.get("kind")), location,
                    stringMap(artifact.get("metadata"))));
        }
        return new GenerationJobSnapshot(request, GenerationJobStatus.valueOf(string(data.get("state"))),
                string(data.get("stage")), number(data.get("progress"), 0), string(data.get("message")), artifacts,
                string(data.get("diagnostic")), integer(data.get("attempts"), 0),
                instant(data.get("updatedAt"), Instant.EPOCH));
    }

    private GenerationJobSnapshot readUnchecked(Path file) {
        try { return read(file); }
        catch (IOException ex) { throw new IllegalStateException("could not read generation job " + file, ex); }
    }

    private String json(GenerationJobSnapshot snapshot) {
        GenerationJobRequest request = snapshot.request();
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("version", request.schemaVersion());
        root.put("jobId", request.jobId().value());
        root.put("capabilityId", request.capabilityId().value());
        root.put("engineId", request.engineId().value());
        root.put("presetId", request.presetId().value());
        root.put("createdAt", request.createdAt().toString());
        root.put("request", Map.of("kind", request.payload().kind(), "parameters", request.parameters(),
                "payload", payloadMap(request.payload())));
        root.put("state", snapshot.status().name());
        root.put("stage", snapshot.stage());
        root.put("progress", snapshot.progress());
        root.put("message", snapshot.message());
        root.put("attempts", snapshot.attempt());
        root.put("artifacts", snapshot.artifacts().stream().map(this::artifactMap).toList());
        root.put("diagnostic", snapshot.diagnostic());
        root.put("updatedAt", snapshot.updatedAt().toString());
        return jsonValue(root) + System.lineSeparator();
    }

    private Map<String, Object> payloadMap(GenerationPayload payload) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("portableFields", payload.portableFields());
        switch (payload) {
            case VoiceJobPayload voice -> { result.put("unitIds", voice.unitIds()); result.put("language", voice.language()); }
            case ImageJobPayload image -> { result.put("prompt", image.prompt()); result.put("width", image.width()); result.put("height", image.height()); }
            case VideoGenerationJobPayload video -> { result.put("prompt", video.prompt()); result.put("initialImage", video.initialImageRelativePath()); result.put("durationSeconds", video.durationSeconds()); }
            case VideoRenderJobPayload render -> result.put("timelineItemIds", render.timelineItemIds());
            case EmptyGenerationPayload ignored -> { }
        }
        return result;
    }

    private GenerationPayload payload(String kind, Map<String, Object> data) {
        Map<String, String> fields = stringMap(data.get("portableFields"));
        return switch (kind) {
            case "voice-synthesis" -> new VoiceJobPayload(stringList(data.get("unitIds")), string(data.get("language")), fields);
            case "image-generation" -> new ImageJobPayload(string(data.get("prompt")), integer(data.get("width"), 512), integer(data.get("height"), 512), fields);
            case "video-generation" -> new VideoGenerationJobPayload(string(data.get("prompt")), string(data.get("initialImage")), number(data.get("durationSeconds"), 5), fields);
            case "video-rendering" -> new VideoRenderJobPayload(stringList(data.get("timelineItemIds")), fields);
            default -> new EmptyGenerationPayload(fields);
        };
    }

    private Map<String, Object> artifactMap(GenerationArtifact artifact) {
        return Map.of("kind", artifact.kind(), "path", relativeOrUri(artifact.location()), "metadata", artifact.metadata());
    }

    private String relativeOrUri(URI location) {
        if ("file".equalsIgnoreCase(location.getScheme())) {
            Path path = Path.of(location).toAbsolutePath().normalize();
            if (path.startsWith(projectRoot)) return relative(path);
        }
        return location.toString();
    }

    private String relative(Path path) { return projectRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'); }

    private static GenerationJobStatus legacyStatus(String value) {
        return switch (value.toUpperCase(java.util.Locale.ROOT)) {
            case "COMPLETED", "SUCCEEDED", "SUCCESS" -> GenerationJobStatus.SUCCEEDED;
            case "RUNNING", "PROCESSING" -> GenerationJobStatus.RUNNING;
            case "FAILED", "ERROR" -> GenerationJobStatus.FAILED;
            case "CANCELLED", "CANCELED" -> GenerationJobStatus.CANCELLED;
            default -> GenerationJobStatus.QUEUED;
        };
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
    private static List<?> array(Object value) { return value instanceof List<?> list ? list : List.of(); }
    private static List<String> stringList(Object value) { return array(value).stream().map(FileGenerationJobRepository::string).toList(); }
    private static Map<String, String> stringMap(Object value) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        object(value).forEach((key, item) -> result.put(key, string(item)));
        return Map.copyOf(result);
    }
    private static String string(Object value) { return value == null ? "" : value.toString(); }
    private static String defaultString(Object value, String fallback) { String text = string(value); return text.isBlank() ? fallback : text; }
    private static int integer(Object value, int fallback) { return value instanceof Number n ? n.intValue() : fallback; }
    private static double number(Object value, double fallback) { return value instanceof Number n ? n.doubleValue() : fallback; }
    private static Instant instant(Object value, Instant fallback) { try { return Instant.parse(string(value)); } catch (Exception ignored) { return fallback; } }

    private static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + escape(text) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(jsonValue(entry.getKey().toString())).append(':').append(jsonValue(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                out.append(jsonValue(item));
            }
            return out.append(']').toString();
        }
        return jsonValue(value.toString());
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static void deleteTree(Path target, Path parent, String prefix) throws IOException {
        if (target == null || parent == null || !target.startsWith(parent)
                || !target.getFileName().toString().startsWith(prefix) || !Files.exists(target)) return;
        try (var paths = Files.walk(target)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static final class ProjectArtifactStaging implements GenerationArtifactStaging {
        private final Path projectRoot;
        private final Path jobDirectory;
        private final Path staging;
        private boolean promoted;

        private ProjectArtifactStaging(Path projectRoot, Path jobDirectory, Path staging) {
            this.projectRoot = projectRoot;
            this.jobDirectory = jobDirectory;
            this.staging = staging;
        }

        @Override public Path directory() { return staging; }

        @Override public List<GenerationArtifact> promote(List<GenerationArtifact> artifacts) {
            ArrayList<GenerationArtifact> promotedArtifacts = new ArrayList<>();
            Path targetRoot = jobDirectory.resolve("artifacts").normalize();
            try {
                Files.createDirectories(targetRoot);
                int index = 0;
                for (GenerationArtifact artifact : artifacts == null ? List.<GenerationArtifact>of() : artifacts) {
                    URI location = artifact.location();
                    if (!"file".equalsIgnoreCase(location.getScheme())) {
                        promotedArtifacts.add(artifact);
                        continue;
                    }
                    Path source = Path.of(location).toAbsolutePath().normalize();
                    Path relative = source.startsWith(staging) ? staging.relativize(source)
                            : Path.of(String.format("%02d-%s", ++index, source.getFileName()));
                    Path target = targetRoot.resolve(relative).normalize();
                    if (!target.startsWith(targetRoot)) throw new IOException("artifact escapes job directory: " + relative);
                    Files.createDirectories(target.getParent());
                    if (source.startsWith(staging)) move(source, target); else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    promotedArtifacts.add(new GenerationArtifact(artifact.kind(), target.toUri(), artifact.metadata()));
                }
                promoted = true;
                return List.copyOf(promotedArtifacts);
            } catch (IOException ex) {
                throw new IllegalStateException("could not promote generation artifacts", ex);
            }
        }

        @Override public void close() {
            try { deleteTree(staging, jobDirectory, ".staging-attempt-"); }
            catch (IOException ignored) { }
        }
    }
}
