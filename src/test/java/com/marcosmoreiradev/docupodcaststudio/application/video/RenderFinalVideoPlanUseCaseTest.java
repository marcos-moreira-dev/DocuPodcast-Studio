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
        Files.write(projectDirectory.resolve("media/images/frame.png"), new byte[] {1});
        Files.write(projectDirectory.resolve("media/audio/narration.wav"), new byte[] {1});
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
    void cancellationStopsBeforeEncodingTheFirstFrame() {
        ArrayList<VideoRenderProgress> progress = new ArrayList<>();

        IOException failure = assertThrows(IOException.class, () -> renderer.render(
                request(tempDir.resolve("cancelled.mp4"), VideoAudioOverlayPlan.emptyPlan()),
                progress::add, () -> true));

        assertTrue(failure.getMessage().contains("cancelada"));
        assertTrue(progress.stream().anyMatch(item -> item.stage() == VideoRenderStage.CANCELLED));
        assertTrue(runner.renderCommands().isEmpty());
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
