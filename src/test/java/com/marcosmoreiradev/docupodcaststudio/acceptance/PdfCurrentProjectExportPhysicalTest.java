package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioCoverageSnapshotAssembler;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.*;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceBudget;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Real current-project gate: no Qwen and no TTS invocation. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.currentProjectExport", matches = "true")
final class PdfCurrentProjectExportPhysicalTest {

    @Test
    void reconcilesCurrentChunksIntoTwentyFiveSegmentAssetsAndExportsTwice()
            throws Exception {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        Path projectRoot = Path.of(System.getProperty(
                "docupodcast.pdf.currentProjectRoot",
                "D:/Proyectos/Demostracion_limite_notable"));
        Path source = projectRoot.resolve("source/Demostracion_limite_notable.pdf");
        var pdfRepository = new JsonPreparedPdfDocumentRepository();
        var pdfManifest = pdfRepository.loadManifest(projectRoot).orElseThrow();
        var workspace = new PreparedPdfWorkspaceRef(projectRoot, source,
                pdfManifest.sourceSha256());
        NarrationScriptDocument script = new BuildPreparedPdfNarrationUseCase(pdfRepository)
                .build(workspace, pdfManifest.title(), "es", false, null,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        List<AudioJobSnapshot> jobs = new AudioJobFileRepository().list(projectRoot);
        List<AudioGenerationUnit> units = currentUnits(script, jobs);

        assertFalse(script.segments().isEmpty());
        int expectedSegments = script.segments().size();
        AudioCoverageSnapshotAssembler assembler = new AudioCoverageSnapshotAssembler();
        AudioCoverageSnapshotAssembler.Result export1 = assembler.assemble(
                units, jobs, projectRoot);
        AudioCoverageSnapshotAssembler.Result export2 = assembler.assemble(
                units, jobs, projectRoot);
        AudioCoverageSnapshotAssembler.Result visualOnlyExport = assembler.assemble(
                units, jobs, projectRoot);
        assertTrue(export1.readyForPreflight(), () -> export1.compositionFailures().toString());
        assertTrue(export2.readyForPreflight(), () -> export2.compositionFailures().toString());
        assertEquals(0, export1.coverage().missingOrStale().size());
        assertEquals(0, export2.coverage().missingOrStale().size());
        assertEquals(expectedSegments, export1.exportJobs().getFirst().segments().size());
        assertEquals(0, export2.composedSegments());
        assertEquals(expectedSegments, export2.reusedSegments());
        assertEquals(0, visualOnlyExport.composedSegments());
        assertEquals(expectedSegments, visualOnlyExport.reusedSegments());
        assertEquals(export2.acousticRevision(), visualOnlyExport.acousticRevision());

        DocumentContentProjection projection = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(pdfRepository)).build(
                new PreparedPdfSource(workspace, pdfManifest.title()), script);
        assertTrue(projection.items().stream().anyMatch(item -> item.presentationMode()
                == DocumentPresentationMode.TEXT_RENDER));
        assertTrue(projection.items().stream().anyMatch(item -> item.presentationMode()
                == DocumentPresentationMode.SOURCE_CAPTURE));
        DocuPodcastProject project = DocuPodcastProject.createNew(
                "PDF current physical export", ProjectMode.DOCUMENTARY_STUDIO);
        var materializer = new MaterializePdfDocumentContentAssetUseCase(
                new CapturePdfVisualRegionUseCase(new PdfBoxRenderEngine()));
        SimpleVideoPlan brokenBoundary = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(),
                new DocumentStudySlideCompositor(), materializer)
                .build(project, projection, script, jobs, projectRoot,
                        DocumentTextVideoOptions.defaults().withResolution(
                                SimpleVideoResolutionPreset.QHD_2K));
        assertEquals(expectedSegments, brokenBoundary.framesMissingAudio(),
                "La frontera historica compara fingerprints de chunk con "
                        + "fingerprints parent y reproduce el 15/15 fisico");
        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(),
                new DocumentStudySlideCompositor(), materializer)
                .build(project, projection, script, export2, projectRoot,
                        DocumentTextVideoOptions.defaults().withResolution(
                                SimpleVideoResolutionPreset.QHD_2K));
        assertEquals(expectedSegments,
                plan.frames().stream().filter(frame -> !frame.silentVisual()).count());
        assertEquals(0, plan.framesMissingImage());
        assertEquals(0, plan.framesMissingAudio());
        var preflight = new DocumentStudyVideoExportPreflight().inspect(plan, projectRoot, script);
        assertTrue(preflight.ready());
        assertEquals(0, preflight.framesMissingImage());
        assertEquals(0, preflight.spokenFramesMissingAudio());
        assertEquals(0, plan.frames().stream().filter(frame -> frame.visualBinding() != null)
                .filter(frame -> frame.visualBinding().quality()
                        == NarratedFrameBinding.Quality.WRONG_VISUAL).count());
        SimpleVideoFrame unreliableMixed = plan.frames().stream()
                .filter(frame -> frame.visualBinding() != null)
                .filter(frame -> "PDF-R000003-E8639D93F678066EEA96".equals(
                        frame.visualBinding().sourceBlockId()))
                .findFirst().orElseThrow();
        assertEquals(NarratedFrameBinding.VisualSource.PAGE_FALLBACK,
                unreliableMixed.visualBinding().visualSource());
        assertEquals(NarratedFrameBinding.Quality.COARSE_BUT_VALID,
                unreliableMixed.visualBinding().quality());
        assertEquals(3, unreliableMixed.visualBinding().pageNumber());
        Path bindingManifest = projectRoot.resolve(NarratedFrameBindingManifestWriter.TSV_PATH);
        assertTrue(Files.isRegularFile(bindingManifest));
        String bindingHeader = Files.readAllLines(bindingManifest, StandardCharsets.UTF_8).getFirst();
        assertTrue(bindingHeader.contains("timelineIndex"));
        assertTrue(bindingHeader.contains("sourceBlockIds"));
        assertTrue(bindingHeader.contains("qualityClassification"));
        SimpleVideoPlan diagnosticPlan = new BuildDiagnosticDocumentStudyVideoPlanUseCase()
                .build(plan, projectRoot);
        assertEquals(plan.frameCount(), diagnosticPlan.frameCount());
        assertEquals(plan.totalDurationSeconds(), diagnosticPlan.totalDurationSeconds());

        DocumentTextVideoOptions visualOnlyOptions = new DocumentTextVideoOptions(
                SimpleVideoResolutionPreset.LOW_540,
                DocumentTextVideoBackgroundMode.SOLID_COLOR, "#F7F7F2", "",
                "#1F2937", "#7C3AED", "SansSerif", 58);
        SimpleVideoPlan visualOnlyPlan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(),
                new DocumentStudySlideCompositor(), materializer)
                .build(project, projection, script, visualOnlyExport,
                        projectRoot, visualOnlyOptions);
        var visualOnlyPreflight = new DocumentStudyVideoExportPreflight()
                .inspect(visualOnlyPlan, projectRoot, script);
        assertTrue(visualOnlyPreflight.ready());
        assertEquals(0, visualOnlyPreflight.spokenFramesMissingAudio());
        if (Boolean.getBoolean("docupodcast.pdf.currentProjectFramesOnly")) {
            writePresentationReport(projection, plan, projectRoot);
            return;
        }

        Path output1 = projectRoot.resolve(
                "exports/Demostracion_limite_notable-normal-final.mp4");
        Path output2 = projectRoot.resolve(
                "exports/Demostracion_limite_notable-diagnostic-final.mp4");
        Path output3 = output2;
        var settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.QHD_2K, 30, 0.15,
                true, true, ComputeDevicePolicy.PREFER_GPU, "",
                VideoEncoderPolicy.NVIDIA_NVENC);
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        try (var platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(repositoryRoot))) {
            var media = new MediaCapabilityService(platform, scheduler,
                    ComputePreference::automatic);
            var exporter = new ExportDocumentStudyVideoUseCase(
                    new RenderFinalVideoPlanUseCase(media));
            export(exporter, project, script, export2.exportJobs(), projectRoot,
                    repositoryRoot, output1, settings, plan);
            export(exporter, project, script, export2.exportJobs(), projectRoot,
                    repositoryRoot, output2, settings, diagnosticPlan);
        }
        assertTrue(Files.size(output1) > 10_000L);
        assertTrue(Files.size(output2) > 10_000L);
        assertTrue(Files.size(output3) > 10_000L);
        writeReports(projectRoot, script, units, jobs, export1, export2,
                visualOnlyExport, brokenBoundary, plan, preflight,
                visualOnlyPreflight, output1, output2, output3);
        writePresentationReport(projection, plan, projectRoot);
    }

    private static void writePresentationReport(DocumentContentProjection projection,
                                                SimpleVideoPlan plan,
                                                Path projectRoot) throws Exception {
        Path reportRoot = Path.of("target/pdf-current-physical-audit");
        Files.createDirectories(reportRoot);
        StringBuilder report = new StringBuilder(
                "contentId|kind|presentationMode|page|sourceIds|title|narrationSegmentIds|frameImage\n");
        Map<String, String> frameBySegment = new LinkedHashMap<>();
        plan.frames().forEach(frame -> frameBySegment.putIfAbsent(
                frame.segmentId(), frame.imageRelativePath()));
        for (DocumentContentItem item : projection.items()) {
            String frame = item.narrationSegmentIds().stream()
                    .map(frameBySegment::get).filter(java.util.Objects::nonNull)
                    .findFirst().orElse("");
            report.append(item.contentId()).append('|').append(item.kind()).append('|')
                    .append(item.presentationMode()).append('|')
                    .append(item.pdfAnchor().map(PdfContentAnchor::pageNumber).orElse(0)).append('|')
                    .append(String.join(",", item.sourceIds())).append('|')
                    .append(item.title().replace('|', '/').replace('\r', ' ')
                            .replace('\n', ' ')).append('|')
                    .append(String.join(",", item.narrationSegmentIds())).append('|')
                    .append(frame).append('\n');
        }
        Files.writeString(reportRoot.resolve("presentation-modes.tsv"), report,
                StandardCharsets.UTF_8);
        long text = projection.items().stream().filter(item -> item.presentationMode()
                == DocumentPresentationMode.TEXT_RENDER).count();
        long source = projection.items().stream().filter(item -> item.presentationMode()
                == DocumentPresentationMode.SOURCE_CAPTURE).count();
        Files.writeString(reportRoot.resolve("presentation-summary.txt"),
                "textRender=" + text + "\nsourceCapture=" + source
                        + "\nqwenCalls=0\nocrCalls=0\nttsCalls=0\nproject="
                        + projectRoot + "\n", StandardCharsets.UTF_8);
    }

    private static List<AudioGenerationUnit> currentUnits(
            NarrationScriptDocument script, List<AudioJobSnapshot> jobs) {
        List<NarrationSegment> longestFirst = script.segments().stream()
                .sorted(Comparator.comparingInt((NarrationSegment value) ->
                        value.id().length()).reversed()).toList();
        LinkedHashMap<String, AudioGenerationUnit> result = new LinkedHashMap<>();
        jobs.stream().sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed())
                .flatMap(job -> job.segments().stream()).forEach(snapshot -> {
                    NarrationSegment parent = longestFirst.stream().filter(segment ->
                                    snapshot.segmentId().equals(segment.id())
                                            || snapshot.segmentId().startsWith(segment.id() + "-U"))
                            .findFirst().orElse(null);
                    if (parent == null) return; // Historical jobs from older narration revisions.
                    result.putIfAbsent(snapshot.segmentId(), new AudioGenerationUnit(
                            snapshot.segmentId(), snapshot.title(), parent.narrationText(),
                            parent.id(), parent.voiceProfileId(),
                            parent.performanceStyleId(), List.of(),
                            snapshot.sourceFingerprint()));
                });
        return List.copyOf(result.values());
    }

    private static void export(ExportDocumentStudyVideoUseCase exporter,
                               DocuPodcastProject project,
                               NarrationScriptDocument script,
                               List<AudioJobSnapshot> jobs,
                               Path projectRoot, Path repositoryRoot, Path output,
                               SimpleVideoExportSettings settings,
                               SimpleVideoPlan plan) throws Exception {
        var request = new FinalVideoExportRequest(project, null, script, null,
                jobs, projectRoot, output, settings, repositoryRoot,
                repositoryRoot.resolve("tools/ffmpeg/bin/ffmpeg.exe"));
        var result = exporter.export(request, plan, ignored -> { }, () -> false);
        assertEquals(output, result.targetFile());
    }

    private static void writeReports(Path root, NarrationScriptDocument script,
                                     List<AudioGenerationUnit> units,
                                     List<AudioJobSnapshot> jobs,
                                     AudioCoverageSnapshotAssembler.Result export1,
                                     AudioCoverageSnapshotAssembler.Result export2,
                                     AudioCoverageSnapshotAssembler.Result visualOnlyExport,
                                     SimpleVideoPlan brokenBoundary,
                                     SimpleVideoPlan plan,
                                     DocumentStudyVideoExportPreflight.Report preflight,
                                     DocumentStudyVideoExportPreflight.Report visualOnlyPreflight,
                                     Path output1, Path output2, Path output3) throws Exception {
        Path reportRoot = Path.of("target/pdf-current-physical-audit");
        Files.createDirectories(reportRoot);
        Map<String, String> jobByUnit = new LinkedHashMap<>();
        Map<String, AudioSegmentSnapshot> audioByUnit = new LinkedHashMap<>();
        jobs.forEach(job -> job.segments().forEach(audio -> {
            jobByUnit.putIfAbsent(audio.segmentId(), job.jobId());
            audioByUnit.putIfAbsent(audio.segmentId(), audio);
        }));
        StringBuilder unitReport = new StringBuilder(
                "unitId|segmentId|jobId|status|fingerprint|audioRelativePath|fileExists|fileSize\n");
        for (AudioGenerationUnit unit : units) {
            AudioSegmentSnapshot audio = audioByUnit.get(unit.id());
            Path file = root.resolve(audio.audioRelativePath());
            unitReport.append(unit.id()).append('|').append(unit.sourceSegmentId()).append('|')
                    .append(jobByUnit.get(unit.id())).append('|').append(audio.status()).append('|')
                    .append(audio.sourceFingerprint()).append('|').append(audio.audioRelativePath()).append('|')
                    .append(Files.isRegularFile(file)).append('|')
                    .append(Files.isRegularFile(file) ? Files.size(file) : 0).append('\n');
        }
        Files.writeString(reportRoot.resolve("audio-units.tsv"), unitReport,
                StandardCharsets.UTF_8);

        Map<String, AudioSegmentSnapshot> finalBySegment = new LinkedHashMap<>();
        export2.exportJobs().getFirst().segments().forEach(audio ->
                finalBySegment.put(audio.segmentId(), audio));
        Map<String, String> frameAudio = new LinkedHashMap<>();
        plan.frames().forEach(frame -> frameAudio.put(frame.segmentId(),
                frame.audioRelativePath()));
        String manifest = Files.readString(root.resolve(export2.manifestRelativePath()));
        StringBuilder segmentReport = new StringBuilder(
                "segmentId|units|allCompleted|chunksExisting|finalExists|manifestEntry|coverage|preflightAudioPath|regenerationReason\n");
        for (NarrationSegment segment : script.segments()) {
            List<AudioGenerationUnit> children = units.stream().filter(unit ->
                    unit.sourceSegmentId().equals(segment.id())).toList();
            long existing = children.stream().filter(unit -> {
                AudioSegmentSnapshot audio = audioByUnit.get(unit.id());
                return audio != null && Files.isRegularFile(root.resolve(audio.audioRelativePath()));
            }).count();
            AudioSegmentSnapshot finalAudio = finalBySegment.get(segment.id());
            segmentReport.append(segment.id()).append('|').append(children.size()).append('|')
                    .append(children.stream().allMatch(unit -> audioByUnit.get(unit.id()).completed())).append('|')
                    .append(existing).append('/').append(children.size()).append('|')
                    .append(finalAudio != null && Files.isRegularFile(root.resolve(finalAudio.audioRelativePath()))).append('|')
                    .append(manifest.contains("\"segmentId\": \"" + segment.id() + "\"")).append('|')
                    .append("READY").append('|').append(frameAudio.getOrDefault(segment.id(), "")).append('|')
                    .append("none").append('\n');
        }
        Files.writeString(reportRoot.resolve("audio-segments.tsv"), segmentReport,
                StandardCharsets.UTF_8);
        Files.writeString(reportRoot.resolve("summary.txt"),
                "narrationSegments=" + script.segments().size()
                        + "\naudioGenerationUnits=" + units.size()
                        + "\nunitCoverageMissing=" + export2.coverage().missingOrStale().size()
                        + "\nsegmentAssets=" + export2.exportJobs().getFirst().segments().size()
                        + "\ncomposedExport1=" + export1.composedSegments()
                        + "\nreusedExport1=" + export1.reusedSegments()
                        + "\ncomposedExport2=" + export2.composedSegments()
                        + "\nreusedExport2=" + export2.reusedSegments()
                        + "\ncomposedVisualOnly=" + visualOnlyExport.composedSegments()
                        + "\nreusedVisualOnly=" + visualOnlyExport.reusedSegments()
                        + "\nttsCallsExport1=0\nttsCallsExport2=0\nttsCallsVisualOnly=0"
                        + "\nbeforeReconciliationSpokenFrames="
                        + brokenBoundary.frames().stream()
                        .filter(frame -> !frame.silentVisual()).count()
                        + "\nbeforeReconciliationMissingAudio="
                        + brokenBoundary.framesMissingAudio()
                        + "\nframes=" + plan.frameCount()
                        + "\nframesMissingImage=" + preflight.framesMissingImage()
                        + "\nspokenFramesMissingAudio=" + preflight.spokenFramesMissingAudio()
                        + "\nvisualOnlySpokenFramesMissingAudio="
                        + visualOnlyPreflight.spokenFramesMissingAudio()
                        + "\nmanifest=" + export2.manifestRelativePath()
                        + "\nacousticRevision=" + export2.acousticRevision()
                        + "\nmp4Export1=" + output1 + "\nmp4Export2=" + output2
                        + "\nmp4VisualOnly=" + output3 + "\n",
                StandardCharsets.UTF_8);
    }
}
