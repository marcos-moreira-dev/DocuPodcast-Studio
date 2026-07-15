package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Owns the low-level playback transport used by the shell.
 *
 * <p>The shell view-model keeps user intent, JavaFX properties and visible messages. This
 * coordinator owns Java Sound operations, cue continuation, runtime queue state, sequential queue
 * driving and transport diagnostics so those cross-cutting playback concerns do not leak across
 * view actions.</p>
 */
public final class PlaybackTransportCoordinator {
    private final SegmentAudioPlayer player;
    private final PlaybackContinuationController continuation;
    private final PlaybackSequentialQueueDriver sequentialQueue = new PlaybackSequentialQueueDriver();
    private final PlaybackRuntimeQueue runtimeQueue = new PlaybackRuntimeQueue();
    private final PlaybackCueDeadlineSequencer deadlineSequencer = new PlaybackCueDeadlineSequencer();
    private final PlaybackDiagnosticRecorder diagnostics = new PlaybackDiagnosticRecorder();

    public record PlaybackTransportCommandResult(
            boolean started,
            Optional<Path> audioFile,
            String message,
            String diagnosticReason,
            boolean playerFailure) {
        private static PlaybackTransportCommandResult started(Optional<Path> audioFile, String diagnosticReason) {
            return new PlaybackTransportCommandResult(true, audioFile, "", diagnosticReason, false);
        }

        private static PlaybackTransportCommandResult failed(Optional<Path> audioFile,
                                                             String message,
                                                             String diagnosticReason,
                                                             boolean playerFailure) {
            return new PlaybackTransportCommandResult(false, audioFile, message, diagnosticReason, playerFailure);
        }
    }

    public PlaybackTransportCoordinator(SegmentAudioPlayer player) {
        this.player = player;
        this.continuation = new PlaybackContinuationController(player);
    }

    public void setOnPlaybackFinished(Consumer<Path> callback) {
        player.setOnPlaybackFinished(callback);
    }

    public void playStandalone(Path audioFile, double startSeconds) throws IOException {
        player.play(audioFile, Math.max(0.0, startSeconds));
    }

    public boolean playCue(PlaybackCue cue,
                           Path audioFile,
                           double localOffsetSeconds,
                           double playbackRate,
                           BooleanSupplier pausedOrInactive,
                           Consumer<PlaybackCue> onCompleted) throws IOException {
        if (cue == null || audioFile == null) {
            return false;
        }
        if (cue.unitId().equals(continuation.activeUnitId())
                && (player.playing() || continuation.blockingCurrentCue(cue))) {
            return true;
        }
        double localOffset = Math.max(0.0, localOffsetSeconds);
        player.play(audioFile, localOffset);
        continuation.startCue(cue, localOffset, playbackRate, 350L);
        runtimeQueue.activate(cue);
        if (!sequentialQueue.active()) {
            startCueMonitoring(cue, localOffset, playbackRate, pausedOrInactive, onCompleted);
        }
        return true;
    }

    public PlaybackTransportCommandResult playProjectCue(Path projectFile,
                                                         PlaybackManifest manifest,
                                                         PlaybackCue cue,
                                                         double localOffsetSeconds,
                                                         double playbackRate,
                                                         BooleanSupplier pausedOrInactive,
                                                         Consumer<PlaybackCue> onCompleted) {
        if (cue == null) {
            return PlaybackTransportCommandResult.failed(Optional.empty(), "", "cue-null", false);
        }
        Optional<Path> audioFile = audioFile(projectFile, cue);
        if (audioFile.isEmpty()) {
            return PlaybackTransportCommandResult.failed(Optional.empty(),
                    "No hay archivo de audio para " + cue.segmentId() + ". "
                            + runtimeDiagnosticLabel(projectFile, manifest, cue, nextRuntimeCueAfter(cue), playbackRate),
                    "audio-file-missing", false);
        }
        Path audio = audioFile.get();
        if (!Files.exists(audio)) {
            return PlaybackTransportCommandResult.failed(audioFile,
                    "No existe el audio del fragmento: " + cue.audioRelativePath() + ". "
                            + runtimeDiagnosticLabel(projectFile, manifest, cue, nextRuntimeCueAfter(cue), playbackRate),
                    "audio-file-not-found", false);
        }
        try {
            boolean started = playCue(cue, audio, localOffsetSeconds, playbackRate, pausedOrInactive, onCompleted);
            return PlaybackTransportCommandResult.started(audioFile, "offset="
                    + String.format(java.util.Locale.ROOT, "%.3f", Math.max(0.0, localOffsetSeconds)));
        } catch (IOException ex) {
            return PlaybackTransportCommandResult.failed(audioFile,
                    "No se pudo reproducir " + cue.segmentId() + ": " + ex.getMessage()
                            + ". El audio fue generado, pero el reproductor interno no pudo abrirlo.",
                    ex.getMessage(), true);
        }
    }

    public void startSequentialPlayback(PlaybackManifest manifest,
                                        PlaybackCue cue,
                                        double playbackRate,
                                        PlaybackSequentialQueueDriver.CueStarter starter,
                                        Runnable onCompleted) {
        sequentialQueue.start(manifest, cue, playbackRate, starter, this::playerPlaying, onCompleted);
    }

    public void attachSequentialPlayback(PlaybackManifest manifest,
                                         PlaybackCue cue,
                                         double playbackRate,
                                         double localOffsetSeconds,
                                         PlaybackSequentialQueueDriver.CueStarter starter,
                                         Runnable onCompleted) {
        sequentialQueue.attachToCurrentCue(manifest, cue, playbackRate, localOffsetSeconds,
                starter, this::playerPlaying, onCompleted);
    }

    public void stopSequentialPlayback() {
        sequentialQueue.stop();
    }

    public void pause() {
        player.pause();
        continuation.pause();
        sequentialQueue.pause();
    }

    public void resume(double localOffsetSeconds) {
        player.resume();
        continuation.resume();
        sequentialQueue.resume(localOffsetSeconds);
    }

    public void setPlaybackRate(double rate) {
        player.setPlaybackRate(rate);
        continuation.setPlaybackRate(rate);
        sequentialQueue.setPlaybackRate(rate, 0.0);
    }

    public void restartActiveCueAtRate(double rate) {
        player.stop();
        continuation.stop();
        player.setPlaybackRate(rate);
    }

    public void setSequentialPlaybackRate(double rate, double localOffsetSeconds) {
        sequentialQueue.setPlaybackRate(rate, localOffsetSeconds);
    }

    public void stopPlayerAndContinuation() {
        player.stop();
        continuation.stop();
    }

    public void stopTransport() {
        sequentialQueue.stop();
        deadlineSequencer.stop();
        stopPlayerAndContinuation();
    }

    public void stopCueMonitoring() {
        deadlineSequencer.stop();
    }

    public boolean continuationActive() {
        return continuation.active();
    }

    public boolean sequentialActive() {
        return sequentialQueue.active();
    }

    public boolean playerPlaying() {
        return player.available() && player.playing();
    }

    public String playerStatusLabel() {
        return player.statusLabel();
    }

    public String manifestRuntimeLabel(PlaybackManifest manifest) {
        if (manifest == null || manifest.emptyManifest()) {
            return "Manifest de audio sin cues reproducibles";
        }
        return "Manifest " + manifest.sourceJobId() + ": " + manifest.cueCount() + " fragmentos listos";
    }

    public Optional<PlaybackCue> sequentialActiveCue() {
        return sequentialQueue.activeCue();
    }

    public void advanceSequentialAfterCurrentCueFinished() {
        sequentialQueue.advanceAfterCurrentCueFinished();
    }

    public void advanceSequentialAfterCurrentCueFinished(String unitId) {
        sequentialQueue.advanceAfterCurrentCueFinished(unitId);
    }

    public Optional<PlaybackCue> cueForCursor(PlaybackManifest manifest, PlaybackCursor cursor) {
        return continuation.activeCue(manifest, cursor);
    }

    public double absolutePosition(PlaybackCue cue, PlaybackCursor cursor) {
        return continuation.absolutePosition(cue, cursor);
    }

    public boolean shouldIgnoreTransientStop() {
        return continuation.shouldIgnoreTransientStop();
    }

    public boolean shouldAdvance(PlaybackCue cue, double absolutePositionSeconds) {
        return continuation.shouldAdvance(cue, absolutePositionSeconds);
    }

    public void markPlayerFinished() {
        continuation.markPlayerFinished();
    }

    public void consumePlayerFinishedMarker() {
        continuation.consumePlayerFinishedMarker();
    }

    public Optional<PlaybackCue> nextCue(PlaybackManifest manifest, PlaybackCue completedCue) {
        return continuation.nextCue(manifest, completedCue);
    }

    public void resetRuntimeQueue() {
        runtimeQueue.reset();
    }

    public void startRuntimeQueue(PlaybackManifest manifest, PlaybackCue cue) {
        runtimeQueue.start(manifest, cue);
    }

    public void refreshRuntimeQueue(PlaybackManifest manifest) {
        runtimeQueue.refresh(manifest);
    }

    public void refreshSequentialQueue(PlaybackManifest manifest) {
        sequentialQueue.refresh(manifest);
    }

    public void activateRuntimeCue(PlaybackCue cue) {
        runtimeQueue.activate(cue);
    }

    public Optional<PlaybackCue> nextRuntimeCueAfter(PlaybackCue cue) {
        return runtimeQueue.nextAfter(cue);
    }

    public String runtimeDiagnosticLabel(PlaybackManifest manifest,
                                         PlaybackCue activeCue,
                                         Optional<PlaybackCue> nextCue,
                                         Optional<Path> audioFile,
                                         double playbackRate) {
        double playerPosition = player.available() ? player.currentPositionSeconds() : -1.0;
        return PlaybackRuntimeDiagnostics.from(
                manifest,
                activeCue,
                nextCue,
                audioFile,
                playerPosition,
                playbackRate,
                player.available() && player.playing(),
                runtimeQueue.diagnosticLabel() + " " + sequentialQueue.diagnosticLabel())
                .compactLabel();
    }

    public String runtimeDiagnosticLabel(Path projectFile,
                                         PlaybackManifest manifest,
                                         PlaybackCue activeCue,
                                         Optional<PlaybackCue> nextCue,
                                         double playbackRate) {
        return runtimeDiagnosticLabel(manifest, activeCue, nextCue, audioFile(projectFile, activeCue), playbackRate);
    }

    public Optional<Path> audioFile(Path projectFile, PlaybackCue cue) {
        if (projectFile == null || cue == null || cue.audioRelativePath().isBlank()
                || projectFile.toAbsolutePath().normalize().getParent() == null) {
            return Optional.empty();
        }
        return Optional.of(projectFile.toAbsolutePath().normalize().getParent()
                .resolve(cue.audioRelativePath())
                .normalize());
    }

    public Path recordPlaybackEvent(Path currentProjectFile,
                                    String event,
                                    PlaybackCue cue,
                                    Optional<Path> audioFile,
                                    double playbackRate,
                                    String reason,
                                    Consumer<String> suspiciousCutCallback) {
        return diagnostics.recordPlaybackEvent(
                currentProjectFile,
                event,
                cue,
                audioFile,
                player,
                playbackRate,
                sequentialQueue.active(),
                runtimeQueue.diagnosticLabel(),
                reason,
                suspiciousCutCallback);
    }

    public Path recordPlaybackEvent(Path currentProjectFile,
                                    String event,
                                    PlaybackCue cue,
                                    double playbackRate,
                                    String reason,
                                    Consumer<String> suspiciousCutCallback) {
        return recordPlaybackEvent(
                currentProjectFile,
                event,
                cue,
                audioFile(currentProjectFile, cue),
                playbackRate,
                reason,
                suspiciousCutCallback);
    }

    public Path diagnosticFile(Path currentProjectFile) {
        return diagnostics.diagnosticFile(currentProjectFile);
    }

    public boolean matchesCompletedAudioFile(PlaybackCue cue, Path projectFile, Path completedFile) {
        if (cue == null || projectFile == null || completedFile == null || cue.audioRelativePath().isBlank()) {
            return false;
        }
        Path expected = projectFile.toAbsolutePath().normalize().getParent().resolve(cue.audioRelativePath()).normalize();
        Path completed = completedFile.toAbsolutePath().normalize();
        if (expected.equals(completed)) {
            return true;
        }
        String expectedName = expected.getFileName() == null ? "" : expected.getFileName().toString();
        String completedName = completed.getFileName() == null ? "" : completed.getFileName().toString();
        return !expectedName.isBlank() && expectedName.equals(completedName)
                && cue.audioRelativePath().replace('\\', '/').endsWith(expectedName);
    }

    public Optional<PlaybackCue> nextCueAfterCompletedSegmentFallback(PlaybackManifest manifest, PlaybackCue completedCue) {
        if (manifest == null || manifest.emptyManifest() || completedCue == null) {
            return Optional.empty();
        }
        Optional<PlaybackCue> bySegment = manifest.nextCueAfter(completedCue.segmentId());
        if (bySegment.isPresent() && !bySegment.get().unitId().equals(completedCue.unitId())) {
            return bySegment;
        }
        return manifest.cues().stream()
                .filter(cue -> cue.startSeconds() > completedCue.startSeconds() + 0.000_001)
                .findFirst();
    }

    private void startCueMonitoring(PlaybackCue cue,
                                    double localOffsetSeconds,
                                    double playbackRate,
                                    BooleanSupplier pausedOrInactive,
                                    Consumer<PlaybackCue> completed) {
        deadlineSequencer.start(
                cue,
                localOffsetSeconds,
                playbackRate,
                pausedOrInactive,
                continuation::transitionGuardActive,
                this::playerPlaying,
                () -> player.available() ? player.currentPositionSeconds() : -1.0,
                completed);
    }
}
