package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.audio.*;
import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.*;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningLanguage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.InMemoryAudioJobQueue;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.VoiceEngineAudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.reading.FileNarrationTranslationCache;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.settings.PropertiesOperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceBudget;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Real page-scoped gate through production TTS, composition, preflight and renderer. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.currentProjectAudioVideo",
        matches = "true")
final class PdfCurrentProjectAudioVideoPhysicalTest {
    @Test
    void generatesOnlyMissingEnglishAudioAndExportsPageThree() throws Exception {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        Path projectRoot = Path.of(System.getProperty(
                "docupodcast.pdf.currentProjectRoot",
                "D:/Proyectos/Demostracion_limite_notable"));
        Path projectFile = projectRoot.resolve(
                "Demostracion_limite_notable.docupodcast.json");
        int page = Integer.getInteger("docupodcast.pdf.audioVideoPage", 3);
        var pdfRepository = new JsonPreparedPdfDocumentRepository();
        var manifest = pdfRepository.loadManifest(projectRoot).orElseThrow();
        Path source = projectRoot.resolve("source/Demostracion_limite_notable.pdf");
        var workspace = new PreparedPdfWorkspaceRef(projectRoot, source,
                manifest.sourceSha256());
        NarrationScriptDocument canonical =
                new BuildPreparedPdfNarrationUseCase(pdfRepository).build(
                        workspace, manifest.title(), "es", false, null,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        NarrationScriptDocument scoped = page <= 0 ? canonical
                : new FilterPdfNarrationByPageIntervalUseCase()
                .execute(canonical, DocumentProcessingInterval.pages(
                        page, page, manifest.pageCount()));
        var preferences = new DocumentTranslationPreferences(true,
                DocumentListeningLanguage.ENGLISH);
        NarrationScriptDocument effective = new AdaptNarrationLanguageUseCase(
                new MediaCapabilityService(new MediaEnginePlatform(null, null, null),
                        request -> com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease.NONE),
                new FileNarrationTranslationCache())
                .fromCache(scoped, preferences, projectRoot).orElseThrow(() ->
                        new IllegalStateException("English translation cache is incomplete"));
        assertEquals("en", effective.language());

        var settingsRepository = PropertiesOperationalSettingsRepository.defaultRepository();
        var loadSettings = new LoadOperationalSettingsUseCase(settingsRepository);
        var operational = loadSettings.load();
        var project = new DocuPodcastProjectFileRepository().open(projectFile);
        var jobsRepository = new AudioJobFileRepository();
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        try (var platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(repositoryRoot));
             var gateway = new VoiceEngineAudioGenerationGateway(
                     new MediaCapabilityService(platform, scheduler,
                             ComputePreference::automatic),
                     loadSettings, new InMemoryAudioJobQueue(), jobsRepository)) {
            var descriptor = gateway.engineDescriptor();
            assertTrue(descriptor.configured(), descriptor::statusLabel);
            AudioGenerationRequest fullRequest = request(effective, projectRoot,
                    project.voiceLibrary(), operational.tts().voiceProfileId(),
                    descriptor.engineId() + "|" + descriptor.mode(), repositoryRoot);
            List<AudioJobSnapshot> beforeJobs = jobsRepository.list(projectRoot);
            var coverage = new ReusableAudioCoverage().resolve(
                    fullRequest.generationUnits(), beforeJobs, projectRoot);
            int reused = coverage.readyAudio().size();
            int generated = coverage.missingOrStale().size();
            if (generated > 0) {
                Set<String> missingSegments = coverage.missingOrStale().stream()
                        .map(entry -> entry.unit().sourceSegmentId()).collect(
                                java.util.stream.Collectors.toSet());
                NarrationScriptDocument missing = subset(effective, missingSegments);
                AudioGenerationRequest missingRequest = request(missing, projectRoot,
                        project.voiceLibrary(), operational.tts().voiceProfileId(),
                        descriptor.engineId() + "|" + descriptor.mode(), repositoryRoot);
                CountDownLatch terminal = new CountDownLatch(1);
                AtomicReference<AudioJobState> state = new AtomicReference<>();
                AtomicReference<String> detail = new AtomicReference<>("");
                gateway.submit(missingRequest, status -> {
                    System.out.printf("stage=AUDIO state=%s completed=%d/%d detail=%s%n",
                            status.state(), status.completedSegments(),
                            status.totalSegments(), status.statusLine());
                    state.set(status.state());
                    detail.set(status.statusLine());
                    if (!status.running()) terminal.countDown();
                });
                assertTrue(terminal.await(45, TimeUnit.MINUTES),
                        "TTS did not reach a terminal state");
                assertEquals(AudioJobState.COMPLETED, state.get(), detail::get);
            }

            List<AudioJobSnapshot> jobs = jobsRepository.list(projectRoot);
            var assembled = new AudioCoverageSnapshotAssembler().assemble(
                    fullRequest.generationUnits(), jobs, projectRoot,
                    "PHYSICAL-GATE-P3-AUDIO-VIDEO");
            assertTrue(assembled.readyForPreflight(),
                    () -> assembled.compositionFailures().toString());
            DocumentContentProjection projection =
                    new BuildDocumentContentProjectionUseCase(
                            new OpenPreparedPdfWorkspaceUseCase(pdfRepository)).build(
                            new PreparedPdfSource(workspace, manifest.title()), effective);
            var materializer = new MaterializePdfDocumentContentAssetUseCase(
                    new CapturePdfVisualRegionUseCase(new PdfBoxRenderEngine()));
            var options = DocumentTextVideoOptions.defaults().withResolution(
                    SimpleVideoResolutionPreset.LOW_540);
            SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                    new DocumentStudyVideoContentResolver(),
                    new DocumentStudySlideCompositor(), materializer)
                    .build(project, projection, effective, assembled,
                            projectRoot, options);
            var preflight = new DocumentStudyVideoExportPreflight()
                    .inspect(plan, projectRoot, effective);
            assertTrue(preflight.ready(), () -> "missing=" + preflight.missingFrames()
                    + " duration=" + preflight.durationMismatches()
                    + " bindings=" + preflight.invalidVisualBindings());
            Path output = projectRoot.resolve(
                    "exports/Demostracion_limite_notable-"
                            + (page <= 0 ? "full" : "page-" + page)
                            + "-English.mp4");
            var exportSettings = new SimpleVideoExportSettings(
                    SimpleVideoResolutionPreset.LOW_540, 24, 0.15,
                    true, true, operational.compute().policy(),
                    operational.compute().selectedDeviceId(),
                    operational.compute().videoEncoderPolicy());
            var exporter = new ExportDocumentStudyVideoUseCase(
                    new RenderFinalVideoPlanUseCase(
                            new MediaCapabilityService(platform, scheduler,
                                    ComputePreference::automatic)));
            var exportRequest = new FinalVideoExportRequest(project, null, effective,
                    null, assembled.exportJobs(), projectRoot, output,
                    exportSettings, repositoryRoot,
                    repositoryRoot.resolve("tools/ffmpeg/bin/ffmpeg.exe"));
            long renderStarted = System.nanoTime();
            exporter.export(exportRequest, plan,
                    progress -> System.out.printf(
                            "stage=FFMPEG ratio=%.3f step=%s%n",
                            progress.ratio(), progress.currentStep()), () -> false);
            assertTrue(Files.isRegularFile(output));
            assertTrue(Files.size(output) > 10_000L);
            Path report = Path.of("target/pdf-current-audio-video-gate.txt");
            Files.writeString(report,
                    "page=" + page + "\nsegments=" + effective.segments().size()
                            + "\naudioReused=" + reused
                            + "\naudioGenerated=" + generated
                            + "\ncomposed=" + assembled.composedSegments()
                            + "\ncompositionReused=" + assembled.reusedSegments()
                            + "\nframes=" + plan.frameCount()
                            + "\ndurationSeconds=" + plan.totalDurationSeconds()
                            + "\nmissingImages=" + plan.framesMissingImage()
                            + "\nmissingAudio=" + plan.framesMissingAudio()
                            + "\noutput=" + output
                            + "\nbytes=" + Files.size(output)
                            + "\nrenderMs=" + Duration.ofNanos(
                            System.nanoTime() - renderStarted).toMillis() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private static AudioGenerationRequest request(
            NarrationScriptDocument script, Path projectRoot,
            com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary voices,
            String voiceProfileId, String runtimeId, Path applicationRoot) {
        return new AudioGenerationRequest(script, null, projectRoot,
                "Physical English page 3", script.language(), voiceProfileId,
                voices, new VoiceReferenceSamplePathResolver(
                applicationRoot, applicationRoot), runtimeId);
    }

    private static NarrationScriptDocument subset(
            NarrationScriptDocument source, Set<String> segmentIds) {
        return new NarrationScriptDocument(source.id(), source.title(), source.language(),
                source.sourceDocumentTitle(), source.segments().stream()
                .filter(segment -> segmentIds.contains(segment.id())).toList(),
                source.createdAt(), source.updatedAt(), source.notes());
    }
}
