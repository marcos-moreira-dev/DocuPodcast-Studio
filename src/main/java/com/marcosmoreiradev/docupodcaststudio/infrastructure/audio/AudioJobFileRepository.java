package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSourceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionRevisionRef;
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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * File-system repository for audio job snapshots.
 *
 * <p>Each job lives under jobs/JOB-* and contains job.json plus segments-status.json.
 * This deliberately mirrors the future real TTS gateway so the UI and recovery workflow can be built early.</p>
 */
public final class AudioJobFileRepository implements AudioJobRepository {
    private final WavAudioDurationProbe durationProbe = new WavAudioDurationProbe();

    @Override
    public void save(Path projectDirectory, AudioJobSnapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Path jobDir = jobDirectory(projectDirectory, snapshot.jobId());
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("job.json"), jobJson(snapshot), StandardCharsets.UTF_8);
        Files.writeString(jobDir.resolve("segments-status.json"), segmentsJson(snapshot.segments()), StandardCharsets.UTF_8);
    }

    @Override
    public Optional<AudioJobSnapshot> load(Path projectDirectory, String jobId) throws IOException {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        Path jobJson = jobDirectory(projectDirectory, jobId.strip()).resolve("job.json");
        if (!Files.exists(jobJson)) {
            return Optional.empty();
        }
        return Optional.of(readSnapshot(jobJson));
    }

    @Override
    public List<AudioJobSnapshot> list(Path projectDirectory) throws IOException {
        Path jobsDir = projectRoot(projectDirectory).resolve("jobs");
        if (!Files.isDirectory(jobsDir)) {
            return List.of();
        }
        ArrayList<AudioJobSnapshot> snapshots = new ArrayList<>();
        try (Stream<Path> stream = Files.list(jobsDir)) {
            for (Path jobDir : stream.filter(Files::isDirectory).toList()) {
                Path jobJson = jobDir.resolve("job.json");
                if (Files.exists(jobJson)) {
                    snapshots.add(readSnapshot(jobJson));
                }
            }
        }
        snapshots.sort(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed());
        return List.copyOf(snapshots);
    }

    @Override
    public void deleteAll(Path projectDirectory) throws IOException {
        java.util.concurrent.locks.Lock maintenance = AudioJobWorkspaceLockRegistry.maintenance(projectDirectory);
        maintenance.lock();
        try {
        Path jobsDir = projectRoot(projectDirectory).resolve("jobs").normalize();
        if (!Files.exists(jobsDir)) {
            return;
        }
        if (!Files.isDirectory(jobsDir)) {
            throw new IOException("La ruta jobs no es una carpeta: " + jobsDir);
        }
        try (Stream<Path> stream = Files.walk(jobsDir)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        } finally {
            maintenance.unlock();
        }
    }

    private AudioJobSnapshot readSnapshot(Path jobJsonPath) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) SimpleJsonParser.parse(Files.readString(jobJsonPath, StandardCharsets.UTF_8));
        Path jobDir = jobJsonPath.getParent();
        Path projectDirectory = jobDir == null || jobDir.getParent() == null ? Path.of("") : jobDir.getParent().getParent();
        List<AudioSegmentSnapshot> segments = readSegments(jobDir.resolve("segments-status.json"), projectDirectory, jobDir);
        return new AudioJobSnapshot(
                string(job.get("jobId"), "jobId"),
                stringOrDefault(job.get("documentName"), "Proyecto DocuPodcast"),
                enumValue(AudioJobState.class, stringOrDefault(job.get("state"), AudioJobState.IDLE.name()), "state"),
                enumValue(AudioGenerationStage.class, stringOrDefault(job.get("stage"), AudioGenerationStage.NONE.name()), "stage"),
                intOrDefault(job.get("completedSegments"), 0),
                intOrDefault(job.get("totalSegments"), segments.size()),
                intOrDefault(job.get("failedSegments"), 0),
                doubleOrDefault(job.get("progress"), 0.0),
                stringOrDefault(job.get("currentSegmentId"), ""),
                stringOrDefault(job.get("currentSegmentTitle"), ""),
                longOrDefault(job.get("estimatedRemainingSeconds"), 0L),
                stringOrDefault(job.get("message"), ""),
                string(job.get("jobRelativeDirectory"), "jobRelativeDirectory"),
                stringOrDefault(job.get("finalAudioPath"), ""),
                stringOrDefault(job.get("manifestPath"), ""),
                segments,
                instantOrNow(job.get("createdAt")),
                instantOrNow(job.get("updatedAt"))
        );
    }

    private List<AudioSegmentSnapshot> readSegments(Path segmentsPath, Path projectDirectory, Path jobDir) throws IOException {
        if (!Files.exists(segmentsPath)) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) SimpleJsonParser.parse(Files.readString(segmentsPath, StandardCharsets.UTF_8));
        Object raw = root.getOrDefault("segments", List.of());
        if (!(raw instanceof List<?> list)) {
            throw new IOException("segments-status.json must contain array field 'segments'");
        }
        ArrayList<AudioSegmentSnapshot> segments = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new IOException("segments entries must be objects");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> segment = (Map<String, Object>) rawMap;
            AudioSegmentSnapshot snapshot = new AudioSegmentSnapshot(
                    string(segment.get("segmentId"), "segmentId"),
                    stringOrDefault(segment.get("title"), ""),
                    enumValue(AudioSegmentStatus.class, stringOrDefault(segment.get("status"), AudioSegmentStatus.PENDING.name()), "status"),
                    stringOrDefault(segment.get("audioRelativePath"), ""),
                    doubleOrDefault(segment.get("durationSeconds"), 0.0),
                    intOrDefault(segment.get("attempts"), 0),
                    stringOrDefault(segment.get("errorMessage"), ""),
                    fingerprint(segment)
            );
            segments.add(withMeasuredDuration(snapshot, projectDirectory, jobDir));
        }
        return List.copyOf(segments);
    }


    private AudioSegmentSnapshot withMeasuredDuration(AudioSegmentSnapshot snapshot, Path projectDirectory, Path jobDir) {
        if (!snapshot.completed() || snapshot.audioRelativePath().isBlank()) {
            return snapshot;
        }
        Path audio = resolveAudioPath(snapshot, projectDirectory, jobDir);
        return durationProbe.tryDurationSeconds(audio)
                .stream()
                .mapToObj(duration -> snapshot.completed(snapshot.audioRelativePath(), duration))
                .findFirst()
                .orElse(snapshot);
    }

    private static Path resolveAudioPath(AudioSegmentSnapshot snapshot, Path projectDirectory, Path jobDir) {
        String relative = snapshot.audioRelativePath();
        Path projectCandidate = projectDirectory == null ? null : projectDirectory.resolve(relative).normalize();
        if (projectCandidate != null && Files.exists(projectCandidate)) {
            return projectCandidate;
        }
        Path jobCandidate = jobDir.resolve(relative).normalize();
        if (Files.exists(jobCandidate)) {
            return jobCandidate;
        }
        return jobDir.resolve("audio").resolve(snapshot.segmentId() + ".wav").normalize();
    }

    private static Path jobDirectory(Path projectDirectory, String jobId) {
        return projectRoot(projectDirectory).resolve("jobs").resolve(jobId).normalize();
    }

    private static Path projectRoot(Path projectDirectory) {
        return Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
    }

    private static String jobJson(AudioJobSnapshot snapshot) {
        return "{\n"
                + field("jobId", snapshot.jobId(), true)
                + field("documentName", snapshot.documentName(), true)
                + field("state", snapshot.state().name(), true)
                + field("stage", snapshot.stage().name(), true)
                + numberField("completedSegments", snapshot.completedSegments(), true)
                + numberField("totalSegments", snapshot.totalSegments(), true)
                + numberField("failedSegments", snapshot.failedSegments(), true)
                + decimalField("progress", snapshot.progress(), true)
                + field("currentSegmentId", snapshot.currentSegmentId(), true)
                + field("currentSegmentTitle", snapshot.currentSegmentTitle(), true)
                + numberField("estimatedRemainingSeconds", snapshot.estimatedRemainingSeconds(), true)
                + field("message", snapshot.message(), true)
                + field("jobRelativeDirectory", snapshot.jobRelativeDirectory(), true)
                + field("finalAudioPath", snapshot.finalAudioPath(), true)
                + field("manifestPath", snapshot.manifestPath(), true)
                + field("createdAt", snapshot.createdAt().toString(), true)
                + field("updatedAt", snapshot.updatedAt().toString(), false)
                + "}\n";
    }

    private static String segmentsJson(List<AudioSegmentSnapshot> segments) {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n  \"segments\": [");
        if (!segments.isEmpty()) {
            out.append('\n');
        }
        for (int i = 0; i < segments.size(); i++) {
            AudioSegmentSnapshot segment = segments.get(i);
            out.append("    {\n")
                    .append(field(3, "segmentId", segment.segmentId(), true))
                    .append(field(3, "title", segment.title(), true))
                    .append(field(3, "status", segment.status().name(), true))
                    .append(field(3, "audioRelativePath", segment.audioRelativePath(), true))
                    .append(decimalField(3, "durationSeconds", segment.durationSeconds(), true))
                    .append(numberField(3, "attempts", segment.attempts(), true))
                    .append(field(3, "errorMessage", segment.errorMessage(), true))
                    .append(field(3, "sourceRegionRefs", regionRefs(segment.sourceFingerprint()), true))
                    .append(numberField(3, "sourceFirstPage", segment.sourceFingerprint().firstPage(), true))
                    .append(numberField(3, "sourceLastPage", segment.sourceFingerprint().lastPage(), true))
                    .append(field(3, "sourceTextSha256", segment.sourceFingerprint().textSha256(), true))
                    .append(field(3, "sourceVoiceSha256", segment.sourceFingerprint().voiceConfigurationSha256(), true))
                    .append(field(3, "sourcePreprocessingSha256", segment.sourceFingerprint().preprocessingSha256(), true))
                    .append(field(3, "sourceDerivedTreatmentSha256",
                            segment.sourceFingerprint().derivedTreatmentSha256(), false))
                    .append("    }");
            if (i < segments.size() - 1) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ]\n}\n");
        return out.toString();
    }

    private static AudioSourceFingerprint fingerprint(Map<String, Object> segment) throws IOException {
        String text = stringOrDefault(segment.get("sourceTextSha256"), "");
        String voice = stringOrDefault(segment.get("sourceVoiceSha256"), "");
        String preprocessing = stringOrDefault(segment.get("sourcePreprocessingSha256"), "");
        String derived = stringOrDefault(segment.get("sourceDerivedTreatmentSha256"), "");
        if (text.isBlank() || voice.isBlank() || preprocessing.isBlank()) {
            return AudioSourceFingerprint.untraceable();
        }
        return new AudioSourceFingerprint(
                parseRegionRefs(stringOrDefault(segment.get("sourceRegionRefs"), "")),
                intOrDefault(segment.get("sourceFirstPage"), 0),
                intOrDefault(segment.get("sourceLastPage"), 0),
                text, voice, preprocessing, derived);
    }

    private static String regionRefs(AudioSourceFingerprint fingerprint) {
        return fingerprint.sourceRegions().stream()
                .map(ref -> ref.regionId() + "|" + ref.pageNumber() + "|" + ref.revision())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private static List<PdfRegionRevisionRef> parseRegionRefs(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        ArrayList<PdfRegionRevisionRef> refs = new ArrayList<>();
        for (String item : raw.split(";")) {
            String[] parts = item.split("\\|", -1);
            if (parts.length != 3) continue;
            try {
                refs.add(new PdfRegionRevisionRef(parts[0], Integer.parseInt(parts[1]), Long.parseLong(parts[2])));
            } catch (IllegalArgumentException ignored) {
                // A malformed old provenance entry makes only that entry unusable.
            }
        }
        return List.copyOf(refs);
    }

    private static String field(String name, String value, boolean comma) {
        return "  \"" + escape(name) + "\": " + quote(value) + (comma ? "," : "") + "\n";
    }

    private static String field(int level, String name, String value, boolean comma) {
        return "  ".repeat(level) + "\"" + escape(name) + "\": " + quote(value) + (comma ? "," : "") + "\n";
    }

    private static String numberField(String name, long value, boolean comma) {
        return "  \"" + escape(name) + "\": " + value + (comma ? "," : "") + "\n";
    }

    private static String numberField(int level, String name, long value, boolean comma) {
        return "  ".repeat(level) + "\"" + escape(name) + "\": " + value + (comma ? "," : "") + "\n";
    }

    private static String decimalField(String name, double value, boolean comma) {
        return "  \"" + escape(name) + "\": " + String.format(java.util.Locale.ROOT, "%.6f", value) + (comma ? "," : "") + "\n";
    }

    private static String decimalField(int level, String name, double value, boolean comma) {
        return "  ".repeat(level) + "\"" + escape(name) + "\": " + String.format(java.util.Locale.ROOT, "%.6f", value) + (comma ? "," : "") + "\n";
    }

    private static String quote(String value) {
        return "\"" + escape(value == null ? "" : value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String string(Object value, String field) throws IOException {
        if (!(value instanceof String text)) {
            throw new IOException(field + " must be a string");
        }
        return text;
    }

    private static String stringOrDefault(Object value, String fallback) throws IOException {
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof String text)) {
            throw new IOException("Expected string value");
        }
        return text;
    }

    private static int intOrDefault(Object value, int fallback) throws IOException {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static long longOrDefault(Object value, long fallback) throws IOException {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static double doubleOrDefault(Object value, double fallback) throws IOException {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static Instant instantOrNow(Object value) throws IOException {
        if (value == null) {
            return Instant.now();
        }
        if (!(value instanceof String text)) {
            throw new IOException("Expected instant string");
        }
        try {
            return Instant.parse(text);
        } catch (RuntimeException ex) {
            throw new IOException("Invalid instant value: " + text, ex);
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String field) throws IOException {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IOException(field + " has unsupported value: " + value, ex);
        }
    }
}
