package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioProcessDiagnosticsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Real local TTS gateway based on a configured command-line worker.
 *
 * <p>The gateway intentionally does not open a HTTP API. JavaFX remains the only visible application.
 * A command template such as {@code runtime/tts/tts_worker.exe --text-file {textFile} --output-file {outputFile}}
 * can call XTTS, Piper or any other local engine packaged with the app or installed by the user.</p>
 */
public final class LocalTtsProcessAudioGenerationGateway implements AudioGenerationGateway {
    private static final DateTimeFormatter JOB_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final InMemoryAudioJobQueue queue;
    private final AudioJobRepository jobRepository;
    private final LocalTtsProcessConfiguration configuration;
    private final AudioProcessDiagnosticsRepository diagnosticsRepository;
    private final ExternalProcessRunner processRunner;
    private final ExecutorService executor;
    private final WavAudioDurationProbe durationProbe = new WavAudioDurationProbe();
    private final AtomicInteger sequence = new AtomicInteger(1);
    private final ConcurrentHashMap<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();

    public LocalTtsProcessAudioGenerationGateway(LocalTtsProcessConfiguration configuration) {
        this(new InMemoryAudioJobQueue(), new AudioJobFileRepository(), new AudioProcessDiagnosticsFileRepository(), configuration);
    }

    public LocalTtsProcessAudioGenerationGateway(InMemoryAudioJobQueue queue, AudioJobRepository jobRepository,
                                                 LocalTtsProcessConfiguration configuration) {
        this(queue, jobRepository, new AudioProcessDiagnosticsFileRepository(), configuration);
    }

    public LocalTtsProcessAudioGenerationGateway(InMemoryAudioJobQueue queue, AudioJobRepository jobRepository,
                                                 AudioProcessDiagnosticsRepository diagnosticsRepository,
                                                 LocalTtsProcessConfiguration configuration) {
        this(queue, jobRepository, diagnosticsRepository, configuration, new DefaultExternalProcessRunner());
    }

    public LocalTtsProcessAudioGenerationGateway(InMemoryAudioJobQueue queue, AudioJobRepository jobRepository,
                                                 AudioProcessDiagnosticsRepository diagnosticsRepository,
                                                 LocalTtsProcessConfiguration configuration,
                                                 ExternalProcessRunner processRunner) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository");
        this.diagnosticsRepository = Objects.requireNonNull(diagnosticsRepository, "diagnosticsRepository");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.processRunner = processRunner == null ? new DefaultExternalProcessRunner() : processRunner;
        this.executor = Executors.newSingleThreadExecutor(new TtsThreadFactory());
    }

    @Override
    public AudioEngineDescriptor engineDescriptor() {
        return configuration.descriptor();
    }

    @Override
    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        requireReady();
        Objects.requireNonNull(request, "request");
        Consumer<AudioJobStatusDto> consumer = statusConsumer == null ? status -> { } : statusConsumer;
        String jobId = nextJobId();
        AtomicBoolean cancellation = new AtomicBoolean(false);
        cancellations.put(jobId, cancellation);
        Path jobDir = request.projectDirectory().resolve("jobs").resolve(jobId).normalize();
        List<AudioSegmentSnapshot> segments = pendingSegments(request);
        Instant createdAt = Instant.now();
        AudioJobStatusDto queued = status(jobId, request, AudioJobState.QUEUED, AudioGenerationStage.PREPARING_WORKSPACE,
                0, segments.size(), 0, "", "", 0L,
                "Trabajo TTS en cola. Motor: " + configuration.displayName() + ".", jobDir, "", "");
        publish(queued, consumer, request.projectDirectory(), segments, createdAt);
        executor.submit(() -> runJob(jobId, request, jobDir, cancellation, consumer, segments, createdAt, false));
        return jobId;
    }

    @Override
    public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
        requireReady();
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(snapshot, "snapshot");
        Consumer<AudioJobStatusDto> consumer = statusConsumer == null ? status -> { } : statusConsumer;
        String jobId = snapshot.jobId();
        AtomicBoolean cancellation = new AtomicBoolean(false);
        cancellations.put(jobId, cancellation);
        Path jobDir = request.projectDirectory().resolve(snapshot.jobRelativeDirectory()).normalize();
        List<AudioSegmentSnapshot> segments = mergedResumeSegments(request, snapshot);
        int completed = (int) segments.stream().filter(AudioSegmentSnapshot::completed).count();
        Instant createdAt = snapshot.createdAt();
        AudioJobStatusDto queued = status(jobId, request, AudioJobState.QUEUED, AudioGenerationStage.PREPARING_WORKSPACE,
                completed, segments.size(), 0, "", "", 0L,
                "Reanudación TTS en cola. Se conservarán WAVs completados.", jobDir,
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
                "Cancelación solicitada. Se intentará detener el proceso TTS activo y se conservarán WAVs completados.",
                status.outputDirectory(), status.finalAudioPath(), status.manifestPath()
        )));
        return true;
    }

    @Override
    public List<AudioJobStatusDto> listStatuses() {
        return queue.list();
    }

    private void runJob(String jobId, AudioGenerationRequest request, Path jobDir, AtomicBoolean cancellation,
                        Consumer<AudioJobStatusDto> consumer, List<AudioSegmentSnapshot> initialStates,
                        Instant createdAt, boolean resumeMode) {
        java.util.concurrent.locks.Lock workspace = AudioJobWorkspaceLockRegistry.writer(request.projectDirectory());
        workspace.lock();
        try {
            runJobWithWorkspaceLock(jobId, request, jobDir, cancellation, consumer, initialStates, createdAt, resumeMode);
        } finally {
            workspace.unlock();
        }
    }

    private void runJobWithWorkspaceLock(String jobId, AudioGenerationRequest request, Path jobDir, AtomicBoolean cancellation,
                        Consumer<AudioJobStatusDto> consumer, List<AudioSegmentSnapshot> initialStates,
                        Instant createdAt, boolean resumeMode) {
        List<AudioGenerationUnit> generationUnits = request.generationUnits();
        int total = generationUnits.size();
        long started = System.nanoTime();
        int completed = 0;
        int failed = 0;
        List<AudioSegmentSnapshot> segmentStates = initialStates;
        ArrayList<String> manifestLines = new ArrayList<>();
        try {
            Files.createDirectories(jobDir.resolve("segments"));
            Files.createDirectories(jobDir.resolve("audio"));
            Files.createDirectories(jobDir.resolve("final"));
            Files.createDirectories(jobDir.resolve("logs"));
            LocalTtsPreflightReport preflight = configuration.preflightReport();
            appendLog(jobDir, "tts_preflight", jobId, preflight.diagnosticLine());
            if (!preflight.ready()) {
                publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED,
                        0, total, total, "", "", 0L, preflight.userMessage(), jobDir, "", ""),
                        consumer, request.projectDirectory(), markFirstPendingAsFailed(segmentStates, preflight.userMessage()), createdAt);
                return;
            }
            appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.success(jobId, "PREFLIGHT", 1,
                    configuration.displayName(), commandSummary(LocalTtsProcessConfiguration.splitCommand(configuration.commandTemplate())),
                    0L, "", 0L, preflight.diagnosticLine()));
            appendLog(jobDir, resumeMode ? "tts_job_resuming" : "tts_job_preparing", jobId,
                    "Motor TTS: " + configuration.displayName());
            publish(status(jobId, request, AudioJobState.PREPARING, AudioGenerationStage.PREPARING_WORKSPACE,
                    (int) segmentStates.stream().filter(AudioSegmentSnapshot::completed).count(), total, 0, "", "", 0L,
                    resumeMode ? "Reanudando TTS real desde snapshot." : "Preparando trabajo TTS real.", jobDir, "", ""),
                    consumer, request.projectDirectory(), segmentStates, createdAt);

            if (runXttsBatchJobIfAvailable(jobId, request, jobDir, cancellation, consumer,
                    generationUnits, segmentStates, createdAt, resumeMode, started)) {
                return;
            }

            for (int index = 0; index < generationUnits.size(); index++) {
                AudioGenerationUnit segment = generationUnits.get(index);
                AudioSegmentSnapshot current = segmentStates.get(index);
                if (current.completed()) {
                    completed++;
                    manifestLines.add(manifestEntry(segment.id(), audioPathInsideJob(current, segment.id()), current.durationSeconds()));
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
                            "Job TTS cancelado. Los WAV completados se conservaron.", jobDir, "", "");
                    appendLog(jobDir, "tts_job_cancelled", jobId, "Cancelado con " + completed + " segmentos completados.");
                    publish(cancelled, consumer, request.projectDirectory(), segmentStates, createdAt);
                    return;
                }
                segmentStates = replaceSegment(segmentStates, index, current.generating());
                publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                        completed, total, failed, segment.id(), segment.title(), etaSeconds(started, Math.max(1, completed), total),
                        "Generando TTS real para " + segment.id() + ".", jobDir, "", ""),
                        consumer, request.projectDirectory(), segmentStates, createdAt);

                Path textFile = jobDir.resolve("segments").resolve(segment.id() + ".txt");
                Path outputFile = jobDir.resolve("audio").resolve(segment.id() + ".wav");
                String sanitizedText = TtsTextPreprocessor.sanitizeForEngine(segment.text(), configuration.displayName(), configuration.commandTemplate());
                Files.writeString(textFile, sanitizedText, StandardCharsets.UTF_8);
                if (TtsTextPreprocessor.changed(segment.text(), sanitizedText)) {
                    appendLog(jobDir, "tts_text_sanitized", jobId, segment.id() + " rawChars="
                            + segment.text().length() + " sanitizedChars=" + sanitizedText.length());
                }
                int maxAttempts = effectiveAudioMaxAttempts();
                boolean generated = false;
                String lastError = "";
                int firstAttempt = 1;
                for (int attempt = firstAttempt; attempt <= maxAttempts; attempt++) {
                    if (cancellation.get()) {
                        segmentStates = cancelPending(segmentStates, index);
                        AudioJobStatusDto cancelled = status(jobId, request, AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                                completed, total, failed, segment.id(), segment.title(), 0L,
                                "Job TTS cancelado antes del intento " + attempt + " de " + segment.id() + ".", jobDir, "", "");
                        appendLog(jobDir, "tts_job_cancelled", jobId, "Cancelado antes de intento " + attempt + " de " + segment.id());
                        publish(cancelled, consumer, request.projectDirectory(), segmentStates, createdAt);
                        return;
                    }
                    segmentStates = replaceSegment(segmentStates, index, new AudioSegmentSnapshot(segment.id(), segment.title(),
                            AudioSegmentStatus.GENERATING, current.audioRelativePath(), current.durationSeconds(), attempt, ""));
                    publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                            completed, total, failed, segment.id(), segment.title(), etaSeconds(started, Math.max(1, completed), total),
                            "Generando TTS real para " + segment.id() + " (intento " + attempt + "/" + maxAttempts + ").", jobDir, "", ""),
                            consumer, request.projectDirectory(), segmentStates, createdAt);
                    appendLog(jobDir, "tts_segment_started", jobId, segment.id() + " attempt=" + attempt);
                    Files.deleteIfExists(outputFile);
                    Path referenceSample = request.referenceSamplePathFor(segment).orElse(null);
                    if (referenceSample != null) {
                        appendLog(jobDir, "tts_reference_sample", jobId, segment.id() + " voice="
                                + segment.effectiveVoiceProfileId(request.voiceProfileId())
                                + " tone=" + segment.performanceStyleId()
                                + " sample=" + referenceSample);
                    }
                    List<String> command = configuration.commandFor(segment.id(), textFile, outputFile,
                            request.language(), segment.effectiveVoiceProfileId(request.voiceProfileId()), referenceSample);
                    final int completedSnapshot = completed;
                    final int failedSnapshot = failed;
                    final int attemptSnapshot = attempt;
                    final List<AudioSegmentSnapshot> progressStates = segmentStates;
                    AtomicReference<String> lastProcessPhase = new AtomicReference<>("");
                    AtomicLong lastProcessPhaseAt = new AtomicLong(0L);
                    ProcessResult result = runCommand(jobId, command, jobDir, cancellation, line -> {
                        String phaseMessage = processProgressMessage(line);
                        if (phaseMessage.isBlank()) {
                            return;
                        }
                        long now = System.nanoTime();
                        String previous = lastProcessPhase.get();
                        if (phaseMessage.equals(previous)
                                && now - lastProcessPhaseAt.get() < TimeUnit.SECONDS.toNanos(3)) {
                            return;
                        }
                        lastProcessPhase.set(phaseMessage);
                        lastProcessPhaseAt.set(now);
                        try {
                            appendLog(jobDir, "tts_process_phase", jobId, segment.id() + " " + phaseMessage);
                        } catch (IOException ignored) {
                            // Best-effort observability; the process itself remains authoritative.
                        }
                        publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                                completedSnapshot, total, failedSnapshot, segment.id(), segment.title(),
                                etaSeconds(started, Math.max(1, completedSnapshot), total),
                                phaseMessage + " (" + segment.id() + ", intento " + attemptSnapshot + "/" + maxAttempts + ").",
                                jobDir, "", ""),
                                consumer, request.projectDirectory(), progressStates, createdAt);
                    });
                    if (result.cancelled() || cancellation.get()) {
                        segmentStates = cancelPending(segmentStates, index);
                        AudioJobStatusDto cancelled = status(jobId, request, AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                                completed, total, failed, segment.id(), segment.title(), 0L,
                                "Job TTS cancelado. Se detuvo el proceso externo activo y se conservaron WAVs completados.", jobDir, "", "");
                        appendLog(jobDir, "tts_process_destroyed_by_cancel", jobId, segment.id());
                        appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.failure(jobId, segment.id(), attempt,
                                configuration.displayName(), commandSummary(command), result.exitCode(), result.durationMillis(),
                                result.timedOut(), "", 0L, "Proceso TTS cancelado por el usuario.", tail(result.output())));
                        publish(cancelled, consumer, request.projectDirectory(), segmentStates, createdAt);
                        return;
                    }
                    long outputBytes = Files.exists(outputFile) ? Files.size(outputFile) : 0L;
                    String outputRelative = outputBytes > 0L ? relative(request.projectDirectory(), outputFile) : "";
                    double measuredDuration = 0.0;
                    String wavInspectionError = "";
                    if (result.exitCode() == 0 && outputBytes > 44L) {
                        try {
                            measuredDuration = durationProbe.durationSeconds(outputFile);
                        } catch (IOException ex) {
                            wavInspectionError = ex.getMessage();
                        }
                    }
                    boolean ok = result.exitCode() == 0 && outputBytes > 44L && measuredDuration > 0.0;
                    String failureMessage = wavInspectionError.isBlank() ? "TTS falló para " + segment.id()
                            : "El WAV generado no tiene duración reproducible: " + wavInspectionError;
                    AudioProcessDiagnosticEvent diagnostic = ok
                            ? AudioProcessDiagnosticEvent.success(jobId, segment.id(), attempt, configuration.displayName(),
                                    commandSummary(command), result.durationMillis(), outputRelative, outputBytes, tail(result.output()))
                            : AudioProcessDiagnosticEvent.failure(jobId, segment.id(), attempt, configuration.displayName(),
                                    commandSummary(command), result.exitCode(), result.durationMillis(), result.timedOut(),
                                    outputRelative, outputBytes, failureMessage, tail(result.output()));
                    appendDiagnostic(request.projectDirectory(), diagnostic);
                    if (ok) {
                        completed++;
                        double duration = measuredDuration;
                        String clipRelativePath = relative(request.projectDirectory(), outputFile);
                        segmentStates = replaceSegment(segmentStates, index, segmentStates.get(index).completed(clipRelativePath, duration));
                        manifestLines.add(manifestEntry(segment.id(), "audio/" + segment.id() + ".wav", duration));
                        appendLog(jobDir, "tts_segment_completed", jobId, segment.id() + " attempt=" + attempt + " durationSeconds=" + String.format(java.util.Locale.ROOT, "%.3f", duration));
                        publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                                completed, total, failed, segment.id(), segment.title(), etaSeconds(started, completed, total),
                                "Segmento TTS " + completed + " de " + total + " completado en intento " + attempt + ".", jobDir, "", ""),
                                consumer, request.projectDirectory(), segmentStates, createdAt);
                        generated = true;
                        break;
                    }
                    lastError = failureMessage + " (exit=" + result.exitCode() + ", intento=" + attempt
                            + "/" + maxAttempts + "): " + tail(result.output());
                    appendLog(jobDir, "tts_segment_failed", jobId, lastError);
                    if (attempt < maxAttempts) {
                        publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                                completed, total, failed, segment.id(), segment.title(), etaSeconds(started, Math.max(1, completed), total),
                                "Reintentando " + segment.id() + " tras fallo del proceso TTS.", jobDir, "", ""),
                                consumer, request.projectDirectory(), segmentStates, createdAt);
                    }
                }
                if (!generated) {
                    segmentStates = replaceSegment(segmentStates, index, segmentStates.get(index).failed(lastError));
                    failed++;
                    publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                            completed, total, failed, segment.id(), segment.title(),
                            etaSeconds(started, Math.max(1, completed), total),
                            "Segmento " + segment.id() + " fallo tras " + maxAttempts
                                    + " intentos; se continua con los fragmentos restantes.", jobDir, "", ""),
                            consumer, request.projectDirectory(), segmentStates, createdAt);
                    continue;
                }
            }

            if (failed > 0) {
                Path manifest = jobDir.resolve("audio-manifest.json");
                Files.writeString(manifest, manifestJson(jobId, manifestLines, ""), StandardCharsets.UTF_8);
                String message = "TTS real finalizo con " + failed + " segmento(s) fallidos. "
                        + "Los WAV completados quedan conservados y el job es reanudable.";
                appendLog(jobDir, "tts_audio_completed_with_failures", jobId, message);
                publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED,
                        completed, total, failed, "", "", 0L, message, jobDir,
                        "", relative(request.projectDirectory(), manifest)),
                        consumer, request.projectDirectory(), segmentStates, createdAt);
                return;
            }

            publish(status(jobId, request, AudioJobState.MERGING_AUDIO, AudioGenerationStage.MERGING_SEGMENTS,
                    completed, total, failed, "", "", 1L,
                    "Creando manifest y audio final lógico. La unión real de WAVs queda para exportación avanzada.", jobDir, "", ""),
                    consumer, request.projectDirectory(), segmentStates, createdAt);
            Path manifest = jobDir.resolve("audio-manifest.json");
            Files.writeString(manifest, manifestJson(jobId, manifestLines, ""), StandardCharsets.UTF_8);
            appendLog(jobDir, resumeMode ? "tts_audio_resumed_completed" : "tts_audio_completed", jobId,
                    "TTS real completó todos los segmentos.");
            publish(status(jobId, request, AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                    completed, total, failed, "", "", 0L,
                    "TTS real completado. Se generaron WAVs por segmento y manifest.", jobDir,
                    "", relative(request.projectDirectory(), manifest)),
                    consumer, request.projectDirectory(), segmentStates, createdAt);
        } catch (Exception ex) {
            failed = Math.max(1, total - completed);
            List<AudioSegmentSnapshot> failedStates = markFirstPendingAsFailed(segmentStates, ex.getMessage());
            try {
                appendLog(jobDir, "tts_job_failed", jobId, ex.getMessage());
            } catch (IOException ignored) {
                // Keep original exception as status.
            }
            publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED,
                    completed, total, failed, "", "", 0L, "Falló el TTS real: " + ex.getMessage(),
                    jobDir, "", ""), consumer, request.projectDirectory(), failedStates, createdAt);
        } finally {
            cancellations.remove(jobId);
        }
    }

    private boolean runXttsBatchJobIfAvailable(String jobId, AudioGenerationRequest request, Path jobDir,
                                               AtomicBoolean cancellation, Consumer<AudioJobStatusDto> consumer,
                                               List<AudioGenerationUnit> generationUnits,
                                               List<AudioSegmentSnapshot> initialStates,
                                               Instant createdAt, boolean resumeMode, long startedNanos)
            throws IOException, InterruptedException {
        Optional<XttsBatchRuntime> runtime = XttsBatchRuntime.from(configuration);
        if (runtime.isEmpty()) {
            return false;
        }
        ArrayList<Integer> pendingIndexes = new ArrayList<>();
        ArrayList<String> manifestLines = new ArrayList<>();
        int completedBeforeBatch = 0;
        for (int index = 0; index < generationUnits.size(); index++) {
            AudioSegmentSnapshot state = initialStates.get(index);
            if (state.completed()) {
                completedBeforeBatch++;
                manifestLines.add(manifestEntry(generationUnits.get(index).id(),
                        audioPathInsideJob(state, generationUnits.get(index).id()), state.durationSeconds()));
            } else {
                pendingIndexes.add(index);
            }
        }
        if (pendingIndexes.isEmpty()) {
            return false;
        }

        Files.createDirectories(jobDir.resolve("segments"));
        Files.createDirectories(jobDir.resolve("audio"));
        Files.createDirectories(jobDir.resolve("logs"));

        AtomicReference<List<AudioSegmentSnapshot>> statesRef = new AtomicReference<>(initialStates);
        AtomicInteger completedRef = new AtomicInteger(completedBeforeBatch);
        AtomicInteger failedRef = new AtomicInteger(0);
        AtomicReference<String> currentSegmentId = new AtomicReference<>("");
        AtomicReference<String> currentSegmentTitle = new AtomicReference<>("");

        publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                completedRef.get(), generationUnits.size(), 0, "", "", etaSeconds(startedNanos, Math.max(1, completedRef.get()), generationUnits.size()),
                "Voz IA avanzada usara modo batch: una carga de modelo para todos los segmentos pendientes.",
                jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);

        int maxAttempts = effectiveAudioMaxAttempts();
        ProcessResult result = null;
        List<String> command = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            List<Integer> attemptIndexes = pendingBatchIndexes(statesRef.get(), pendingIndexes);
            if (attemptIndexes.isEmpty()) {
                break;
            }
            Path batchManifest = jobDir.resolve("segments").resolve("xtts-batch-manifest.json");
            Files.writeString(batchManifest, xttsBatchManifestJson(request, generationUnits, attemptIndexes, jobDir, runtime.get()),
                    StandardCharsets.UTF_8);
            command = runtime.get().command(batchManifest, request.language(), configuration);
            List<String> commandSnapshot = command;
            appendLog(jobDir, "tts_xtts_batch_started", jobId,
                    "Voz IA avanzada batch intento " + attempt + "/" + maxAttempts + ": "
                            + attemptIndexes.size() + " segmentos pendientes.");
            statesRef.set(markBatchAttempt(statesRef.get(), attemptIndexes, attempt));
            failedRef.set(failedSegmentCount(statesRef.get()));
            publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                    completedRef.get(), generationUnits.size(), failedRef.get(), currentSegmentId.get(), currentSegmentTitle.get(),
                    etaSeconds(startedNanos, Math.max(1, completedRef.get()), generationUnits.size()),
                    "Voz IA avanzada batch intento " + attempt + "/" + maxAttempts + " sobre "
                            + attemptIndexes.size() + " fragmentos pendientes.",
                    jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);

            result = runCommand(jobId, commandSnapshot, jobDir, cancellation, line -> {
                try {
                    handleXttsBatchProgressLine(line, jobId, request, jobDir, consumer, generationUnits, statesRef,
                            completedRef, failedRef, currentSegmentId, currentSegmentTitle, manifestLines, createdAt, startedNanos, commandSnapshot);
                } catch (RuntimeException | IOException ignored) {
                    // Progress persistence is best-effort; final file inspection below remains authoritative.
                }
            });

            if (result.cancelled() || cancellation.get()) {
                List<AudioSegmentSnapshot> cancelled = cancelPending(statesRef.get(), firstPendingIndex(statesRef.get()));
                appendLog(jobDir, "tts_xtts_batch_cancelled", jobId, "Proceso batch cancelado por el usuario.");
                appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.failure(jobId, "BATCH", attempt,
                        configuration.displayName(), commandSummary(commandSnapshot), result.exitCode(), result.durationMillis(),
                        result.timedOut(), "", 0L, "Proceso TTS batch cancelado por el usuario.", tail(result.output())));
                publish(status(jobId, request, AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                        completedRef.get(), generationUnits.size(), failedRef.get(), currentSegmentId.get(), currentSegmentTitle.get(), 0L,
                        "Job TTS cancelado. Se detuvo el proceso XTTS batch y se conservaron WAVs completados.",
                        jobDir, "", ""), consumer, request.projectDirectory(), cancelled, createdAt);
                return true;
            }

            inspectBatchOutputs(jobId, request, jobDir, generationUnits, attemptIndexes, statesRef,
                    completedRef, failedRef, manifestLines, commandSnapshot, result);
            failedRef.set(failedSegmentCount(statesRef.get()));
            if (pendingBatchIndexes(statesRef.get(), pendingIndexes).isEmpty()) {
                break;
            }
            if (attempt < maxAttempts) {
                publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                        completedRef.get(), generationUnits.size(), failedRef.get(), currentSegmentId.get(), currentSegmentTitle.get(),
                        etaSeconds(startedNanos, Math.max(1, completedRef.get()), generationUnits.size()),
                        "Reintentando XTTS batch; se conservaran los WAVs ya completados.",
                        jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);
            }
        }

        failedRef.set(failedSegmentCount(statesRef.get()));
        List<Integer> remaining = pendingBatchIndexes(statesRef.get(), pendingIndexes);
        if (!remaining.isEmpty()) {
            String message = "Voz IA avanzada batch fallo tras " + maxAttempts
                    + " intentos. Fragmentos sin WAV valido: " + remaining.size()
                    + ". Ultima salida: " + (result == null ? "" : tail(result.output()));
            appendLog(jobDir, "tts_xtts_batch_failed", jobId, message);
            publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED,
                    completedRef.get(), generationUnits.size(), failedRef.get(), currentSegmentId.get(), currentSegmentTitle.get(), 0L,
                    message, jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);
            return true;
        }

        publish(status(jobId, request, AudioJobState.MERGING_AUDIO, AudioGenerationStage.MERGING_SEGMENTS,
                completedRef.get(), generationUnits.size(), failedRef.get(), "", "", 1L,
                "Creando manifest y audio final logico. XTTS batch completo sin recargar modelo por segmento.",
                jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);
        Path manifest = jobDir.resolve("audio-manifest.json");
        Files.writeString(manifest, manifestJson(jobId, manifestLines, ""), StandardCharsets.UTF_8);
        appendLog(jobDir, resumeMode ? "tts_audio_resumed_completed" : "tts_audio_completed", jobId,
                "TTS real completo todos los segmentos con XTTS batch.");
        appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.success(jobId, "BATCH", 1,
                configuration.displayName(), commandSummary(command), result == null ? 0L : result.durationMillis(),
                relative(request.projectDirectory(), manifest), Files.size(manifest), result == null ? "" : tail(result.output())));
        publish(status(jobId, request, AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                completedRef.get(), generationUnits.size(), failedRef.get(), "", "", 0L,
                "TTS real completado. Voz IA avanzada uso batch con una sola carga de modelo.",
                jobDir, "", relative(request.projectDirectory(), manifest)),
                consumer, request.projectDirectory(), statesRef.get(), createdAt);
        return true;
    }

    private int effectiveAudioMaxAttempts() {
        return GenerationAttemptPolicy.robustAudioMaxAttemptsFromRetries(configuration.maxRetries());
    }

    private void handleXttsBatchProgressLine(String line, String jobId, AudioGenerationRequest request, Path jobDir,
                                             Consumer<AudioJobStatusDto> consumer,
                                             List<AudioGenerationUnit> generationUnits,
                                             AtomicReference<List<AudioSegmentSnapshot>> statesRef,
                                             AtomicInteger completedRef, AtomicInteger failedRef,
                                             AtomicReference<String> currentSegmentId,
                                             AtomicReference<String> currentSegmentTitle,
                                             List<String> manifestLines, Instant createdAt, long startedNanos,
                                             List<String> command)
            throws IOException {
        String text = line == null ? "" : line.strip();
        if (text.contains("DOCUPODCAST_XTTS_BATCH: segment_start=")) {
            String segmentId = valueAfter(text, "segment_start=");
            int index = generationUnitIndex(generationUnits, segmentId);
            if (index >= 0) {
                AudioGenerationUnit unit = generationUnits.get(index);
                currentSegmentId.set(unit.id());
                currentSegmentTitle.set(unit.title());
                statesRef.set(replaceSegment(statesRef.get(), index, statesRef.get().get(index).generating()));
                appendLog(jobDir, "tts_xtts_batch_segment_started", jobId, unit.id());
                publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                        completedRef.get(), generationUnits.size(), failedRef.get(), unit.id(), unit.title(),
                        etaSeconds(startedNanos, Math.max(1, completedRef.get()), generationUnits.size()),
                        "Voz IA avanzada batch sintetizando " + unit.id() + ".",
                        jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);
            }
            return;
        }
        if (text.contains("DOCUPODCAST_XTTS_BATCH: segment_done=")) {
            String segmentId = valueAfter(text, "segment_done=");
            int space = segmentId.indexOf(' ');
            if (space > 0) {
                segmentId = segmentId.substring(0, space);
            }
            completeBatchSegment(jobId, request, jobDir, consumer, generationUnits, statesRef, completedRef,
                    failedRef, manifestLines, createdAt, startedNanos, command, segmentId);
            return;
        }
        String phase = processProgressMessage(text);
        if (!phase.isBlank()) {
            publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                    completedRef.get(), generationUnits.size(), failedRef.get(), currentSegmentId.get(),
                    currentSegmentTitle.get(), etaSeconds(startedNanos, Math.max(1, completedRef.get()), generationUnits.size()),
                    phase + " (XTTS batch).", jobDir, "", ""),
                    consumer, request.projectDirectory(), statesRef.get(), createdAt);
        }
    }

    private void completeBatchSegment(String jobId, AudioGenerationRequest request, Path jobDir,
                                      Consumer<AudioJobStatusDto> consumer,
                                      List<AudioGenerationUnit> generationUnits,
                                      AtomicReference<List<AudioSegmentSnapshot>> statesRef,
                                      AtomicInteger completedRef, AtomicInteger failedRef,
                                      List<String> manifestLines, Instant createdAt, long startedNanos,
                                      List<String> command, String segmentId)
            throws IOException {
        int index = generationUnitIndex(generationUnits, segmentId);
        if (index < 0 || statesRef.get().get(index).completed()) {
            return;
        }
        AudioGenerationUnit unit = generationUnits.get(index);
        Path outputFile = jobDir.resolve("audio").resolve(unit.id() + ".wav");
        long outputBytes = Files.exists(outputFile) ? Files.size(outputFile) : 0L;
        if (outputBytes <= 44L) {
            return;
        }
        double duration = durationProbe.durationSeconds(outputFile);
        String clipRelativePath = relative(request.projectDirectory(), outputFile);
        statesRef.set(replaceSegment(statesRef.get(), index, statesRef.get().get(index).completed(clipRelativePath, duration)));
        synchronized (manifestLines) {
            if (manifestLines.stream().noneMatch(line -> line.contains("\"segmentId\": \"" + unit.id() + "\""))) {
                manifestLines.add(manifestEntry(unit.id(), "audio/" + unit.id() + ".wav", duration));
            }
        }
        int completed = completedRef.incrementAndGet();
        appendLog(jobDir, "tts_xtts_batch_segment_completed", jobId,
                unit.id() + " durationSeconds=" + String.format(java.util.Locale.ROOT, "%.3f", duration));
        appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.success(jobId, unit.id(), 1,
                configuration.displayName(), commandSummary(command), 0L, clipRelativePath, outputBytes,
                "XTTS batch genero el WAV dentro de un proceso persistente."));
        publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                completed, generationUnits.size(), failedRef.get(), unit.id(), unit.title(),
                etaSeconds(startedNanos, completed, generationUnits.size()),
                "Segmento TTS " + completed + " de " + generationUnits.size() + " completado por XTTS batch.",
                jobDir, "", ""), consumer, request.projectDirectory(), statesRef.get(), createdAt);
    }

    private void inspectBatchOutputs(String jobId, AudioGenerationRequest request, Path jobDir,
                                     List<AudioGenerationUnit> generationUnits, List<Integer> pendingIndexes,
                                     AtomicReference<List<AudioSegmentSnapshot>> statesRef,
                                     AtomicInteger completedRef, AtomicInteger failedRef,
                                     List<String> manifestLines, List<String> command, ProcessResult result)
            throws IOException {
        for (int index : pendingIndexes) {
            AudioSegmentSnapshot state = statesRef.get().get(index);
            if (state.completed()) {
                continue;
            }
            AudioGenerationUnit unit = generationUnits.get(index);
            Path outputFile = jobDir.resolve("audio").resolve(unit.id() + ".wav");
            long outputBytes = Files.exists(outputFile) ? Files.size(outputFile) : 0L;
            if (outputBytes > 44L) {
                completeBatchSegmentWithoutPublish(jobId, request, jobDir, generationUnits, statesRef,
                        completedRef, manifestLines, command, index, outputFile, outputBytes);
            } else {
                String error = "XTTS batch no genero WAV valido para " + unit.id()
                        + ". Codigo de salida: " + result.exitCode() + ". " + tail(result.output());
                statesRef.set(replaceSegment(statesRef.get(), index, state.failed(error)));
                failedRef.incrementAndGet();
                appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.failure(jobId, unit.id(), 1,
                        configuration.displayName(), commandSummary(command), result.exitCode(), result.durationMillis(),
                        result.timedOut(), "", outputBytes, error, tail(result.output())));
            }
        }
    }

    private void completeBatchSegmentWithoutPublish(String jobId, AudioGenerationRequest request, Path jobDir,
                                                    List<AudioGenerationUnit> generationUnits,
                                                    AtomicReference<List<AudioSegmentSnapshot>> statesRef,
                                                    AtomicInteger completedRef, List<String> manifestLines,
                                                    List<String> command, int index, Path outputFile, long outputBytes)
            throws IOException {
        AudioGenerationUnit unit = generationUnits.get(index);
        double duration = durationProbe.durationSeconds(outputFile);
        String clipRelativePath = relative(request.projectDirectory(), outputFile);
        statesRef.set(replaceSegment(statesRef.get(), index, statesRef.get().get(index).completed(clipRelativePath, duration)));
        synchronized (manifestLines) {
            if (manifestLines.stream().noneMatch(line -> line.contains("\"segmentId\": \"" + unit.id() + "\""))) {
                manifestLines.add(manifestEntry(unit.id(), "audio/" + unit.id() + ".wav", duration));
            }
        }
        completedRef.incrementAndGet();
        appendLog(jobDir, "tts_xtts_batch_segment_completed", jobId,
                unit.id() + " durationSeconds=" + String.format(java.util.Locale.ROOT, "%.3f", duration));
        appendDiagnostic(request.projectDirectory(), AudioProcessDiagnosticEvent.success(jobId, unit.id(), 1,
                configuration.displayName(), commandSummary(command), 0L, clipRelativePath, outputBytes,
                "XTTS batch genero el WAV dentro de un proceso persistente."));
    }

    private String xttsBatchManifestJson(AudioGenerationRequest request, List<AudioGenerationUnit> generationUnits,
                                         List<Integer> pendingIndexes, Path jobDir, XttsBatchRuntime runtime) {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"schema\": \"docupodcast-xtts-batch-v1\",\n  \"segments\": [");
        for (int i = 0; i < pendingIndexes.size(); i++) {
            int index = pendingIndexes.get(i);
            AudioGenerationUnit unit = generationUnits.get(index);
            Path textFile = jobDir.resolve("segments").resolve(unit.id() + ".txt");
            Path outputFile = jobDir.resolve("audio").resolve(unit.id() + ".wav");
            String sanitizedText = TtsTextPreprocessor.sanitizeForEngine(unit.text(), configuration.displayName(), configuration.commandTemplate());
            try {
                Files.createDirectories(textFile.getParent());
                Files.writeString(textFile, sanitizedText, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo escribir texto batch para " + unit.id() + ": " + ex.getMessage(), ex);
            }
            Path referenceSample;
            try {
                referenceSample = request.referenceSamplePathFor(unit).orElse(runtime.defaultSpeakerWav());
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo resolver muestra de voz batch para " + unit.id()
                        + ": " + ex.getMessage(), ex);
            }
            if (i > 0) {
                out.append(',');
            }
            out.append("\n    {")
                    .append("\"segmentId\": \"").append(escapeJson(unit.id())).append("\",")
                    .append(" \"textFile\": \"").append(escapeJson(textFile.toString())).append("\",")
                    .append(" \"outputFile\": \"").append(escapeJson(outputFile.toString())).append("\",")
                    .append(" \"speakerWav\": \"").append(escapeJson(referenceSample.toString())).append("\",")
                    .append(" \"language\": \"").append(escapeJson(request.language())).append("\"")
                    .append("}");
        }
        out.append("\n  ]\n}\n");
        return out.toString();
    }

    private static int firstPendingIndex(List<AudioSegmentSnapshot> states) {
        for (int i = 0; i < states.size(); i++) {
            if (!states.get(i).completed()) {
                return i;
            }
        }
        return states.size();
    }

    private static List<Integer> pendingBatchIndexes(List<AudioSegmentSnapshot> states, List<Integer> candidateIndexes) {
        if (states == null || candidateIndexes == null || candidateIndexes.isEmpty()) {
            return List.of();
        }
        ArrayList<Integer> pending = new ArrayList<>();
        for (int index : candidateIndexes) {
            if (index >= 0 && index < states.size() && !states.get(index).completed()) {
                pending.add(index);
            }
        }
        return List.copyOf(pending);
    }

    private static List<AudioSegmentSnapshot> markBatchAttempt(List<AudioSegmentSnapshot> states,
                                                               List<Integer> attemptIndexes,
                                                               int attempt) {
        ArrayList<AudioSegmentSnapshot> updated = new ArrayList<>(states);
        for (int index : attemptIndexes) {
            AudioSegmentSnapshot current = updated.get(index);
            if (!current.completed()) {
                updated.set(index, new AudioSegmentSnapshot(current.segmentId(), current.title(),
                        AudioSegmentStatus.GENERATING, current.audioRelativePath(), current.durationSeconds(),
                        attempt, ""));
            }
        }
        return List.copyOf(updated);
    }

    private static int failedSegmentCount(List<AudioSegmentSnapshot> states) {
        if (states == null || states.isEmpty()) {
            return 0;
        }
        return (int) states.stream()
                .filter(segment -> segment.status() == AudioSegmentStatus.FAILED)
                .count();
    }

    private static int generationUnitIndex(List<AudioGenerationUnit> generationUnits, String segmentId) {
        String target = segmentId == null ? "" : segmentId.strip();
        for (int i = 0; i < generationUnits.size(); i++) {
            if (generationUnits.get(i).id().equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private static String valueAfter(String text, String marker) {
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        return text.substring(start + marker.length()).strip();
    }

    private static String flagValue(List<String> tokens, String flag) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (flag.equalsIgnoreCase(tokens.get(i))) {
                return tokens.get(i + 1);
            }
        }
        return "";
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private record XttsBatchRuntime(Path script, Path python, Path wrapper, Path modelDir, Path defaultSpeakerWav) {
        static Optional<XttsBatchRuntime> from(LocalTtsProcessConfiguration configuration) {
            String template = configuration == null ? "" : configuration.commandTemplate();
            String lower = template.toLowerCase(java.util.Locale.ROOT);
            if (!lower.contains("xtts-file-to-wav") || !lower.contains("synthesize_xtts.py")) {
                return Optional.empty();
            }
            List<String> tokens = LocalTtsProcessConfiguration.splitCommand(template);
            String scriptToken = flagValue(tokens, "-File");
            String pythonToken = flagValue(tokens, "-Python");
            String wrapperToken = flagValue(tokens, "-Wrapper");
            String modelToken = flagValue(tokens, "-ModelDir");
            String speakerToken = flagValue(tokens, "-SpeakerWav");
            if (scriptToken.isBlank() || pythonToken.isBlank() || wrapperToken.isBlank()
                    || modelToken.isBlank() || speakerToken.isBlank()) {
                return Optional.empty();
            }
            Path batchScript = Path.of(scriptToken).toAbsolutePath().normalize()
                    .resolveSibling("xtts-batch-to-wav.ps1");
            Path batchWrapper = Path.of(wrapperToken).toAbsolutePath().normalize()
                    .resolveSibling("synthesize_xtts_batch.py");
            Path python = Path.of(pythonToken).toAbsolutePath().normalize();
            Path modelDir = Path.of(modelToken).toAbsolutePath().normalize();
            Path speaker = Path.of(speakerToken).toAbsolutePath().normalize();
            if (!Files.isRegularFile(batchScript) || !Files.isRegularFile(batchWrapper)
                    || !Files.isRegularFile(python) || !Files.isDirectory(modelDir)
                    || !Files.isRegularFile(speaker)) {
                return Optional.empty();
            }
            return Optional.of(new XttsBatchRuntime(batchScript, python, batchWrapper, modelDir, speaker));
        }

        List<String> command(Path manifest, String language, LocalTtsProcessConfiguration configuration) {
            return List.of(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    script.toString(),
                    "-Python",
                    python.toString(),
                    "-Wrapper",
                    wrapper.toString(),
                    "-ModelDir",
                    modelDir.toString(),
                    "-Manifest",
                    manifest.toString(),
                    "-Language",
                    language == null || language.isBlank() ? configuration.language() : language,
                    "-Device",
                    com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceArgumentMapper
                            .toProcessDeviceArgument(configuration.computePolicy(), configuration.computeDeviceId())
            );
        }
    }

    private ProcessResult runCommand(String jobId, List<String> command, Path workingDirectory, AtomicBoolean cancellation)
            throws IOException, InterruptedException {
        return runCommand(jobId, command, workingDirectory, cancellation, line -> { });
    }

    private ProcessResult runCommand(String jobId, List<String> command, Path workingDirectory, AtomicBoolean cancellation,
                                     Consumer<String> outputLineConsumer)
            throws IOException, InterruptedException {
        if (command.isEmpty()) {
            return new ProcessResult(-1, "Comando TTS vacío.", 0L, false, false);
        }
        Consumer<String> progressConsumer = outputLineConsumer == null ? line -> { } : outputLineConsumer;
        ExternalProcessRequest request = ExternalProcessRequest.of(command, "tts-" + jobId,
                        Duration.ofSeconds(configuration.timeoutSeconds()))
                .withWorkingDirectory(workingDirectory)
                .withEnvironment(Map.of("PYTHONUNBUFFERED", "1", "PYTHONIOENCODING", "utf-8", "PYTHONUTF8", "1"))
                .redirectingErrorStream();
        ExternalProcessResult result = processRunner.run(request, new ExternalProcessObserver() {
            @Override
            public void onOutputLine(String line) {
                progressConsumer.accept(line);
            }

            @Override
            public boolean cancellationRequested() {
                return cancellation.get();
            }
        });
        String output = result.combinedOutputTail();
        if (result.timedOut()) {
            output = "Timeout TTS después de " + configuration.timeoutSeconds() + " segundos. " + output;
        }
        return new ProcessResult(result.exitCode(), output == null ? "" : output,
                result.duration().toMillis(), result.timedOut(), result.cancelled());
    }

    private static String processProgressMessage(String outputLine) {
        String line = outputLine == null ? "" : outputLine.strip();
        if (line.isBlank() || !line.contains("DOCUPODCAST_XTTS")) {
            return "";
        }
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("importando_paquete_tts")) {
            return "Voz IA avanzada: cargando librerías locales de voz";
        }
        if (lower.contains("se solicitó gpu") && lower.contains("cpu")) {
            return "Voz IA avanzada: GPU no disponible en este entorno Python; generando por CPU";
        }
        if (lower.contains("cargando_modelo")) {
            return "Voz IA avanzada: cargando modelo local; la primera vez puede tardar varios minutos";
        }
        if (lower.contains("asignando_device")) {
            return "Voz IA avanzada: preparando dispositivo de generación";
        }
        if (lower.contains("sintetizando")) {
            return "Voz IA avanzada: sintetizando audio del fragmento";
        }
        if (lower.contains("wav_generado")) {
            return "Voz IA avanzada: WAV generado, verificando audio";
        }
        if (lower.contains("texto_cargado")) {
            return "Voz IA avanzada: texto del fragmento cargado";
        }
        if (lower.contains("inicio")) {
            return "Voz IA avanzada: iniciando proceso local";
        }
        return "";
    }

    private void requireReady() {
        LocalTtsPreflightReport report = configuration.preflightReport();
        if (!report.ready()) {
            throw new IllegalStateException(report.userMessage());
        }
    }

    private AudioJobStatusDto status(String jobId, AudioGenerationRequest request, AudioJobState state, AudioGenerationStage stage,
                                     int completed, int total, int failed, String segmentId, String segmentTitle, long eta,
                                     String message, Path jobDir, String finalAudioPath, String manifestPath) {
        return new AudioJobStatusDto(jobId, request.script().title(), state, stage, completed, total, failed,
                total == 0 ? 0.0 : (double) completed / Math.max(1, total), segmentId, segmentTitle,
                eta, message, jobDir.toString(), finalAudioPath, manifestPath);
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
            // A later diagnostics view will expose persistence errors. Keep generation flow alive.
        }
    }

    private String nextJobId() {
        return "TTS-JOB-" + LocalDateTime.now().format(JOB_TIME) + "-" + String.format(java.util.Locale.ROOT, "%03d", sequence.getAndIncrement());
    }

    private static List<AudioSegmentSnapshot> pendingSegments(AudioGenerationRequest request) {
        return request.generationUnits().stream()
                .map(unit -> AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()))
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
                merged.add(AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()));
            } else if (persisted.completed()) {
                merged.add(persisted);
            } else {
                // Reanudar debe significar "volver a intentar", no conservar intentos agotados.
                // Los segmentos fallidos/cancelados/en generación se reinician como pendientes para que
                // el botón Seguir generando ejecute de verdad el proceso externo otra vez.
                merged.add(AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()));
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

    private long etaSeconds(long startedNanos, int completed, int total) {
        if (completed <= 0 || total <= completed) {
            return 0L;
        }
        double elapsedSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
        double average = elapsedSeconds / completed;
        return Math.max(1L, Math.round(average * (total - completed)));
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
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private void appendDiagnostic(Path projectDirectory, AudioProcessDiagnosticEvent event) {
        try {
            diagnosticsRepository.append(projectDirectory, event.jobId(), event);
        } catch (IOException ignored) {
            // Process diagnostics are best-effort; job persistence remains authoritative.
        }
    }

    private static String commandSummary(List<String> command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        return command.stream()
                .map(token -> token.contains(" ") ? "\"" + token.replace("\"", "'") + "\"" : token)
                .collect(Collectors.joining(" "));
    }

    private static String tail(String output) {
        String normalized = output == null ? "" : output.replace("\r", " ").strip();
        if (normalized.length() <= 1500) {
            return normalized;
        }
        return normalized.substring(normalized.length() - 1500);
    }

    private record ProcessResult(int exitCode, String output, long durationMillis, boolean timedOut, boolean cancelled) {
    }

    private static final class TtsThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "docupodcast-local-tts-worker");
            thread.setDaemon(true);
            return thread;
        }
    }
}
