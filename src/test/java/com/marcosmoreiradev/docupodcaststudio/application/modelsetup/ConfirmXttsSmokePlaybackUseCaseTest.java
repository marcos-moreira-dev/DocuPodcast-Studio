package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfirmXttsSmokePlaybackUseCaseTest {
    @Test
    void confirmsPlaybackOnlyAfterInternalPlayerReportsNaturalEnd() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-smoke-confirm");
        try {
            Path dir = InspectXttsSmokeTestUseCase.smokeDirectory(root);
            Files.createDirectories(dir);
            Path wav = dir.resolve("xtts-readiness-smoke.wav");
            Files.write(wav, new byte[128]);
            Path manifest = dir.resolve("xtts-readiness-smoke.json");
            Files.writeString(manifest, "{\n"
                    + "  \"schema\": \"docupodcast-xtts-readiness-smoke-v1\",\n"
                    + "  \"generatedAt\": \"" + Instant.now() + "\",\n"
                    + "  \"generated\": true,\n"
                    + "  \"playbackConfirmed\": false,\n"
                    + "  \"audioFile\": \"xtts-readiness-smoke.wav\",\n"
                    + "  \"outputBytes\": 128\n"
                    + "}\n");

            ConfirmXttsSmokePlaybackUseCase useCase = new ConfirmXttsSmokePlaybackUseCase(
                    new InspectXttsSmokeTestUseCase(), new FinishingPlayer());
            XttsSmokeTestReport report = useCase.confirm(root);

            assertTrue(report.fullyVerified());
            String updated = Files.readString(manifest);
            assertTrue(updated.contains("\"playbackConfirmed\": true"));
            assertTrue(updated.contains("\"playbackConfirmedAt\""));
            assertTrue(updated.contains("\"playbackConfirmedBy\""));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static final class FinishingPlayer implements SegmentAudioPlayer {
        private Consumer<Path> callback = ignored -> { };
        private double rate = 1.0;

        @Override
        public void play(Path audioFile, double startSeconds) {
            callback.accept(audioFile.toAbsolutePath().normalize());
        }

        @Override public void pause() { }
        @Override public void resume() { }
        @Override public void stop() { }
        @Override public void setPlaybackRate(double rate) { this.rate = rate; }
        @Override public void setOnPlaybackFinished(Consumer<Path> callback) { this.callback = callback == null ? ignored -> { } : callback; }
        @Override public double playbackRate() { return rate; }
        @Override public boolean available() { return true; }
        @Override public boolean playing() { return false; }
        @Override public double currentPositionSeconds() { return 0; }
        @Override public String statusLabel() { return "fake"; }
    }
}
