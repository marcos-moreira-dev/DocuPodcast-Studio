package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Mock audio generator used before wiring a real TTS engine.
 *
 * <p>It writes valid silent WAV files per segment, reports progress/ETA and writes
 * job.json + segments-status.json. It can also resume a persisted job by keeping
 * already completed WAVs and generating only pending/failed/cancelled segments.</p>
 */
public final class MockAudioGenerationGateway implements AudioGenerationGateway {
    private static final DateTimeFormatter JOB_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final InMemoryAudioJobQueue queue;
    private final AudioJobRepository jobRepository;
    private final ExecutorService executor;
    private final MockWavWriter wavWriter = new MockWavWriter();
    private final AtomicInteger sequence = new AtomicInteger(1);
    private final ConcurrentHashMap<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();
    private final long segmentDelayMillis;

    public MockAudioGenerationGateway() {
        this(new InMemoryAudioJobQueue(), new AudioJobFileRepository(), 120L);
    }

    public MockAudioGenerationGateway(InMemoryAudioJobQueue queue, long segmentDelayMillis) {
        this(queue, new AudioJobFileRepository(), segmentDelayMillis);
    }

    public MockAudioGenerationGateway(InMemoryAudioJobQueue queue, AudioJobRepository jobRepository, long segmentDelayMillis) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository");
        this.segmentDelayMillis = Math.max(0L, segmentDelayMillis);
        this.executor = Executors.newSingleThreadExecutor(new AudioThreadFactory());
    }

    @Override
    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        Objects.requireNonNull(request, "request");
        Consumer<AudioJobStatusDto> consumer = statusConsumer == null ? status -> { } : statusConsumer;
        String jobId = nextJobId();
        AtomicBoolean cancellation = new AtomicBoolean(false);
        cancellations.put(jobId, cancellation);

        Path jobDir = request.projectDirectory().resolve("jobs").resolve(jobId).normalize();
        List<AudioSegmentSnapshot> segments = pendingSegments(request);
        Instant createdAt = Instant.now();
        AudioJobStatusDto queued = status(jobId, request, AudioJobState.QUEUED, AudioGenerationStage.PREPARING_WORKSPACE,
                0, segments.size(), 0, "", "", 0L, "Trabajo de audio en cola.", jobDir, "", "");
        publish(queued, consumer, request.projectDirectory(), segments, createdAt);

        executor.submit(() -> runJob(jobId, request, jobDir, cancellation, consumer, segments, createdAt, false));
        return jobId;
    }

    @Override
    public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(snapshot, "snapshot");
        Consumer<AudioJobStatusDto> consumer = statusConsumer == null ? status -> { } : statusConsumer;
        String jobId = snapshot.jobId();
        AtomicBoolean cancellation = new AtomicBoolean(false);
        cancellations.put(jobId, cancellation);

        Path jobDir = request.projectDirectory().resolve(snapshot.jobRelativeDirectory()).normalize();
        List<AudioSegmentSnapshot> segments = mergedResumeSegments(request, snapshot);
        Instant createdAt = snapshot.createdAt();
        long completed = segments.stream().filter(AudioSegmentSnapshot::completed).count();
        AudioJobStatusDto queued = status(jobId, request, AudioJobState.QUEUED, AudioGenerationStage.PREPARING_WORKSPACE,
                (int) completed, segments.size(), 0, "", "", 0L,
                "Reanudación en cola. Se conservarán los WAV completados.", jobDir,
                snapshot.finalAudioPath(), snapshot.manifestPath());
        publish(queued, consumer, request.projectDirectory(), segments, createdAt);

        executor.submit(() -> runJob(jobId, request, jobDir, cancellation, consumer, segments, createdAt, true));
        return jobId;
    }

    @Override
    public boolean cancel(String jobId) {
        String normalized = jobId == null ? "" : jobId.strip();
        AtomicBoolean token = cancellations.get(normalized);
        if (token == null) {
            return false;
        }
        token.set(true);
        queue.byId(normalized).ifPresent(status -> queue.update(new AudioJobStatusDto(
                status.jobId(), status.documentName(), AudioJobState.CANCELLATION_REQUESTED, status.stage(),
                status.completedSegments(), status.totalSegments(), status.failedSegments(), status.progress(),
                status.currentSegmentId(), status.currentSegmentTitle(), status.estimatedRemainingSeconds(),
                status.estimatedRemainingErrorSeconds(),
                "Cancelación solicitada. El job se detendrá al terminar el paso seguro actual.",
                status.outputDirectory(), status.finalAudioPath(), status.manifestPath()
        )));
        return true;
    }

    @Override
    public List<AudioJobStatusDto> listStatuses() {
        return queue.list();
    }

    private void runJob(String jobId, AudioGenerationRequest request, Path jobDir, AtomicBoolean cancellation,
                        Consumer<AudioJobStatusDto> consumer, List<AudioSegmentSnapshot> initialSegmentStates,
                        Instant createdAt, boolean resumeMode) {
        java.util.concurrent.locks.Lock workspace = AudioJobWorkspaceLockRegistry.writer(request.projectDirectory());
        workspace.lock();
        try { runJobWithWorkspaceLock(jobId, request, jobDir, cancellation, consumer, initialSegmentStates, createdAt, resumeMode); }
        finally { workspace.unlock(); }
    }

    private void runJobWithWorkspaceLock(String jobId, AudioGenerationRequest request, Path jobDir, AtomicBoolean cancellation,
                        Consumer<AudioJobStatusDto> consumer, List<AudioSegmentSnapshot> initialSegmentStates,
                        Instant createdAt, boolean resumeMode) {
        List<AudioGenerationUnit> generationUnits = request.generationUnits();
        int total = generationUnits.size();
        long started = System.nanoTime();
        int completed = 0;
        int failed = 0;
        List<AudioSegmentSnapshot> segmentStates = initialSegmentStates;
        ArrayList<String> manifestLines = new ArrayList<>();
        try {
            Files.createDirectories(jobDir.resolve("audio"));
            Files.createDirectories(jobDir.resolve("final"));
            Files.createDirectories(jobDir.resolve("logs"));
            appendLog(jobDir, resumeMode ? "job_resuming" : "job_preparing", jobId,
                    resumeMode ? "Reanudando audio mock desde snapshot persistido." : "Preparando carpeta de audio mock.");
            publish(status(jobId, request, AudioJobState.PREPARING, AudioGenerationStage.PREPARING_WORKSPACE,
                    (int) segmentStates.stream().filter(AudioSegmentSnapshot::completed).count(), total, 0, "", "", 0L,
                    resumeMode ? "Reanudando job. Los segmentos completados se conservarán." : "Preparando carpeta de audio mock.",
                    jobDir, "", ""), consumer, request.projectDirectory(), segmentStates, createdAt);

            for (int index = 0; index < generationUnits.size(); index++) {
                AudioGenerationUnit segment = generationUnits.get(index);
                AudioSegmentSnapshot currentSnapshot = segmentStates.get(index);
                if (currentSnapshot.completed()) {
                    completed++;
                    manifestLines.add(manifestEntry(segment.id(), audioPathInsideJob(currentSnapshot, segment.id()), currentSnapshot.durationSeconds()));
                    publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                            completed, total, failed, segment.id(), segment.title(), etaSeconds(started, completed, total),
                            "Conservado WAV existente para " + segment.id() + ".", jobDir, "", ""),
                            consumer, request.projectDirectory(), segmentStates, createdAt);
                    continue;
                }
                if (cancellation.get()) {
                    segmentStates = cancelPending(segmentStates, index);
                    AudioJobStatusDto cancelled = status(jobId, request, AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                            completed, total, failed, segment.id(), segment.title(), 0L,
                            "Job cancelado. Los WAV ya generados se conservaron.", jobDir, "", "");
                    appendLog(jobDir, "job_cancelled", jobId, "Cancelado con " + completed + " segmentos completados.");
                    publish(cancelled, consumer, request.projectDirectory(), segmentStates, createdAt);
                    return;
                }
                segmentStates = replaceSegment(segmentStates, index, currentSnapshot.generating());
                publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                        completed, total, failed, segment.id(), segment.title(), etaSeconds(started, Math.max(1, completed), total),
                        "Generando audio mock para " + segment.id() + ".", jobDir, "", ""),
                        consumer, request.projectDirectory(), segmentStates, createdAt);
                appendLog(jobDir, "segment_started", jobId, segment.id());
                sleepSegmentDelay();
                Path clip = jobDir.resolve("audio").resolve(segment.id() + ".wav");
                double duration = estimatedDuration(segment);
                wavWriter.writeSilence(clip, duration);
                completed++;
                String clipRelativePath = relative(request.projectDirectory(), clip);
                segmentStates = replaceSegment(segmentStates, index, segmentStates.get(index).completed(clipRelativePath, duration));
                manifestLines.add(manifestEntry(segment.id(), "audio/" + segment.id() + ".wav", duration));
                appendLog(jobDir, "segment_completed", jobId, segment.id());
                publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                        completed, total, failed, segment.id(), segment.title(), etaSeconds(started, completed, total),
                        "Segmento " + completed + " de " + total + " completado.", jobDir, "", ""),
                        consumer, request.projectDirectory(), segmentStates, createdAt);
            }

            publish(status(jobId, request, AudioJobState.MERGING_AUDIO, AudioGenerationStage.MERGING_SEGMENTS,
                    completed, total, failed, "", "", 1L, "Creando audio final mock.", jobDir, "", ""),
                    consumer, request.projectDirectory(), segmentStates, createdAt);
            Path finalAudio = jobDir.resolve("final").resolve("podcast-mock.wav");
            wavWriter.writeSilence(finalAudio, Math.max(0.5, total * 0.15));
            Path manifest = jobDir.resolve("audio-manifest.json");
            Files.writeString(manifest, manifestJson(jobId, manifestLines, "final/podcast-mock.wav"), StandardCharsets.UTF_8);
            appendLog(jobDir, resumeMode ? "mock_audio_resumed_completed" : "mock_audio_completed", jobId,
                    resumeMode ? "Reanudación mock completada." : "Audio mock completado.");
            publish(status(jobId, request, AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                    completed, total, failed, "", "", 0L,
                    resumeMode ? "Audio mock reanudado y completado." : "Audio mock completado. Se generó WAV final y manifest.",
                    jobDir, relative(request.projectDirectory(), finalAudio), relative(request.projectDirectory(), manifest)),
                    consumer, request.projectDirectory(), segmentStates, createdAt);
        } catch (Exception ex) {
            failed = Math.max(1, total - completed);
            List<AudioSegmentSnapshot> failedStates = markFirstPendingAsFailed(segmentStates, ex.getMessage());
            try {
                appendLog(jobDir, "job_failed", jobId, ex.getMessage());
            } catch (IOException ignored) {
                // Keep the original failure as the user-visible status.
            }
            publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED,
                    completed, total, failed, "", "", 0L, "Falló la generación mock: " + ex.getMessage(),
                    jobDir, "", ""), consumer, request.projectDirectory(), failedStates, createdAt);
        } finally {
            cancellations.remove(jobId);
        }
    }

    private AudioJobStatusDto status(String jobId, AudioGenerationRequest request, AudioJobState state, AudioGenerationStage stage,
                                     int completed, int total, int failed, String segmentId, String segmentTitle, long eta,
                                     String message, Path jobDir, String finalAudioPath, String manifestPath) {
        return new AudioJobStatusDto(jobId, request.script().title(), state, stage, completed, total, failed,
                total == 0 ? 0.0 : (double) completed / Math.max(1, total), segmentId, segmentTitle,
                eta, eta <= 0 ? 0 : Math.max(1, Math.round(eta * 0.15)),
                message, jobDir.toString(), finalAudioPath, manifestPath);
    }

    private void publish(AudioJobStatusDto status, Consumer<AudioJobStatusDto> consumer, Path projectDirectory,
                         List<AudioSegmentSnapshot> segments, Instant createdAt) {
        queue.update(status);
        persist(projectDirectory, status, segments, createdAt);
        consumer.accept(status);
    }

    private void persist(Path projectDirectory, AudioJobStatusDto status, List<AudioSegmentSnapshot> segments, Instant createdAt) {
        try {
            String jobRelativeDirectory = "jobs/" + status.jobId();
            jobRepository.save(projectDirectory, AudioJobSnapshotMapper.fromStatus(status, jobRelativeDirectory, segments, createdAt));
        } catch (IOException ex) {
            // Persistence errors are reflected as logs in later real gateways. The mock keeps the UI flow alive.
        }
    }

    private static List<AudioSegmentSnapshot> pendingSegments(AudioGenerationRequest request) {
        return request.generationUnits().stream()
                .map(unit -> AudioSegmentSnapshot.pending(
                        unit.id(), unit.effectiveTitle(), unit.sourceFingerprint()))
                .toList();
    }

    private static List<AudioSegmentSnapshot> mergedResumeSegments(AudioGenerationRequest request, AudioJobSnapshot snapshot) {
        Map<String, AudioSegmentSnapshot> persistedById = new LinkedHashMap<>();
        for (AudioSegmentSnapshot segment : snapshot.segments()) {
            persistedById.put(segment.segmentId(), segment);
        }
        ArrayList<AudioSegmentSnapshot> merged = new ArrayList<>();
        for (AudioGenerationUnit unit : request.generationUnits()) {
            AudioSegmentSnapshot persisted = persistedById.get(unit.id());
            if (persisted == null) {
                merged.add(AudioSegmentSnapshot.pending(
                        unit.id(), unit.effectiveTitle(), unit.sourceFingerprint()));
            } else if (persisted.reusableFor(unit.sourceFingerprint())) {
                merged.add(persisted);
            } else {
                // Failed, cancelled, generating-at-crash and pending units are eligible for regeneration.
                merged.add(new AudioSegmentSnapshot(unit.id(), unit.effectiveTitle(), persisted.status(),
                        persisted.audioRelativePath(), persisted.durationSeconds(), persisted.attempts(),
                        persisted.errorMessage(), unit.sourceFingerprint()));
            }
        }
        return List.copyOf(merged);
    }

    private static List<AudioSegmentSnapshot> replaceSegment(List<AudioSegmentSnapshot> segments, int index, AudioSegmentSnapshot replacement) {
        ArrayList<AudioSegmentSnapshot> updated = new ArrayList<>(segments);
        updated.set(index, replacement);
        return List.copyOf(updated);
    }

    private static List<AudioSegmentSnapshot> cancelPending(List<AudioSegmentSnapshot> segments, int startIndex) {
        ArrayList<AudioSegmentSnapshot> updated = new ArrayList<>(segments);
        for (int i = startIndex; i < updated.size(); i++) {
            AudioSegmentSnapshot current = updated.get(i);
            if (!current.completed()) {
                updated.set(i, current.cancelled());
            }
        }
        return List.copyOf(updated);
    }

    private static List<AudioSegmentSnapshot> markFirstPendingAsFailed(List<AudioSegmentSnapshot> segments, String error) {
        ArrayList<AudioSegmentSnapshot> updated = new ArrayList<>(segments);
        for (int i = 0; i < updated.size(); i++) {
            AudioSegmentSnapshot current = updated.get(i);
            if (!current.completed()) {
                updated.set(i, current.failed(error));
                break;
            }
        }
        return List.copyOf(updated);
    }

    private String nextJobId() {
        return "JOB-" + LocalDateTime.now().format(JOB_TIME) + "-" + String.format(java.util.Locale.ROOT, "%03d", sequence.getAndIncrement());
    }

    private long etaSeconds(long startedNanos, int completed, int total) {
        if (completed <= 0 || total <= completed) {
            return 0L;
        }
        double elapsedSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
        double average = elapsedSeconds / completed;
        return Math.max(1L, Math.round(average * (total - completed)));
    }

    private void sleepSegmentDelay() throws InterruptedException {
        if (segmentDelayMillis > 0L) {
            Thread.sleep(segmentDelayMillis);
        }
    }

    private static double estimatedDuration(AudioGenerationUnit segment) {
        return Math.max(0.25, Math.min(2.5, 0.25 + segment.characterCount() / 220.0));
    }

    private static String manifestEntry(String segmentId, String audioPath, double duration) {
        return "    {\"segmentId\":\"" + escape(segmentId) + "\",\"audio\":\"" + escape(audioPath)
                + "\",\"durationSeconds\":" + String.format(java.util.Locale.ROOT, "%.2f", duration) + "}";
    }

    private static String audioPathInsideJob(AudioSegmentSnapshot snapshot, String segmentId) {
        String path = snapshot.audioRelativePath();
        if (path.isBlank()) {
            return "audio/" + segmentId + ".wav";
        }
        int marker = path.indexOf("/audio/");
        if (marker >= 0) {
            return path.substring(marker + 1);
        }
        return path.startsWith("audio/") ? path : "audio/" + segmentId + ".wav";
    }

    private static String manifestJson(String jobId, List<String> clipLines, String finalAudio) {
        return "{\n"
                + "  \"jobId\": \"" + escape(jobId) + "\",\n"
                + "  \"finalAudio\": \"" + escape(finalAudio) + "\",\n"
                + "  \"clips\": [\n"
                + String.join(",\n", clipLines)
                + "\n  ]\n"
                + "}\n";
    }

    private static void appendLog(Path jobDir, String event, String jobId, String message) throws IOException {
        Path log = jobDir.resolve("logs").resolve("generation-log.jsonl");
        Files.createDirectories(log.getParent());
        String line = "{\"event\":\"" + escape(event) + "\",\"jobId\":\"" + escape(jobId)
                + "\",\"message\":\"" + escape(message) + "\"}\n";
        Files.writeString(log, line, StandardCharsets.UTF_8, Files.exists(log)
                ? java.nio.file.StandardOpenOption.APPEND
                : java.nio.file.StandardOpenOption.CREATE);
    }

    private static String relative(Path projectDirectory, Path target) {
        try {
            return projectDirectory.toAbsolutePath().normalize().relativize(target.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return target.toString().replace('\\', '/');
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class AudioThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "docupodcast-audio-mock-worker");
            thread.setDaemon(true);
            return thread;
        }
    }
}
