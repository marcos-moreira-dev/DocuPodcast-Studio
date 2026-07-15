package com.marcosmoreiradev.docupodcaststudio.integration;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreVideoAudioOverlayPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.BuildSimpleVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportNarrativeVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoExportSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeAndTheatreVideoRealSmokeTest {
    @Test
    @EnabledIfSystemProperty(named = "docupodcast.categorySmokeRoot", matches = ".+")
    void exportsNarrativeAndTheatrePathsWithLocalFfmpeg() throws Exception {
        Path sourceRoot = Path.of(System.getProperty("docupodcast.categorySmokeRoot")).toAbsolutePath().normalize();
        Path applicationRoot = Path.of(System.getProperty("docupodcast.applicationRoot", "."))
                .toAbsolutePath().normalize();
        Path sourceImage = sourceRoot.resolve("exports/document-study-frames/doc-frame-001.png");
        Path sourceAudio;
        try (var files = Files.walk(sourceRoot.resolve("jobs"))) {
            sourceAudio = files.filter(path -> path.getFileName().toString().endsWith(".wav"))
                    .findFirst().orElseThrow();
        }
        Path outputRoot = sourceRoot.getParent().resolve("category-smokes");
        Files.createDirectories(outputRoot);

        DefaultExternalProcessRunner runner = new DefaultExternalProcessRunner();
        RenderFinalVideoPlanUseCase renderer = new RenderFinalVideoPlanUseCase(
                new FfmpegRuntimeProbeUseCase(runner), new EmbeddedFfmpegLocator(), runner);
        SimpleVideoExportSettings settings = settings();

        Path narrativeTarget = exportNarrative(outputRoot.resolve("narrative"), sourceImage, sourceAudio,
                applicationRoot, renderer, settings);
        assertVideo(narrativeTarget);

        Path theatreRoot = prepareMedia(outputRoot.resolve("theatre"), sourceImage, sourceAudio);
        NarrationScriptDocument theatreScript = script("Teatro");
        SimpleVideoPlan theatrePlan = plan("Teatro");
        BuildTheatreVideoAudioOverlayPlanUseCase adapter = new BuildTheatreVideoAudioOverlayPlanUseCase();
        VideoAudioOverlayPlan withoutTracks = adapter.build(
                DocuPodcastProject.createNew("Teatro", ProjectMode.THEATRE_PRODUCTION),
                theatreScript, theatrePlan, theatreRoot);
        assertTrue(withoutTracks.empty());
        Path cleanTarget = theatreRoot.resolve("theatre-without-tracks.mp4");
        renderer.render(new FinalVideoRenderRequest(theatrePlan, theatreRoot, cleanTarget, settings,
                applicationRoot, null, withoutTracks));
        assertVideo(cleanTarget);

        DocuPodcastProject projectWithTrack = theatreProjectWithTrack();
        VideoAudioOverlayPlan withTracks = adapter.build(projectWithTrack, theatreScript, theatrePlan, theatreRoot);
        assertFalse(withTracks.empty());
        Path mixedTarget = theatreRoot.resolve("theatre-with-track.mp4");
        renderer.render(new FinalVideoRenderRequest(theatrePlan, theatreRoot, mixedTarget, settings,
                applicationRoot, null, withTracks));
        assertVideo(mixedTarget);
    }

    private static Path exportNarrative(Path root,
                                        Path sourceImage,
                                        Path sourceAudio,
                                        Path applicationRoot,
                                        RenderFinalVideoPlanUseCase renderer,
                                        SimpleVideoExportSettings settings) throws Exception {
        Path projectRoot = prepareMedia(root, sourceImage, sourceAudio);
        NarrationScriptDocument script = script("Narrativa");
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script)
                .withBinding(StoryboardBinding.of("BND-001", "SEG-001", "IMG-001", "Frame"));
        DocuPodcastProject project = DocuPodcastProject.createNew("Narrativa", ProjectMode.NARRATIVE_VIDEO)
                .withAsset(new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Frame",
                        "media/images/frame.png", "image/png", "smoke", "", ""));
        AudioJobSnapshot job = audioJob();
        Path target = projectRoot.resolve("narrative.mp4");
        FinalVideoExportRequest request = new FinalVideoExportRequest(project, null, script, storyboard,
                List.of(job), projectRoot, target, settings, applicationRoot, null);

        new ExportNarrativeVideoUseCase(new BuildSimpleVideoPlanUseCase(), renderer).export(request);
        return target;
    }

    private static Path prepareMedia(Path root, Path sourceImage, Path sourceAudio) throws Exception {
        Files.createDirectories(root.resolve("media/images"));
        Files.createDirectories(root.resolve("media/audio"));
        Files.copy(sourceImage, root.resolve("media/images/frame.png"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceAudio, root.resolve("media/audio/narration.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceAudio, root.resolve("media/audio/ambience.wav"), StandardCopyOption.REPLACE_EXISTING);
        return root;
    }

    private static NarrationScriptDocument script(String title) {
        return NarrationScriptDocument.create(title, "es", "", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH,
                        "Escena", "Texto de prueba.", List.of("B0001"))));
    }

    private static AudioJobSnapshot audioJob() {
        AudioSegmentSnapshot segment = new AudioSegmentSnapshot("SEG-001", "Escena",
                AudioSegmentStatus.COMPLETED, "media/audio/narration.wav", 1.0, 1, "");
        return new AudioJobSnapshot("JOB-001", "Smoke", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0, "OK",
                "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json",
                List.of(segment), Instant.now(), Instant.now());
    }

    private static SimpleVideoPlan plan(String title) {
        SimpleVideoFrame frame = new SimpleVideoFrame("FRAME-001", "SEG-001", "Escena", "Texto",
                "IMG-001", "media/images/frame.png", "media/audio/narration.wav",
                1.0, 0.0, true, true);
        return new SimpleVideoPlan(title, List.of(frame), 0.0, Instant.now());
    }

    private static DocuPodcastProject theatreProjectWithTrack() {
        TheatreProjectLayer.TheatreAudioTrack track = new TheatreProjectLayer.TheatreAudioTrack(
                "TRACK-001", "AUDIO-OVERLAY", "", "SEG-001",
                0.0, 1.0, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.20, 10.0, false);
        return DocuPodcastProject.createNew("Teatro", ProjectMode.THEATRE_PRODUCTION)
                .withAsset(new ProjectAssetReference("AUDIO-OVERLAY", ProjectAssetKind.AUDIO_CLIP, "Ambiente",
                        "media/audio/ambience.wav", "audio/wav", "smoke", "", ""))
                .withTheatre(TheatreProjectLayer.empty().withAudioTracks(List.of(track)));
    }

    private static SimpleVideoExportSettings settings() {
        return new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 24, 0.0,
                true, true, ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
    }

    private static void assertVideo(Path target) throws Exception {
        assertTrue(Files.isRegularFile(target));
        assertTrue(Files.size(target) > 0L);
    }
}
