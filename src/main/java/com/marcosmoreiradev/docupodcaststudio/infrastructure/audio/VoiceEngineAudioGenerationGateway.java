package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Compatibility facade for existing audio UI, executed exclusively through neutral voice engines. */
public final class VoiceEngineAudioGenerationGateway implements AudioGenerationGateway, AutoCloseable {
    private static final DateTimeFormatter JOB_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final MediaCapabilityService media;
    private final LoadOperationalSettingsUseCase loadSettings;
    private final InMemoryAudioJobQueue queue;
    private final AudioJobRepository repository;
    private final WavAudioDurationProbe durationProbe = new WavAudioDurationProbe();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable ->
            Thread.ofPlatform().name("voice-generation-jobs").daemon(true).unstarted(runnable));
    private final Map<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    public VoiceEngineAudioGenerationGateway(MediaCapabilityService media,
                                             LoadOperationalSettingsUseCase loadSettings,
                                             InMemoryAudioJobQueue queue,
                                             AudioJobRepository repository) {
        this.media = Objects.requireNonNull(media, "media capabilities");
        this.loadSettings = Objects.requireNonNull(loadSettings, "load settings");
        this.queue = Objects.requireNonNull(queue, "audio queue");
        this.repository = Objects.requireNonNull(repository, "audio job repository");
    }

    @Override public AudioEngineDescriptor engineDescriptor() {
        try {
            OperationalSettings settings = loadSettings.load();
            EngineId selected = SelectedMediaEngines.from(settings).voice();
            VoiceSynthesisEngine engine = media.platform().voiceEngines().require(selected);
            EngineReadiness readiness = engine.inspectReadiness(null);
            EngineDescriptor descriptor = engine.descriptor();
            return new AudioEngineDescriptor(descriptor.id().value(), descriptor.displayName(),
                    descriptor.runtimeKind(), readiness.ready(), true, "", readiness.summary());
        } catch (Exception failure) {
            return AudioEngineDescriptor.unavailable("voice", "Motor de voz", rootMessage(failure));
        }
    }

    @Override public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        Objects.requireNonNull(request, "request");
        String jobId = "JOB-" + LocalDateTime.now().format(JOB_TIME) + "-" + String.format("%03d", sequence.getAndIncrement());
        List<AudioSegmentSnapshot> segments = request.generationUnits().stream()
                .map(unit -> AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle())).toList();
        schedule(jobId, request, segments, Instant.now(), statusConsumer);
        return jobId;
    }

    @Override public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot,
                                   Consumer<AudioJobStatusDto> statusConsumer) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(snapshot, "snapshot");
        schedule(snapshot.jobId(), request, mergeResumeSegments(request, snapshot), snapshot.createdAt(), statusConsumer);
        return snapshot.jobId();
    }

    @Override public boolean cancel(String jobId) {
        AtomicBoolean cancellation = cancellations.get(jobId == null ? "" : jobId.strip());
        if (cancellation == null) return false;
        cancellation.set(true);
        return true;
    }

    @Override public List<AudioJobStatusDto> listStatuses() { return queue.list(); }

    private void schedule(String jobId, AudioGenerationRequest request, List<AudioSegmentSnapshot> segments,
                          Instant createdAt, Consumer<AudioJobStatusDto> statusConsumer) {
        Consumer<AudioJobStatusDto> consumer = statusConsumer == null ? ignored -> { } : statusConsumer;
        AtomicBoolean cancellation = new AtomicBoolean();
        if (cancellations.putIfAbsent(jobId, cancellation) != null) throw new IllegalStateException("audio job already running: " + jobId);
        Path jobDirectory = request.projectDirectory().resolve("jobs").resolve(jobId).normalize();
        publish(status(jobId, request, AudioJobState.QUEUED, AudioGenerationStage.PREPARING_WORKSPACE,
                segments, "Trabajo de voz en cola.", jobDirectory, "", ""), request, segments, createdAt, consumer);
        worker.submit(() -> run(jobId, request, jobDirectory, segments, createdAt, cancellation, consumer));
    }

    private void run(String jobId, AudioGenerationRequest request, Path jobDirectory,
                     List<AudioSegmentSnapshot> initial, Instant createdAt, AtomicBoolean cancellation,
                     Consumer<AudioJobStatusDto> consumer) {
        List<AudioSegmentSnapshot> segments = new ArrayList<>(initial);
        try {
            Path audioDirectory = jobDirectory.resolve("audio");
            Path finalDirectory = jobDirectory.resolve("final");
            Files.createDirectories(audioDirectory);
            Files.createDirectories(finalDirectory);
            OperationalSettings settings = loadSettings.load();
            EngineId selected = SelectedMediaEngines.from(settings).voice();
            ArrayList<VoiceSynthesisUnit> units = new ArrayList<>();
            for (AudioGenerationUnit unit : request.generationUnits()) {
                int index = indexOf(segments, unit.id());
                if (index >= 0 && segments.get(index).completed()
                        && Files.isRegularFile(request.projectDirectory().resolve(segments.get(index).audioRelativePath()))) continue;
                Path target = audioDirectory.resolve(safe(unit.id()) + ".wav");
                units.add(new VoiceSynthesisUnit(unit.id(), unit.text(), unit.effectiveVoiceProfileId(request.voiceProfileId()),
                        unit.performanceStyleId(), request.referenceSamplePathFor(unit).orElse(null), target,
                        Map.of("sourceSegmentId", unit.sourceSegmentId())));
                if (index >= 0) segments.set(index, segments.get(index).generating());
            }
            publish(status(jobId, request, AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                    segments, "Generando unidades de voz.", jobDirectory, "", ""), request, segments, createdAt, consumer);
            if (!units.isEmpty()) {
                ExecutionPolicy policy = new ExecutionPolicy(Duration.ofSeconds(settings.tts().timeoutSeconds()),
                        settings.tts().maxRetries() + 1);
                ExecutionContext context = new ExecutionContext(jobId, cancellation::get,
                        (stage, amount, message) -> publish(status(jobId, request, AudioJobState.GENERATING_AUDIO,
                                AudioGenerationStage.GENERATING_SEGMENTS, segments, message, jobDirectory, "", ""),
                                request, segments, createdAt, consumer), policy, ResourceLease.NONE);
                VoiceSynthesisBatchResult generated = media.synthesize(selected,
                        new VoiceSynthesisBatchRequest(units, request.language(), Map.of()), context);
                if (generated.units().size() != units.size()) throw new IOException("El motor devolvió un lote de voz incompleto.");
                for (int index = 0; index < units.size(); index++) {
                    VoiceSynthesisUnit unit = units.get(index);
                    VoiceSynthesisResult result = generated.units().get(index);
                    if (!Files.isRegularFile(result.audioFile())) throw new IOException("Falta WAV de " + unit.id());
                    double duration = result.durationSeconds() > 0 ? result.durationSeconds()
                            : durationProbe.durationSeconds(result.audioFile());
                    int stateIndex = indexOf(segments, unit.id());
                    segments.set(stateIndex, segments.get(stateIndex).completed(
                            portable(request.projectDirectory(), result.audioFile()), duration));
                    publish(status(jobId, request, AudioJobState.GENERATING_AUDIO,
                            AudioGenerationStage.GENERATING_SEGMENTS, segments, "Voz generada: " + unit.id(),
                            jobDirectory, "", ""), request, segments, createdAt, consumer);
                }
            }
            if (cancellation.get()) throw new InterruptedException("audio generation cancelled");
            Path finalAudio = finalDirectory.resolve("podcast.wav");
            publish(status(jobId, request, AudioJobState.MERGING_AUDIO, AudioGenerationStage.MERGING_SEGMENTS,
                    segments, "Uniendo WAVs.", jobDirectory, "", ""), request, segments, createdAt, consumer);
            mergeWav(segments.stream().map(segment -> request.projectDirectory().resolve(segment.audioRelativePath())).toList(),
                    finalAudio);
            Path manifest = jobDirectory.resolve("audio-manifest.json");
            Files.writeString(manifest, manifest(jobId, selected, segments), StandardCharsets.UTF_8);
            publish(status(jobId, request, AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                    segments, "Audio listo.", jobDirectory, portable(request.projectDirectory(), finalAudio),
                    portable(request.projectDirectory(), manifest)), request, segments, createdAt, consumer);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            cancelPending(segments);
            publish(status(jobId, request, AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED, segments,
                    "Generación cancelada.", jobDirectory, "", ""), request, segments, createdAt, consumer);
        } catch (Exception failure) {
            failPending(segments, rootMessage(failure));
            publish(status(jobId, request, AudioJobState.FAILED, AudioGenerationStage.FAILED, segments,
                    rootMessage(failure), jobDirectory, "", ""), request, segments, createdAt, consumer);
        } finally {
            cancellations.remove(jobId, cancellation);
        }
    }

    private void publish(AudioJobStatusDto status, AudioGenerationRequest request, List<AudioSegmentSnapshot> segments,
                         Instant createdAt, Consumer<AudioJobStatusDto> consumer) {
        queue.update(status);
        consumer.accept(status);
        try {
            repository.save(request.projectDirectory(), AudioJobSnapshotMapper.fromStatus(status,
                    portable(request.projectDirectory(), Path.of(status.outputDirectory())), List.copyOf(segments), createdAt));
        } catch (Exception ignored) { }
    }

    private static AudioJobStatusDto status(String jobId, AudioGenerationRequest request, AudioJobState state,
                                            AudioGenerationStage stage, List<AudioSegmentSnapshot> segments,
                                            String message, Path jobDirectory, String finalAudio, String manifest) {
        int completed = (int) segments.stream().filter(AudioSegmentSnapshot::completed).count();
        int failed = (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.FAILED).count();
        AudioSegmentSnapshot current = segments.stream()
                .filter(segment -> segment.status() == AudioSegmentStatus.GENERATING).findFirst().orElse(null);
        return new AudioJobStatusDto(jobId, request.jobName(), state, stage, completed, segments.size(), failed,
                segments.isEmpty() ? 0 : completed / (double) segments.size(),
                current == null ? "" : current.segmentId(), current == null ? "" : current.title(), 0,
                message, jobDirectory.toString(), finalAudio, manifest);
    }

    private static List<AudioSegmentSnapshot> mergeResumeSegments(AudioGenerationRequest request, AudioJobSnapshot snapshot) {
        Map<String, AudioSegmentSnapshot> previous = snapshot.segments().stream()
                .collect(java.util.stream.Collectors.toMap(AudioSegmentSnapshot::segmentId, item -> item, (left, right) -> left));
        return request.generationUnits().stream().map(unit -> previous.getOrDefault(unit.id(),
                AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()))).toList();
    }

    private static void mergeWav(List<Path> inputs, Path output) throws Exception {
        if (inputs.isEmpty()) throw new IOException("No hay WAVs para unir.");
        Files.createDirectories(output.getParent());
        if (inputs.size() == 1) {
            Files.copy(inputs.getFirst(), output, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        ArrayList<AudioInputStream> streams = new ArrayList<>();
        try {
            for (Path input : inputs) streams.add(AudioSystem.getAudioInputStream(input.toFile()));
            AudioFormat format = streams.getFirst().getFormat();
            for (AudioInputStream stream : streams) if (!format.matches(stream.getFormat())) {
                throw new IOException("Las unidades de voz tienen formatos WAV incompatibles.");
            }
            long frames = streams.stream().mapToLong(AudioInputStream::getFrameLength).sum();
            SequenceInputStream sequence = new SequenceInputStream(Collections.enumeration(streams));
            try (AudioInputStream combined = new AudioInputStream(sequence, format, frames)) {
                AudioSystem.write(combined, AudioFileFormat.Type.WAVE, output.toFile());
            }
        } finally {
            for (AudioInputStream stream : streams) try { stream.close(); } catch (IOException ignored) { }
        }
    }

    private static String manifest(String jobId, EngineId engine, List<AudioSegmentSnapshot> segments) {
        return "{\n  \"version\": 2,\n  \"jobId\": \"" + escape(jobId) + "\",\n  \"engineId\": \""
                + escape(engine.value()) + "\",\n  \"units\": " + segments.size() + "\n}\n";
    }

    private static int indexOf(List<AudioSegmentSnapshot> segments, String id) {
        for (int index = 0; index < segments.size(); index++) if (segments.get(index).segmentId().equals(id)) return index;
        return -1;
    }
    private static void cancelPending(List<AudioSegmentSnapshot> segments) {
        for (int i = 0; i < segments.size(); i++) if (!segments.get(i).completed()) segments.set(i, segments.get(i).cancelled());
    }
    private static void failPending(List<AudioSegmentSnapshot> segments, String message) {
        for (int i = 0; i < segments.size(); i++) if (!segments.get(i).completed()) segments.set(i, segments.get(i).failed(message));
    }
    private static String portable(Path root, Path file) { return root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/'); }
    private static String safe(String value) { String result = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "-"); return result.isBlank() ? "voice-unit" : result; }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String rootMessage(Throwable failure) { Throwable current = failure; while (current != null && current.getCause() != null) current = current.getCause(); return current == null || current.getMessage() == null ? "Error de voz" : current.getMessage(); }

    @Override public void close() { worker.shutdownNow(); }
}
