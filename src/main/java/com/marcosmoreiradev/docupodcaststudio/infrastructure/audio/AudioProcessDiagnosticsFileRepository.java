package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioProcessDiagnosticsRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JSONL repository for process-level TTS diagnostics under a job logs directory. */
public final class AudioProcessDiagnosticsFileRepository implements AudioProcessDiagnosticsRepository {
    public static final String FILE_NAME = "process-diagnostics.jsonl";

    @Override
    public void append(Path projectDirectory, String jobId, AudioProcessDiagnosticEvent event) throws IOException {
        Objects.requireNonNull(event, "event");
        Path file = diagnosticFile(projectDirectory, jobId);
        Files.createDirectories(file.getParent());
        Files.writeString(file, toJson(event) + "\n", StandardCharsets.UTF_8,
                Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
    }

    @Override
    public List<AudioProcessDiagnosticEvent> list(Path projectDirectory, String jobId) throws IOException {
        Path file = diagnosticFile(projectDirectory, jobId);
        if (!Files.exists(file)) {
            return List.of();
        }
        ArrayList<AudioProcessDiagnosticEvent> events = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank()) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) SimpleJsonParser.parse(line);
            events.add(fromMap(map));
        }
        return List.copyOf(events);
    }

    private static Path diagnosticFile(Path projectDirectory, String jobId) {
        String normalizedJobId = jobId == null ? "" : jobId.strip();
        if (normalizedJobId.isBlank() || normalizedJobId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("jobId inválido para diagnósticos de proceso");
        }
        return Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize()
                .resolve("jobs").resolve(normalizedJobId).resolve("logs").resolve(FILE_NAME);
    }

    private static AudioProcessDiagnosticEvent fromMap(Map<String, Object> map) {
        return new AudioProcessDiagnosticEvent(
                Instant.parse(string(map.get("timestamp"))),
                string(map.get("jobId")),
                string(map.get("segmentId")),
                intValue(map.get("attempt")),
                string(map.get("engineName")),
                string(map.get("commandSummary")),
                intValue(map.get("exitCode")),
                longValue(map.get("durationMillis")),
                booleanValue(map.get("timedOut")),
                string(map.get("outputRelativePath")),
                longValue(map.get("outputBytes")),
                string(map.get("message")),
                string(map.get("outputTail"))
        );
    }

    private static String toJson(AudioProcessDiagnosticEvent event) {
        return "{"
                + field("timestamp", event.timestamp().toString()) + ","
                + field("jobId", event.jobId()) + ","
                + field("segmentId", event.segmentId()) + ","
                + number("attempt", event.attempt()) + ","
                + field("engineName", event.engineName()) + ","
                + field("commandSummary", event.commandSummary()) + ","
                + number("exitCode", event.exitCode()) + ","
                + number("durationMillis", event.durationMillis()) + ","
                + bool("timedOut", event.timedOut()) + ","
                + field("outputRelativePath", event.outputRelativePath()) + ","
                + number("outputBytes", event.outputBytes()) + ","
                + field("message", event.message()) + ","
                + field("outputTail", event.outputTail())
                + "}";
    }

    private static String field(String name, String value) {
        return quote(name) + ":" + quote(value == null ? "" : value);
    }

    private static String number(String name, long value) {
        return quote(name) + ":" + value;
    }

    private static String bool(String name, boolean value) {
        return quote(name) + ":" + value;
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static String string(Object value) {
        return value instanceof String text ? text : "";
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }
}
