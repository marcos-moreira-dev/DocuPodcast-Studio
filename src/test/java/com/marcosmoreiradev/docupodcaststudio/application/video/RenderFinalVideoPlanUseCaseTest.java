package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenderFinalVideoPlanUseCaseTest {
    @TempDir Path tempDir;
    private Path projectDirectory;
    private Path applicationRoot;
    private Path overlayAudio;
    private RecordingRunner runner;
    private RenderFinalVideoPlanUseCase renderer;

    @BeforeEach
    void setUp() throws Exception {
        projectDirectory = tempDir.resolve("project");
        applicationRoot = tempDir.resolve("app");
        Path tools = applicationRoot.resolve("tools/ffmpeg/bin");
        Files.createDirectories(tools);
        Files.writeString(tools.resolve("ffmpeg.exe"), "fake");
        Files.writeString(tools.resolve("ffprobe.exe"), "fake");
        Files.createDirectories(projectDirectory.resolve("media/images"));
        Files.createDirectories(projectDirectory.resolve("media/audio"));
        Files.createDirectories(projectDirectory.resolve("generated/narrative/clips"));
        Files.write(projectDirectory.resolve("media/images/frame.png"), new byte[] {1});
        Files.write(projectDirectory.resolve("media/audio/narration.wav"), new byte[] {1});
        Files.write(projectDirectory.resolve("generated/narrative/clips/take.mp4"), new byte[] {1});
        overlayAudio = projectDirectory.resolve("media/audio/ambience.wav");
        Files.write(overlayAudio, new byte[] {1});
        runner = new RecordingRunner();
        renderer = new RenderFinalVideoPlanUseCase(
                new FfmpegRuntimeProbeUseCase(runner), new EmbeddedFfmpegLocator(), runner);
    }

    @Test
    void rendersPreparedPlanWithoutCategoryStateAndCleansTemporaryWork() throws Exception {
        Path target = tempDir.resolve("plain.mp4");
        ArrayList<VideoRenderProgress> progress = new ArrayList<>();

        FinalVideoExportResult result = renderer.render(request(target, VideoAudioOverlayPlan.emptyPlan()),
                progress::add, () -> false);

        assertEquals(target.toAbsolutePath().normalize(), result.targetFile());
        assertTrue(Files.size(target) > 0L);
        assertTrue(progress.stream().anyMatch(item -> item.stage() == VideoRenderStage.COMPLETED));
        assertFalse(runner.renderCommands().stream().anyMatch(command -> command.contains("-filter_complex")));
        List<String> frameCommand = runner.renderCommands().stream()
                .filter(command -> command.contains(projectDirectory.resolve("media/audio/narration.wav").toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(frameCommand.stream().anyMatch(part -> part.contains(
                "apad=pad_dur=1.000,atrim=duration=1.000,asetpts=PTS-STARTPTS")));
        assertTrue(frameCommand.stream().anyMatch(part -> part.contains("aresample=48000")));
        assertTrue(frameCommand.stream().anyMatch(part -> part.contains(
                "aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo")));
        assertTrue(frameCommand.contains("-fps_mode"));
        assertTrue(frameCommand.contains("cfr"));
        assertFalse(frameCommand.contains("-shortest"));
        assertTrue(frameCommand.contains("-t"));
        try (var children = Files.list(projectDirectory.resolve("exports/video-render-work"))) {
            assertEquals(0L, children.count());
        }
    }

    @Test
    void appliesOptionalGenericAudioOverlayInASecondPass() throws Exception {
        Path target = tempDir.resolve("overlay.mp4");
        VideoAudioOverlayPlan overlays = new VideoAudioOverlayPlan(List.of(
                new VideoAudioOverlayPlan.Input("AMBIENCE", overlayAudio, 0.0, 1.0, 0.0, 0.25)));

        renderer.render(request(target, overlays), ignored -> { }, () -> false);

        assertTrue(Files.size(target) > 0L);
        assertTrue(runner.renderCommands().stream().anyMatch(command -> command.contains("-filter_complex")));
        assertTrue(runner.renderCommands().stream().anyMatch(command -> command.stream()
                .anyMatch(part -> part.contains("amix=inputs=2"))));
    }

    @Test
    void keepsOverlayMixAliveForTheWholeSilentClosingTimeline() throws Exception {
        SimpleVideoFrame closing = new SimpleVideoFrame(
                "FRAME-CLOSING", "CLOSING-1", "Fin", "", "IMG-001",
                "media/images/frame.png", "", 0.0, 6.0, true, false, true);
        SimpleVideoPlan plan = new SimpleVideoPlan("Cierre", List.of(closing), 0.0, Instant.now());
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_720, 24, 0.0, true, true,
                ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
        VideoAudioOverlayPlan overlays = new VideoAudioOverlayPlan(List.of(
                new VideoAudioOverlayPlan.Input("MUSIC", overlayAudio, 0.0, 1.0, 0.0, 0.25)));
        FinalVideoRenderRequest request = new FinalVideoRenderRequest(
                plan, projectDirectory, tempDir.resolve("closing.mp4"), settings,
                applicationRoot, null, overlays);

        renderer.render(request, ignored -> { }, () -> false);

        List<String> mixCommand = runner.renderCommands().stream()
                .filter(command -> command.contains("-filter_complex"))
                .findFirst()
                .orElseThrow();
        String joined = String.join(" ", mixCommand);
        assertTrue(joined.contains("apad=pad_dur=6.000"));
        assertTrue(joined.contains("atrim=duration=6.000"));
        assertTrue(joined.contains("amix=inputs=2:duration=longest"));
        assertTrue(joined.contains("-t 6.000"));
        assertTrue(joined.contains("-ar 48000 -ac 2"));
        assertFalse(mixCommand.contains("-shortest"));

        List<String> closingCommand = runner.renderCommands().stream()
                .filter(command -> command.stream().anyMatch(part -> part.contains("anullsrc=")))
                .findFirst()
                .orElseThrow();
        assertTrue(closingCommand.contains("anullsrc=channel_layout=stereo:sample_rate=48000"));
        assertTrue(closingCommand.stream().anyMatch(part -> part.contains("fps=24")));
        assertFalse(closingCommand.contains("-shortest"));
    }

    @Test
    void cancellationStopsBeforeEncodingTheFirstFrame() {
        ArrayList<VideoRenderProgress> progress = new ArrayList<>();

        IOException failure = assertThrows(IOException.class, () -> renderer.render(
                request(tempDir.resolve("cancelled.mp4"), VideoAudioOverlayPlan.emptyPlan()),
                progress::add, () -> true));

        assertTrue(failure.getMessage().contains("cancelada"));
        assertTrue(progress.stream().anyMatch(item -> item.stage() == VideoRenderStage.CANCELLED));
        assertTrue(runner.renderCommands().isEmpty());
    }

    @Test
    void rendersNarrativeVideoPartsWithoutTheirAudioAndMuxesNarrationOnce() throws Exception {
        SimpleVideoFrame.VisualPart clip = SimpleVideoFrame.VisualPart.videoClip(
                "VIDEO-001", "generated/narrative/clips/take.mp4", 1.25, 0.75, "clip-1");
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-001", "SEG-001", "Toma", "Texto",
                "IMG-001", "media/images/frame.png", "media/audio/narration.wav",
                1.25, 0.0, true, true, false, List.of(), List.of(clip));
        SimpleVideoPlan plan = new SimpleVideoPlan("Narrativo", List.of(frame), 0.0, Instant.now());
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_VERTICAL_720X1280, 24, 0.0, true, true,
                ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
        FinalVideoRenderRequest request = new FinalVideoRenderRequest(
                plan, projectDirectory, tempDir.resolve("narrative.mp4"), settings,
                applicationRoot, null, VideoAudioOverlayPlan.emptyPlan());

        renderer.render(request, ignored -> { }, () -> false);

        List<List<String>> commands = runner.renderCommands();
        List<String> videoPartCommand = commands.stream()
                .filter(command -> command.contains(projectDirectory.resolve(
                        "generated/narrative/clips/take.mp4").toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(videoPartCommand.contains("-an"));
        assertTrue(videoPartCommand.contains("-ss"));
        assertTrue(videoPartCommand.contains("0.750"));
        assertTrue(videoPartCommand.contains("-t"));
        assertTrue(videoPartCommand.contains("1.250"));
        long narrationInputs = commands.stream()
                .filter(command -> command.contains(projectDirectory.resolve(
                        "media/audio/narration.wav").toString()))
                .count();
        assertEquals(1L, narrationInputs);
    }

    private FinalVideoRenderRequest request(Path target, VideoAudioOverlayPlan overlays) {
        SimpleVideoFrame frame = new SimpleVideoFrame("FRAME-001", "SEG-001", "Escena", "Texto",
                "IMG-001", "media/images/frame.png", "media/audio/narration.wav",
                1.0, 0.0, true, true);
        SimpleVideoPlan plan = new SimpleVideoPlan("Prueba", List.of(frame), 0.0, Instant.now());
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_720, 24, 0.0, true, true,
                ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
        return new FinalVideoRenderRequest(plan, projectDirectory, target, settings,
                applicationRoot, null, overlays);
    }

    private static final class RecordingRunner implements ExternalProcessRunner {
        private final ArrayList<List<String>> commands = new ArrayList<>();

        @Override
        public ExternalProcessResult run(ExternalProcessRequest request) throws IOException {
            commands.add(request.command());
            if (request.command().contains("-encoders")) {
                return result(request, " V....D libx264 libx264 H.264");
            }
            if (request.command().contains("-version")) {
                String executable = request.command().getFirst().toLowerCase();
                return result(request, executable.contains("ffprobe")
                        ? "ffprobe version fake"
                        : "ffmpeg version fake --enable-libx264");
            }
            Path output = Path.of(request.command().getLast());
            Files.createDirectories(output.toAbsolutePath().normalize().getParent());
            Files.write(output, new byte[] {1, 2, 3});
            return result(request, "frame=1 time=00:00:01.00 speed=1x");
        }

        @Override
        public ExternalProcessResult run(ExternalProcessRequest request, ExternalProcessObserver observer)
                throws IOException {
            if (observer != null && observer.cancellationRequested()) {
                return new ExternalProcessResult(1, false, true, "", "", request.commandAudit(), Duration.ZERO);
            }
            if (observer != null) observer.onOutputLine("frame=1 time=00:00:01.00 speed=1x");
            return run(request);
        }

        List<List<String>> renderCommands() {
            return commands.stream()
                    .filter(command -> !command.contains("-version") && !command.contains("-encoders"))
                    .toList();
        }

        private static ExternalProcessResult result(ExternalProcessRequest request, String stdout) {
            return new ExternalProcessResult(0, false, false, stdout, "",
                    request.commandAudit(), Duration.ofMillis(1));
        }
    }
}
