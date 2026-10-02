package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceEngineAudioGenerationGatewayProgressTest {
    @TempDir Path tempDirectory;

    @Test void piperUsesNativeVoiceWithoutChangingCustomAssignments() throws Exception {
        CapturingVoiceEngine engine = new CapturingVoiceEngine();
        var library = com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary.defaults();
        String customVoice = com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;
        var request = new AudioGenerationRequest(request().script(), null, tempDirectory,
                "Piper con voz personalizada", "es", customVoice, library);
        assertTrue(request.requiresReferenceSampleFor(request.generationUnits().getFirst()));
        CountDownLatch completed = new CountDownLatch(1);
        try (var gateway = gateway(engine, new AudioJobFileRepository())) {
            gateway.submit(request, status -> {
                if (status.state() == AudioJobState.COMPLETED) completed.countDown();
            });
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(2, engine.receivedUnits.size());
            for (var unit : engine.receivedUnits) {
                assertEquals("VOC-NARRATOR", unit.voiceId());
                assertNull(unit.referenceAudio());
                assertEquals("", unit.styleId());
                assertFalse(unit.metadata().containsKey("referenceTranscript"));
            }
            assertEquals(customVoice, request.voiceProfileId());
            assertSame(library, request.voiceLibrary());
        }
    }

    @Test void acousticFailurePreservesOtherUnitsAndDoesNotProducePartialFinalAudio() throws Exception {
        CapturingVoiceEngine engine = new CapturingVoiceEngine();
        engine.rejectFirst = true;
        AudioJobFileRepository repository = new AudioJobFileRepository();
        CountDownLatch failed = new CountDownLatch(1);
        try (var gateway = gateway(engine, repository)) {
            String job = gateway.submit(request(), status -> {
                if (status.state() == AudioJobState.FAILED) failed.countDown();
            });
            assertTrue(failed.await(5, TimeUnit.SECONDS));
            var snapshot = repository.load(tempDirectory, job).orElseThrow();
            assertEquals(1, snapshot.completedSegments());
            assertFalse(snapshot.segments().getFirst().completed());
            assertTrue(snapshot.segments().get(1).completed());
            assertFalse(Files.exists(tempDirectory.resolve("jobs").resolve(job).resolve("final/podcast.wav")));
        }
    }

    @Test
    void reportsWaitingForResourcesBeforeAnySegmentStartsGenerating()
            throws Exception {
        CapturingVoiceEngine engine = new CapturingVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.CPU_HEAVY, 1));
        ResourceLease holder = scheduler.acquire(new ComputeAdmissionRequest(
                "holder", "holder", ComputeJobPriority.BACKGROUND,
                ComputeWorkloadKind.OTHER,
                ComputeResourceDemand.of(ResourceId.CPU_HEAVY),
                CancellationToken.NONE));
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        try (VoiceEngineAudioGenerationGateway gateway = gateway(
                engine, repository, scheduler)) {
            String jobId = gateway.submit(request(), status -> {
                if (status.stage() == com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage.WAITING_FOR_RESOURCES) {
                    waiting.countDown();
                }
                if (status.state() == AudioJobState.COMPLETED) completed.countDown();
            });
            assertTrue(waiting.await(2, TimeUnit.SECONDS));
            AudioJobSnapshot queued = repository.load(tempDirectory, jobId)
                    .orElseThrow();
            assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage.WAITING_FOR_RESOURCES,
                    queued.stage());
            assertTrue(queued.segments().stream().allMatch(segment ->
                    segment.status() == com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus.PENDING));
            holder.close();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
        } finally {
            holder.close();
        }
    }

    @Test
    void publishesEachClosedWavBeforeTheBatchFinishes() throws Exception {
        BlockingBatchVoiceEngine engine = new BlockingBatchVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        CountDownLatch firstPublished = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);

        try (VoiceEngineAudioGenerationGateway gateway = gateway(engine, repository)) {
            String jobId = gateway.submit(request(), status -> {
                if (status.completedSegments() >= 1) firstPublished.countDown();
                if (status.state() == AudioJobState.COMPLETED) completed.countDown();
            });

            assertTrue(firstPublished.await(5, TimeUnit.SECONDS),
                    "El primer WAV debe publicarse mientras el lote continúa ejecutándose.");
            AudioJobSnapshot running = repository.load(tempDirectory, jobId).orElseThrow();
            assertEquals(1, running.completedSegments());
            assertTrue(running.segments().getFirst().completed());
            assertTrue(Files.isRegularFile(
                    tempDirectory.resolve(running.segments().getFirst().audioRelativePath())));

            engine.allowSecondUnit.countDown();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            AudioJobSnapshot finished = repository.load(tempDirectory, jobId).orElseThrow();
            assertEquals(2, finished.completedSegments());
        }
    }

    @Test
    void keepsSpanishAndMathematicalTextForAnEngineWithConservativePolicy() {
        String prepared = VoiceEngineAudioGenerationGateway.prepareTextForEngine(
                "La expresión a i ≥ 0", null);

        assertEquals("La expresión a i ≥ 0", prepared);
    }

    @Test
    void sendsPreprocessedTextToTheSelectedPiperEngine() throws Exception {
        CapturingVoiceEngine engine = new CapturingVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        CountDownLatch completed = new CountDownLatch(1);

        try (VoiceEngineAudioGenerationGateway gateway = gateway(engine, repository)) {
            gateway.submit(requestWithText("# Canción\nhttps://example.test"), status -> {
                if (status.state() == AudioJobState.COMPLETED) completed.countDown();
            });

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("Canción"), engine.receivedTexts,
                    "El gateway aplica normalización común sin inventar una política por nombre.");
        }
    }

    @Test
    void cancellationKeepsAlreadyPublishedChunksPlayable() throws Exception {
        BlockingBatchVoiceEngine engine = new BlockingBatchVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        CountDownLatch firstPublished = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);

        try (VoiceEngineAudioGenerationGateway gateway = gateway(engine, repository)) {
            String jobId = gateway.submit(request(), status -> {
                if (status.completedSegments() >= 1) firstPublished.countDown();
                if (status.state() == AudioJobState.CANCELLED) cancelled.countDown();
            });
            assertTrue(firstPublished.await(5, TimeUnit.SECONDS));
            assertTrue(gateway.cancel(jobId));
            engine.allowSecondUnit.countDown();
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));

            AudioJobSnapshot snapshot = repository.load(tempDirectory, jobId).orElseThrow();
            assertEquals(1, snapshot.completedSegments());
            assertTrue(snapshot.segments().getFirst().completed());
            assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus.CANCELLED,
                    snapshot.segments().get(1).status());
        }
    }

    @Test
    void destructiveMaintenanceWaitsUntilTheRealVoiceWorkerReleasesTheWorkspace()
            throws Exception {
        BlockingBatchVoiceEngine engine = new BlockingBatchVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        CountDownLatch firstPublished = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);

        try (VoiceEngineAudioGenerationGateway gateway = gateway(engine, repository)) {
            String jobId = gateway.submit(request(), status -> {
                if (status.completedSegments() >= 1) firstPublished.countDown();
                if (status.state() == AudioJobState.CANCELLED) cancelled.countDown();
            });
            assertTrue(firstPublished.await(5, TimeUnit.SECONDS));

            CompletableFuture<Void> maintenance = CompletableFuture.runAsync(() -> {
                try {
                    repository.deleteAll(tempDirectory);
                } catch (IOException failure) {
                    throw new java.util.concurrent.CompletionException(failure);
                }
            });
            assertThrows(TimeoutException.class,
                    () -> maintenance.get(200, TimeUnit.MILLISECONDS),
                    "La limpieza no debe entrar mientras el motor conserva archivos abiertos.");

            assertTrue(gateway.cancel(jobId));
            engine.allowSecondUnit.countDown();
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
            maintenance.get(5, TimeUnit.SECONDS);
            assertFalse(Files.exists(tempDirectory.resolve("jobs")));
        }
    }

    @Test
    void resumeReconcilesValidWavsLeftByAnInterruptedLegacyBatch() throws Exception {
        ImmediateVoiceEngine engine = new ImmediateVoiceEngine();
        AudioJobFileRepository repository = new AudioJobFileRepository();
        AudioGenerationRequest request = request();
        List<AudioGenerationUnit> units = request.generationUnits();
        String jobId = "JOB-RECOVER-001";
        Path recoveredWav = tempDirectory.resolve("jobs").resolve(jobId)
                .resolve("audio").resolve("SEG-001.wav");
        writeTestWav(recoveredWav);
        List<com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot> cancelled =
                units.stream().map(unit ->
                        com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot
                                .pending(unit.id(), unit.effectiveTitle(), unit.sourceFingerprint())
                                .cancelled()).toList();
        AudioJobSnapshot interrupted = new AudioJobSnapshot(
                jobId, "Interrumpido", AudioJobState.CANCELLED,
                com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage.CANCELLED,
                0, cancelled.size(), 0, 0, "", "", 0, "Cancelado",
                "jobs/" + jobId, "", "", cancelled, Instant.now(), Instant.now());
        CountDownLatch completed = new CountDownLatch(1);

        try (VoiceEngineAudioGenerationGateway gateway = gateway(engine, repository)) {
            gateway.resume(request, interrupted, status -> {
                if (status.state() == AudioJobState.COMPLETED) completed.countDown();
            });
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            AudioJobSnapshot finished = repository.load(tempDirectory, jobId).orElseThrow();
            assertEquals(2, finished.completedSegments());
            assertEquals(List.of("SEG-002"), engine.generatedIds,
                    "El WAV válido de SEG-001 debe recuperarse sin volver a sintetizarlo.");
        }
    }

    private VoiceEngineAudioGenerationGateway gateway(VoiceSynthesisEngine engine,
                                                       AudioJobFileRepository repository) {
        return gateway(engine, repository, LocalResourceScheduler.safeDefaults());
    }

    private VoiceEngineAudioGenerationGateway gateway(VoiceSynthesisEngine engine,
                                                       AudioJobFileRepository repository,
                                                       ResourceScheduler scheduler) {
        EngineRegistry<VoiceSynthesisEngine> voices = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        voices.register(engine);
        MediaCapabilityService media = new MediaCapabilityService(
                new MediaEnginePlatform(voices, null, null), scheduler);
        LoadOperationalSettingsUseCase settings = new LoadOperationalSettingsUseCase(
                new FixedSettingsRepository());
        return new VoiceEngineAudioGenerationGateway(
                media, settings, new InMemoryAudioJobQueue(), repository);
    }

    private AudioGenerationRequest request() {
        List<NarrationSegment> segments = List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH,
                        "Primero", "Primer fragmento.", List.of("B0001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH,
                        "Segundo", "Segundo fragmento.", List.of("B0002")));
        return new AudioGenerationRequest(
                NarrationScriptDocument.create("Prueba", "es", "Documento", segments),
                tempDirectory, "Progreso incremental");
    }

    private AudioGenerationRequest requestWithText(String text) {
        NarrationSegment segment = NarrationSegment.of(
                "SEG-001", NarrationSegmentType.PARAGRAPH,
                "Fragmento", text, List.of("B0001"));
        return new AudioGenerationRequest(
                NarrationScriptDocument.create("Prueba", "es", "Documento", List.of(segment)),
                tempDirectory, "Preprocesamiento Piper");
    }

    private static final class FixedSettingsRepository implements OperationalSettingsRepository {
        @Override public OperationalSettings load() {
            return OperationalSettings.defaults();
        }

        @Override public void save(OperationalSettings settings) {
        }
    }

    private static final class BlockingBatchVoiceEngine implements VoiceSynthesisEngine {
        private static final EngineId ID = new EngineId("piper");
        private final CountDownLatch allowSecondUnit = new CountDownLatch(1);

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VOICE_SYNTHESIS, "Voz de prueba",
                    "1", "test", Set.of(EngineFeature.BATCH), true);
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }

        @Override public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request,
                                                         ExecutionContext context) throws IOException {
            writeWav(request.outputFile());
            return new VoiceSynthesisResult(request.outputFile(), 0.1, Map.of());
        }

        @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                                   ExecutionContext context)
                throws IOException, InterruptedException {
            ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
            VoiceSynthesisUnit first = request.units().getFirst();
            writeWav(first.outputFile());
            results.add(new VoiceSynthesisResult(first.outputFile(), 0.1, Map.of()));
            context.progress().report("voice-unit", 0.5, first.id());
            assertTrue(allowSecondUnit.await(5, TimeUnit.SECONDS));
            context.cancellation().throwIfCancellationRequested();
            VoiceSynthesisUnit second = request.units().get(1);
            writeWav(second.outputFile());
            results.add(new VoiceSynthesisResult(second.outputFile(), 0.1, Map.of()));
            context.progress().report("voice-unit", 1.0, second.id());
            return new VoiceSynthesisBatchResult(results, Map.of("mode", "test"));
        }

        private static void writeWav(Path output) throws IOException {
            writeTestWav(output);
        }
    }

    private static final class ImmediateVoiceEngine implements VoiceSynthesisEngine {
        private static final EngineId ID = new EngineId("piper");
        private final List<String> generatedIds = new ArrayList<>();

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VOICE_SYNTHESIS, "Voz inmediata",
                    "1", "test", Set.of(EngineFeature.BATCH), true);
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }

        @Override public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request,
                                                         ExecutionContext context) throws IOException {
            writeTestWav(request.outputFile());
            return new VoiceSynthesisResult(request.outputFile(), 0.1, Map.of());
        }

        @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                                   ExecutionContext context)
                throws IOException, InterruptedException {
            ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
            for (VoiceSynthesisUnit unit : request.units()) {
                context.cancellation().throwIfCancellationRequested();
                generatedIds.add(unit.id());
                writeTestWav(unit.outputFile());
                results.add(new VoiceSynthesisResult(unit.outputFile(), 0.1, Map.of()));
                context.progress().report("voice-unit",
                        results.size() / (double) request.units().size(), unit.id());
            }
            return new VoiceSynthesisBatchResult(results, Map.of("mode", "test"));
        }
    }

    private static final class CapturingVoiceEngine implements VoiceSynthesisEngine {
        boolean rejectFirst;
        private static final EngineId ID = new EngineId("piper");
        private final List<String> receivedTexts = new ArrayList<>();
        private final List<VoiceSynthesisUnit> receivedUnits = new ArrayList<>();

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VOICE_SYNTHESIS, "Voz local simple",
                    "1", "test", Set.of(EngineFeature.BATCH), true);
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }

        @Override public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request,
                                                         ExecutionContext context) throws IOException {
            receivedTexts.add(request.text());
            writeTestWav(request.outputFile());
            return new VoiceSynthesisResult(request.outputFile(), 0.1, Map.of());
        }

        @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                                   ExecutionContext context) throws IOException {
            ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
            for (VoiceSynthesisUnit unit : request.units()) {
                receivedTexts.add(unit.text());
                receivedUnits.add(unit);
                if (rejectFirst && results.isEmpty()) {
                    results.add(new VoiceSynthesisResult(unit.outputFile(), 0, Map.of(
                            "qualityStatus", "REVIEW_REQUIRED", "qualityReason", "Señal sospechosa")));
                    continue;
                }
                writeTestWav(unit.outputFile());
                results.add(new VoiceSynthesisResult(unit.outputFile(), 0.1, Map.of()));
                context.progress().report("voice-unit",
                        results.size() / (double) request.units().size(), unit.id());
            }
            return new VoiceSynthesisBatchResult(results, Map.of("mode", "capture"));
        }
    }

    private static void writeTestWav(Path output) throws IOException {
        Files.createDirectories(output.toAbsolutePath().normalize().getParent());
        AudioFormat format = new AudioFormat(16_000, 16, 1, true, false);
        byte[] samples = new byte[3_200];
        try (AudioInputStream input = new AudioInputStream(
                new ByteArrayInputStream(samples), format, samples.length / format.getFrameSize())) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output.toFile());
        }
    }
}
