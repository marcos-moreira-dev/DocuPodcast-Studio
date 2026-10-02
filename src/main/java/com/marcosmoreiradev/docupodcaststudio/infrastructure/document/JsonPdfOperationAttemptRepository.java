package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOperationAttemptRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One atomic JSON file per attempt under document/pdf-operations. */
public final class JsonPdfOperationAttemptRepository
        implements PdfOperationAttemptRepository {
    public static final String RELATIVE_DIRECTORY = "document/pdf-operations";
    private final AtomicUtf8JsonFileWriter writer =
            new AtomicUtf8JsonFileWriter();

    @Override
    public List<PdfOperationAttempt> list(Path projectRoot) throws IOException {
        Path directory = directory(projectRoot);
        if (!Files.isDirectory(directory)) return List.of();
        ArrayList<PdfOperationAttempt> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(value -> value.getFileName().toString()
                    .endsWith(".json")).sorted().toList()) {
                result.add(read(path));
            }
        }
        return result.stream().sorted(java.util.Comparator
                .comparing(PdfOperationAttempt::startedAt)).toList();
    }

    @Override
    public synchronized void save(Path projectRoot, PdfOperationAttempt attempt)
            throws IOException {
        Path target = directory(projectRoot).resolve(attempt.id() + ".json");
        writer.write(target, json(attempt));
    }

    private static PdfOperationAttempt read(Path path) throws IOException {
        Object parsed = SimpleJsonParser.parse(
                Files.readString(path, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("attempt JSON");
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
        @SuppressWarnings("unchecked") Map<String, Object> metrics =
                (Map<String, Object>) map.get("metrics");
        return new PdfOperationAttempt(integer(map, "schemaVersion"),
                string(map, "id"), string(map, "operationKey"),
                integer(map, "pageNumber"), string(map, "operation"),
                strings(map.get("sourceRegionIds")),
                string(map, "evidenceFingerprint"),
                string(map, "parameterFingerprint"),
                PdfOperationAttemptState.valueOf(string(map, "state")),
                Instant.parse(string(map, "startedAt")),
                Instant.parse(string(map, "finishedAt")),
                new PdfOperationMetrics(number(metrics, "durationMillis"),
                        number(metrics, "ttftMillis"),
                        number(metrics, "promptTokens"),
                        number(metrics, "outputTokens"),
                        number(metrics, "contextCharacters"),
                        number(metrics, "ramBytes"), number(metrics, "vramBytes"),
                        number(metrics, "cpuMillis"),
                        integer(metrics, "widthPixels"),
                        integer(metrics, "heightPixels"),
                        string(metrics, "engineId"), string(metrics, "modelId"),
                        string(metrics, "doneReason")),
                string(map, "treatmentId"), string(map, "diagnostic"));
    }

    private static String json(PdfOperationAttempt value) {
        PdfOperationMetrics m = value.metrics();
        return "{\n"
                + field("schemaVersion", value.schemaVersion())
                + field("id", value.id()) + field("operationKey", value.operationKey())
                + field("pageNumber", value.pageNumber())
                + field("operation", value.operation())
                + "  \"sourceRegionIds\": " + array(value.sourceRegionIds()) + ",\n"
                + field("evidenceFingerprint", value.evidenceFingerprint())
                + field("parameterFingerprint", value.parameterFingerprint())
                + field("state", value.state().name())
                + field("startedAt", value.startedAt().toString())
                + field("finishedAt", value.finishedAt().toString())
                + "  \"metrics\": {\n"
                + metric("durationMillis", m.durationMillis())
                + metric("ttftMillis", m.ttftMillis())
                + metric("promptTokens", m.promptTokens())
                + metric("outputTokens", m.outputTokens())
                + metric("contextCharacters", m.contextCharacters())
                + metric("ramBytes", m.ramBytes()) + metric("vramBytes", m.vramBytes())
                + metric("cpuMillis", m.cpuMillis())
                + metric("widthPixels", m.widthPixels())
                + metric("heightPixels", m.heightPixels())
                + field4("engineId", m.engineId()) + field4("modelId", m.modelId())
                + "    \"doneReason\": " + quote(m.doneReason()) + "\n  },\n"
                + field("treatmentId", value.treatmentId())
                + "  \"diagnostic\": " + quote(value.diagnostic()) + "\n}\n";
    }

    private static Path directory(Path root) {
        return root.toAbsolutePath().normalize().resolve(RELATIVE_DIRECTORY);
    }
    private static String field(String key, String value) {
        return "  \"" + key + "\": " + quote(value) + ",\n";
    }
    private static String field(String key, long value) {
        return "  \"" + key + "\": " + value + ",\n";
    }
    private static String field4(String key, String value) {
        return "    \"" + key + "\": " + quote(value) + ",\n";
    }
    private static String metric(String key, long value) {
        return "    \"" + key + "\": " + value + ",\n";
    }
    private static String array(List<String> values) {
        return values.stream().map(JsonPdfOperationAttemptRepository::quote)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
    private static String quote(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
    private static String string(Map<String, Object> map, String key) {
        return java.util.Objects.toString(map.get(key), "");
    }
    private static int integer(Map<String, Object> map, String key) {
        return (int) number(map, key);
    }
    private static long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.longValue()
                : Long.parseLong(java.util.Objects.toString(value, "0"));
    }
    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(item -> java.util.Objects.toString(item, ""))
                .filter(item -> !item.isBlank()).toList();
    }
}
