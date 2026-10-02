package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.*;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceBudget;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sound.sampled.AudioSystem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real Piper -> PDF frames/crops -> FFmpeg MP4 proof. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.physicalExport", matches = "true")
final class PdfPhysicalExportTest {

    @Test
    void exportsSmallPdfVideoWithRealAudioAndCanonicalVisuals() throws Exception {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        Path canonicalProject = Path.of("D:/Proyectos/Demostracion_limite_notable");
        Path outputRoot = repositoryRoot.resolve("target/pdf-physical-export/project");
        Files.createDirectories(outputRoot);
        Path source = canonicalProject.resolve("source/Demostracion_limite_notable.pdf");
        var repository = new JsonPreparedPdfDocumentRepository();
        var manifest = repository.loadManifest(canonicalProject).orElseThrow();
        var workspace = new PreparedPdfWorkspaceRef(canonicalProject, source,
                manifest.sourceSha256());
        NarrationScriptDocument complete = new BuildPreparedPdfNarrationUseCase(repository)
                .build(workspace, manifest.title(), "es", false, null,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);

        Set<String> desiredTypes = Set.of("IMAGE", "CAPTION", "MATH", "TABLE", "SIDEBAR");
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        complete.segments().stream().map(segment -> segment.metadata().get("sourceBlockType"))
                .filter(desiredTypes::contains).forEach(seen::add);
        var selected = complete.segments();
        assertEquals(desiredTypes, seen);
        NarrationScriptDocument script = new NarrationScriptDocument(
                complete.id(), complete.title(), complete.language(),
                complete.sourceDocumentTitle(), selected, complete.createdAt(),
                Instant.now(), complete.notes());

        DocumentContentProjection completeProjection =
                new BuildDocumentContentProjectionUseCase(
                        new OpenPreparedPdfWorkspaceUseCase(repository)).build(
                        new PreparedPdfSource(workspace, manifest.title()), complete);
        DocumentContentProjection projection = completeProjection;

        List<AudioJobSnapshot> previousJobs = new AudioJobFileRepository().list(canonicalProject);
        StringBuilder previousAudioMatrix = new StringBuilder(
                "segmentId|sourceRegionId|text|audioId|path|exists|binding|reason\n");
        int previousMissing = 0;
        for (var segment : selected) {
            var expected = AudioGenerationUnit.fromSegment(segment).sourceFingerprint();
            var candidates = previousJobs.stream().flatMap(job -> job.segments().stream())
                    .filter(clip -> clip.segmentId().equals(segment.id())
                            || clip.segmentId().startsWith(segment.id() + "-")).toList();
            var reusable = candidates.stream().filter(clip -> {
                Path file = canonicalProject.resolve(clip.audioRelativePath()).normalize();
                return clip.completed() && Files.isRegularFile(file)
                        && (clip.audioRelativePath().endsWith("-manual.wav") || clip.reusableFor(expected));
            }).findFirst();
            if (reusable.isEmpty()) previousMissing++;
            var diagnostic = reusable.orElse(candidates.isEmpty() ? null : candidates.getLast());
            previousAudioMatrix.append(segment.id()).append('|')
                    .append(segment.sourceBlockIds().isEmpty() ? "" : segment.sourceBlockIds().getFirst()).append('|')
                    .append(segment.narrationText().replace('|', '/').replace('\n', ' ')).append('|')
                    .append(diagnostic == null ? "" : diagnostic.segmentId()).append('|')
                    .append(diagnostic == null ? "" : diagnostic.audioRelativePath()).append('|')
                    .append(diagnostic != null && Files.isRegularFile(canonicalProject.resolve(diagnostic.audioRelativePath()))).append('|')
                    .append(reusable.isPresent()).append('|')
                    .append(candidates.isEmpty() ? "audio-never-generated-for-current-segment"
                            : reusable.isPresent() ? "reusable" : "obsolete-fingerprint-or-missing-file")
                    .append('\n');
        }

        Path piper = repositoryRoot.resolve("tools/piper/piper.exe");
        Path model = repositoryRoot.resolve(
                "models/tts/piper/voices/es_ES-default-medium.onnx");
        ArrayList<AudioSegmentSnapshot> clips = new ArrayList<>();
        for (var segment : selected) {
            String relative = "jobs/JOB-PDF-PHYSICAL/audio/" + segment.id() + "-manual.wav";
            Path wav = outputRoot.resolve(relative);
            Files.createDirectories(wav.getParent());
            synthesize(piper, model, segment.narrationText(), wav);
            clips.add(AudioSegmentSnapshot.pending(segment.id(), segment.title(),
                            AudioGenerationUnit.fromSegment(segment).sourceFingerprint())
                    .completed(relative, wavDuration(wav)));
        }
        Instant now = Instant.now();
        AudioJobSnapshot audioJob = new AudioJobSnapshot(
                "JOB-PDF-PHYSICAL", manifest.title(), AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, clips.size(), clips.size(), 0,
                1.0, "", "", 0, "Audio Piper físico listo",
                "jobs/JOB-PDF-PHYSICAL", "", "", clips, now, now);

        DocuPodcastProject project = DocuPodcastProject.createNew(
                "PDF physical export", ProjectMode.DOCUMENTARY_STUDIO);
        var materializer = new MaterializePdfDocumentContentAssetUseCase(
                new CapturePdfVisualRegionUseCase(new PdfBoxRenderEngine()));
        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(),
                new DocumentStudySlideCompositor(), materializer)
                .build(project, projection, script, List.of(audioJob), outputRoot,
                        DocumentTextVideoOptions.defaults().withResolution(
                                SimpleVideoResolutionPreset.LOW_540));
        assertTrue(plan.exportableAsRenderedVideo(), () -> "images="
                + plan.framesMissingImage() + " audio=" + plan.framesMissingAudio());
        var preflight = new DocumentStudyVideoExportPreflight().inspect(plan, outputRoot);
        assertTrue(preflight.ready());
        assertEquals(0, preflight.framesMissingImage());
        assertEquals(0, preflight.spokenFramesMissingAudio());

        Path target = outputRoot.resolve("Demostracion_limite_notable-full-stabilized.mp4");
        var settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.LOW_540, 12, 0.15,
                true, true, ComputeDevicePolicy.CPU_ONLY, "",
                VideoEncoderPolicy.CPU_X264);
        var request = new FinalVideoExportRequest(project, null, script, null,
                List.of(audioJob), outputRoot, target, settings, repositoryRoot,
                repositoryRoot.resolve("tools/ffmpeg/bin/ffmpeg.exe"));
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        try (var platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(repositoryRoot))) {
            var media = new MediaCapabilityService(platform, scheduler,
                    ComputePreference::automatic);
            var result = new ExportDocumentStudyVideoUseCase(
                    new RenderFinalVideoPlanUseCase(media)).export(
                    request, plan, ignored -> { }, () -> false);
            assertEquals(target, result.targetFile());
            assertTrue(Files.size(target) > 10_000L);
            assertTrue(result.totalDurationSeconds() > 1.0);
            Files.writeString(outputRoot.resolve("physical-export-report.txt"),
                    "output=" + target + "\nbytes=" + Files.size(target)
                            + "\ndurationSeconds=" + result.totalDurationSeconds()
                            + "\nframes=" + result.frameCount()
                            + "\npreflightFramesMissingImage=" + preflight.framesMissingImage()
                            + "\npreflightSpokenFramesMissingAudio=" + preflight.spokenFramesMissingAudio()
                            + "\npreviousAudioMissing=" + previousMissing
                            + "\ncontentTypes=" + seen
                            + "\naudioEngine=Piper\nvideoEngine=FFmpeg/x264\n",
                    StandardCharsets.UTF_8);
            Files.writeString(outputRoot.resolve("previous-audio-binding-matrix.txt"),
                    previousAudioMatrix.toString(), StandardCharsets.UTF_8);
        }
    }

    private static void synthesize(Path piper, Path model, String text, Path output)
            throws Exception {
        Process process = new ProcessBuilder(piper.toString(), "--model", model.toString(),
                "--output_file", output.toString()).redirectErrorStream(true).start();
        try (var stdin = process.getOutputStream()) {
            stdin.write((text + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        }
        String diagnostics = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), diagnostics);
        assertTrue(Files.isRegularFile(output) && Files.size(output) > 44L, diagnostics);
    }

    private static double wavDuration(Path wav) throws Exception {
        var format = AudioSystem.getAudioFileFormat(wav.toFile());
        return format.getFrameLength() / format.getFormat().getFrameRate();
    }
}
