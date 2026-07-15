package com.marcosmoreiradev.docupodcaststudio.infrastructure.video;

import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderCommandPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobArtifact;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobLogReference;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** File-system repository for persistent FFmpeg render jobs. */
public final class VideoRenderJobFileRepository implements VideoRenderJobRepository {
    private static final String VIDEO_JOBS_DIR = "jobs/video";

    @Override
    public void save(Path projectDirectory, VideoRenderJobSnapshot snapshot, VideoRenderCommandPlan commandPlan) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Objects.requireNonNull(snapshot, "snapshot");
        Path jobDir = projectDirectory.toAbsolutePath().normalize().resolve(snapshot.jobRelativeDirectory()).normalize();
        if (!jobDir.startsWith(projectDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Invalid video render job directory");
        }
        Files.createDirectories(jobDir.resolve("logs"));
        Files.writeString(jobDir.resolve("video-render-job.json"), toJson(snapshot), StandardCharsets.UTF_8);
        Files.writeString(jobDir.resolve("logs").resolve("render-diagnostics.jsonl"), diagnosticLine(snapshot), StandardCharsets.UTF_8);
        if (commandPlan != null) {
            Files.writeString(jobDir.resolve("render-commands.txt"), commandPlan.commandsText(), StandardCharsets.UTF_8);
            Files.writeString(jobDir.resolve("RENDER_MANIFEST.json"), commandPlan.manifestJson(), StandardCharsets.UTF_8);
        }
        Path cancellationToken = jobDir.resolve("cancel.requested");
        if (snapshot.cancellationRequested() || snapshot.state() == ProcessJobState.CANCELLED) {
            Files.writeString(cancellationToken, snapshot.message(), StandardCharsets.UTF_8);
        } else {
            Files.deleteIfExists(cancellationToken);
        }
    }

    @Override
    public java.util.Optional<VideoRenderJobSnapshot> load(Path projectDirectory, String jobId) throws IOException {
        if (projectDirectory == null || jobId == null || jobId.isBlank()) {
            return java.util.Optional.empty();
        }
        Path file = projectDirectory.toAbsolutePath().normalize().resolve(VIDEO_JOBS_DIR).resolve(jobId).resolve("video-render-job.json");
        if (!Files.isRegularFile(file)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(fromJson(Files.readString(file, StandardCharsets.UTF_8)));
    }

    @Override
    public List<VideoRenderJobSnapshot> list(Path projectDirectory) throws IOException {
        if (projectDirectory == null) {
            return List.of();
        }
        Path root = projectDirectory.toAbsolutePath().normalize().resolve(VIDEO_JOBS_DIR);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        ArrayList<VideoRenderJobSnapshot> jobs = new ArrayList<>();
        try (java.util.stream.Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                Path file = dir.resolve("video-render-job.json");
                if (Files.isRegularFile(file)) {
                    jobs.add(fromJson(Files.readString(file, StandardCharsets.UTF_8)));
                }
            }
        }
        jobs.sort(Comparator.comparing(VideoRenderJobSnapshot::updatedAt).reversed());
        return List.copyOf(jobs);
    }

    private static String diagnosticLine(VideoRenderJobSnapshot snapshot) {
        return "{\"event\":\"video-render-job\",\"jobId\":\"" + escape(snapshot.jobId())
                + "\",\"state\":\"" + snapshot.state().name()
                + "\",\"stage\":\"" + snapshot.stage().name()
                + "\",\"message\":\"" + escape(snapshot.message()) + "\"}\n";
    }

    private static String toJson(VideoRenderJobSnapshot snapshot) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        field(json, "jobId", snapshot.jobId(), true);
        field(json, "state", snapshot.state().name(), true);
        field(json, "stage", snapshot.stage().name(), true);
        number(json, "progress", snapshot.progress(), true);
        number(json, "totalFrames", snapshot.totalFrames(), true);
        number(json, "completedFrames", snapshot.completedFrames(), true);
        field(json, "currentStep", snapshot.currentStep(), true);
        field(json, "message", snapshot.message(), true);
        field(json, "packageRelativeDirectory", snapshot.packageRelativeDirectory(), true);
        field(json, "jobRelativeDirectory", snapshot.jobRelativeDirectory(), true);
        field(json, "outputRelativePath", snapshot.outputRelativePath(), true);
        bool(json, "cancellationRequested", snapshot.cancellationRequested(), true);
        bool(json, "recoverable", snapshot.recoverable(), true);
        field(json, "createdAt", snapshot.createdAt().toString(), true);
        field(json, "updatedAt", snapshot.updatedAt().toString(), true);
        json.append("  \"artifacts\": [");
        for (int i = 0; i < snapshot.artifacts().size(); i++) {
            ProcessJobArtifact artifact = snapshot.artifacts().get(i);
            if (i > 0) json.append(", ");
            json.append("{\"role\":\"").append(escape(artifact.role())).append("\",\"relativePath\":\"")
                    .append(escape(artifact.relativePath())).append("\",\"description\":\"")
                    .append(escape(artifact.description())).append("\"}");
        }
        json.append("],\n");
        ProcessJobLogReference logs = snapshot.logs();
        json.append("  \"logs\": {\"generationLogPath\":\"").append(escape(logs.generationLogPath()))
                .append("\",\"diagnosticsPath\":\"").append(escape(logs.diagnosticsPath()))
                .append("\",\"stdoutLogPath\":\"").append(escape(logs.stdoutLogPath()))
                .append("\",\"stderrLogPath\":\"").append(escape(logs.stderrLogPath()))
                .append("\"}\n");
        json.append("}\n");
        return json.toString();
    }

    private static VideoRenderJobSnapshot fromJson(String json) throws IOException {
        Object parsed = SimpleJsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new IOException("video-render-job.json must be an object");
        }
        Map<String, Object> map = cast(raw);
        ProcessJobLogReference logs = logs(map.get("logs"));
        return new VideoRenderJobSnapshot(
                string(map, "jobId"),
                enumValue(ProcessJobState.class, string(map, "state"), ProcessJobState.QUEUED),
                enumValue(ProcessJobStage.class, string(map, "stage"), ProcessJobStage.PREPARING_WORKSPACE),
                number(map, "progress"),
                (int) number(map, "totalFrames"),
                (int) number(map, "completedFrames"),
                string(map, "currentStep"),
                string(map, "message"),
                string(map, "packageRelativeDirectory"),
                string(map, "jobRelativeDirectory"),
                string(map, "outputRelativePath"),
                bool(map, "cancellationRequested"),
                bool(map, "recoverable"),
                artifacts(map.get("artifacts")),
                logs,
                instant(string(map, "createdAt")),
                instant(string(map, "updatedAt"))
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }

    private static List<ProcessJobArtifact> artifacts(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProcessJobArtifact> artifacts = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> obj = cast(map);
                artifacts.add(new ProcessJobArtifact(string(obj, "role"), string(obj, "relativePath"), string(obj, "description")));
            }
        }
        return List.copyOf(artifacts);
    }

    private static ProcessJobLogReference logs(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return new ProcessJobLogReference("", "", "", "");
        }
        Map<String, Object> obj = cast(map);
        return new ProcessJobLogReference(
                string(obj, "generationLogPath"),
                string(obj, "diagnosticsPath"),
                string(obj, "stdoutLogPath"),
                string(obj, "stderrLogPath")
        );
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static Instant instant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }

    private static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private static double number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(string(map, key));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static boolean bool(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(string(map, key));
    }

    private static void field(StringBuilder json, String name, String value, boolean comma) {
        json.append("  \"").append(name).append("\": \"").append(escape(value)).append("\"").append(comma ? "," : "").append("\n");
    }

    private static void number(StringBuilder json, String name, double value, boolean comma) {
        json.append("  \"").append(name).append("\": ").append(value).append(comma ? "," : "").append("\n");
    }

    private static void bool(StringBuilder json, String name, boolean value, boolean comma) {
        json.append("  \"").append(name).append("\": ").append(value).append(comma ? "," : "").append("\n");
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
