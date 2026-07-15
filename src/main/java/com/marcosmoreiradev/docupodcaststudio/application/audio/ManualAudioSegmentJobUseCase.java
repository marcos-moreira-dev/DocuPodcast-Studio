package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Applies or removes a user-recorded WAV as the effective audio for one segment in a persisted job. */
public final class ManualAudioSegmentJobUseCase {
    private static final DateTimeFormatter JOB_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AudioJobRepository repository;
    private final AudioDurationProbe durationProbe;

    public ManualAudioSegmentJobUseCase(AudioJobRepository repository, AudioDurationProbe durationProbe) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.durationProbe = Objects.requireNonNull(durationProbe, "durationProbe");
    }

    public AudioJobSnapshot apply(Path projectDirectory, AudioJobSnapshot targetSnapshot,
                                  String documentName, String segmentId, String segmentTitle,
                                  Path sourceWav, List<AudioSegmentSnapshot> baselineSegments) throws IOException {
        Path root = projectRoot(projectDirectory);
        Path source = Objects.requireNonNull(sourceWav, "sourceWav").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source) || Files.size(source) <= 44L) {
            throw new IOException("La grabacion no produjo un WAV valido: " + sourceWav);
        }
        String safeSegmentId = token(segmentId, "segmentId");
        AudioJobSnapshot base = targetSnapshot == null
                ? emptyManualSnapshot(documentName, baselineSegments)
                : targetSnapshot;
        Path jobDir = root.resolve(base.jobRelativeDirectory()).normalize();
        if (!jobDir.startsWith(root.resolve("jobs").normalize())) {
            throw new IOException("Job de audio fuera de la carpeta jobs/: " + base.jobRelativeDirectory());
        }
        Path target = jobDir.resolve("audio").resolve(safeSegmentId + "-manual.wav").normalize();
        if (!target.startsWith(jobDir.resolve("audio").normalize())) {
            throw new IOException("Ruta de audio manual invalida para " + safeSegmentId);
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        double duration = durationProbe.tryDurationSeconds(target).orElse(0.0);
        String relative = relative(root, target);
        List<AudioSegmentSnapshot> merged = replaceOrAppend(
                mergeBaseline(base.segments(), baselineSegments),
                safeSegmentId,
                titleOrDefault(segmentTitle, safeSegmentId),
                current -> current.completed(relative, duration));
        AudioJobSnapshot updated = withSegments(base, merged,
                "Audio manual aplicado a " + safeSegmentId + ".");
        writeManifest(root, updated);
        repository.save(root, updated);
        return updated;
    }

    public AudioJobSnapshot delete(Path projectDirectory, AudioJobSnapshot targetSnapshot,
                                   String segmentId, String segmentTitle,
                                   List<AudioSegmentSnapshot> baselineSegments) throws IOException {
        if (targetSnapshot == null) {
            throw new IOException("No hay job de audio donde eliminar la grabacion manual.");
        }
        Path root = projectRoot(projectDirectory);
        String safeSegmentId = token(segmentId, "segmentId");
        Path jobDir = root.resolve(targetSnapshot.jobRelativeDirectory()).normalize();
        Path manual = jobDir.resolve("audio").resolve(safeSegmentId + "-manual.wav").normalize();
        if (manual.startsWith(jobDir.resolve("audio").normalize())) {
            Files.deleteIfExists(manual);
        }
        Path generated = jobDir.resolve("audio").resolve(safeSegmentId + ".wav").normalize();
        List<AudioSegmentSnapshot> merged = replaceOrAppend(
                mergeBaseline(targetSnapshot.segments(), baselineSegments),
                safeSegmentId,
                titleOrDefault(segmentTitle, safeSegmentId),
                current -> {
                    if (Files.isRegularFile(generated)) {
                        double duration = durationProbe.tryDurationSeconds(generated).orElse(0.0);
                        return current.completed(relative(root, generated), duration);
                    }
                    return new AudioSegmentSnapshot(safeSegmentId, titleOrDefault(segmentTitle, safeSegmentId),
                            AudioSegmentStatus.PENDING, "", 0.0, current.attempts(), "");
                });
        AudioJobSnapshot updated = withSegments(targetSnapshot, merged,
                "Audio manual eliminado de " + safeSegmentId + ".");
        writeManifest(root, updated);
        repository.save(root, updated);
        return updated;
    }

    /** Replaces a generated subset only after every staged WAV has been validated. */
    public AudioJobSnapshot applyGeneratedBatch(Path projectDirectory, AudioJobSnapshot targetSnapshot,
                                                AudioJobSnapshot stagedSnapshot, List<String> expectedUnitIds) throws IOException {
        if (targetSnapshot == null) throw new IOException("No hay un job reproducible donde actualizar la intervencion.");
        if (stagedSnapshot == null) throw new IOException("El job aislado de regeneracion no produjo estado persistido.");
        Path root = projectRoot(projectDirectory);
        List<String> ids = expectedUnitIds == null ? List.of() : expectedUnitIds.stream().map(String::strip).filter(id -> !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) throw new IOException("La intervencion no contiene unidades TTS regenerables.");
        Map<String, AudioSegmentSnapshot> staged = stagedSnapshot.segments().stream().collect(java.util.stream.Collectors.toMap(
                AudioSegmentSnapshot::segmentId, segment -> segment, (left, right) -> right, LinkedHashMap::new));
        Path targetJob = checkedJobDirectory(root, targetSnapshot.jobRelativeDirectory());
        Path work = targetJob.resolve("audio").resolve(".regenerate-" + UUID.randomUUID()).normalize();
        Map<String, Path> prepared = new LinkedHashMap<>();
        Map<String, Path> backups = new LinkedHashMap<>();
        List<String> mutated = new ArrayList<>();
        try {
            Files.createDirectories(work);
            for (String id : ids) {
                AudioSegmentSnapshot segment = staged.get(id);
                if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank())
                    throw new IOException("La regeneracion no completo la unidad " + id + ".");
                Path source = root.resolve(segment.audioRelativePath()).normalize();
                if (!source.startsWith(root) || !Files.isRegularFile(source) || Files.size(source) <= 44L)
                    throw new IOException("La regeneracion no produjo un WAV valido para " + id + ".");
                Path pending = work.resolve(id + ".wav");
                Files.copy(source, pending, StandardCopyOption.REPLACE_EXISTING);
                prepared.put(id, pending);
            }
            for (String id : ids) {
                Path destination = targetJob.resolve("audio").resolve(id + ".wav").normalize();
                if (!destination.startsWith(targetJob.resolve("audio").normalize())) throw new IOException("Ruta de unidad invalida: " + id);
                if (Files.exists(destination)) {
                    Path backup = work.resolve(id + ".previous.wav");
                    move(destination, backup);
                    backups.put(id, backup);
                    mutated.add(id);
                }
                move(prepared.get(id), destination);
                if (!mutated.contains(id)) mutated.add(id);
            }
            List<AudioSegmentSnapshot> merged = mergeGenerated(targetSnapshot.segments(), staged, ids, root, targetJob);
            AudioJobSnapshot updated = withSegments(targetSnapshot, merged,
                    "Audio TTS actualizado para " + ids.size() + " unidad(es) de una intervencion.");
            writeManifest(root, updated);
            repository.save(root, updated);
            deleteTree(work);
            return updated;
        } catch (IOException | RuntimeException ex) {
            rollbackGenerated(targetJob, mutated, backups);
            deleteTree(work);
            if (ex instanceof IOException io) throw io;
            throw new IOException("No se pudo aplicar atomicamente el audio regenerado: " + ex.getMessage(), ex);
        }
    }

    private AudioJobSnapshot emptyManualSnapshot(String documentName, List<AudioSegmentSnapshot> baselineSegments) {
        String jobId = "MANUAL-JOB-" + LocalDateTime.now().format(JOB_TIME);
        List<AudioSegmentSnapshot> segments = baselineSegments == null || baselineSegments.isEmpty()
                ? List.of()
                : List.copyOf(baselineSegments);
        Instant now = Instant.now();
        return new AudioJobSnapshot(jobId, documentName, AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                0, segments.size(), 0, 0.0, "", "", 0L,
                "Job creado para audio manual.", "jobs/" + jobId, "", "", segments, now, now);
    }

    private static List<AudioSegmentSnapshot> mergeBaseline(List<AudioSegmentSnapshot> current,
                                                            List<AudioSegmentSnapshot> baseline) {
        LinkedHashMap<String, AudioSegmentSnapshot> merged = new LinkedHashMap<>();
        if (baseline != null) {
            for (AudioSegmentSnapshot segment : baseline) {
                merged.put(segment.segmentId(), segment);
            }
        }
        if (current != null) {
            for (AudioSegmentSnapshot segment : current) {
                merged.put(segment.segmentId(), segment);
            }
        }
        return List.copyOf(merged.values());
    }

    private static List<AudioSegmentSnapshot> replaceOrAppend(List<AudioSegmentSnapshot> segments, String segmentId,
                                                              String title,
                                                              java.util.function.Function<AudioSegmentSnapshot, AudioSegmentSnapshot> updater) {
        ArrayList<AudioSegmentSnapshot> updated = new ArrayList<>(segments == null ? List.of() : segments);
        for (int i = 0; i < updated.size(); i++) {
            AudioSegmentSnapshot current = updated.get(i);
            if (current.segmentId().equals(segmentId)) {
                updated.set(i, updater.apply(current));
                return List.copyOf(updated);
            }
        }
        updated.add(updater.apply(AudioSegmentSnapshot.pending(segmentId, title)));
        return List.copyOf(updated);
    }

    private List<AudioSegmentSnapshot> mergeGenerated(List<AudioSegmentSnapshot> target,
                                                      Map<String, AudioSegmentSnapshot> staged,
                                                      List<String> ids, Path root, Path targetJob) {
        Set<String> expected = Set.copyOf(ids);
        LinkedHashMap<String, AudioSegmentSnapshot> merged = new LinkedHashMap<>();
        if (target != null) target.forEach(segment -> merged.put(segment.segmentId(), segment));
        for (String id : ids) {
            AudioSegmentSnapshot source = staged.get(id);
            Path wav = targetJob.resolve("audio").resolve(id + ".wav");
            AudioSegmentSnapshot current = merged.getOrDefault(id, AudioSegmentSnapshot.pending(id, source.title()));
            merged.put(id, current.completed(relative(root, wav), durationProbe.tryDurationSeconds(wav).orElse(source.durationSeconds())));
        }
        if (!merged.keySet().containsAll(expected)) throw new IllegalStateException("No se pudieron integrar todas las unidades regeneradas.");
        return List.copyOf(merged.values());
    }

    private static Path checkedJobDirectory(Path root, String relative) throws IOException {
        Path jobs = root.resolve("jobs").normalize();
        Path job = root.resolve(relative).normalize();
        if (!job.startsWith(jobs)) throw new IOException("Job de audio fuera de jobs/: " + relative);
        return job;
    }

    private static void rollbackGenerated(Path targetJob, List<String> ids, Map<String, Path> backups) {
        for (String id : ids) {
            Path destination = targetJob.resolve("audio").resolve(id + ".wav");
            try {
                Files.deleteIfExists(destination);
                Path backup = backups.get(id);
                if (backup != null && Files.exists(backup)) move(backup, destination);
            } catch (IOException ignored) {
                // The original exception remains the actionable failure.
            }
        }
    }

    private static void move(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        try { Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    private static AudioJobSnapshot withSegments(AudioJobSnapshot base, List<AudioSegmentSnapshot> segments, String message) {
        int completed = (int) segments.stream().filter(AudioSegmentSnapshot::completed).count();
        int failed = (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.FAILED).count();
        int total = segments.size();
        return new AudioJobSnapshot(base.jobId(), base.documentName(), AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, completed, total, failed,
                total == 0 ? 0.0 : completed / (double) total, "", "", 0L, message,
                base.jobRelativeDirectory(), base.finalAudioPath(), base.manifestPath(),
                segments, base.createdAt(), Instant.now());
    }

    private static void writeManifest(Path projectRoot, AudioJobSnapshot snapshot) throws IOException {
        Path jobDir = projectRoot.resolve(snapshot.jobRelativeDirectory()).normalize();
        Path manifest = jobDir.resolve("audio-manifest.json");
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"jobId\": \"").append(escape(snapshot.jobId())).append("\",\n");
        json.append("  \"finalAudio\": \"\",\n  \"clips\": [");
        List<AudioSegmentSnapshot> completed = snapshot.segments().stream()
                .filter(AudioSegmentSnapshot::completed)
                .filter(segment -> !segment.audioRelativePath().isBlank())
                .toList();
        for (int i = 0; i < completed.size(); i++) {
            AudioSegmentSnapshot segment = completed.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("\n    {\"segmentId\":\"").append(escape(segment.segmentId()))
                    .append("\",\"audio\":\"").append(escape(audioPathInsideJob(segment)))
                    .append("\",\"durationSeconds\":")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", segment.durationSeconds()))
                    .append('}');
        }
        json.append("\n  ]\n}\n");
        Files.createDirectories(jobDir);
        Files.writeString(manifest, json.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String audioPathInsideJob(AudioSegmentSnapshot snapshot) {
        String path = snapshot.audioRelativePath();
        int marker = path.indexOf("/audio/");
        if (marker >= 0) {
            return path.substring(marker + 1);
        }
        return path.startsWith("audio/") ? path : path;
    }

    private static Path projectRoot(Path projectDirectory) {
        return Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String titleOrDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
