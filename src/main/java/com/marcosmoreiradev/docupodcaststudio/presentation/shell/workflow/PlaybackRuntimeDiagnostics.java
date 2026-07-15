package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Human-readable snapshot for debugging the runtime playback transport.
 *
 * <p>This is intentionally presentation/workflow code: it does not decide playback, it only explains
 * the exact active cue, next cue, WAV path and player state so support can diagnose why a long
 * reading stopped after the first chunk.</p>
 */
public record PlaybackRuntimeDiagnostics(
        String manifestId,
        String jobId,
        int cueCount,
        String activeUnitId,
        String activeSegmentId,
        String nextUnitId,
        String nextSegmentId,
        String audioRelativePath,
        boolean audioFileExists,
        double cueDurationSeconds,
        double localPositionSeconds,
        double playbackRate,
        boolean playerPlaying,
        String queueLabel
) {
    public static PlaybackRuntimeDiagnostics from(
            PlaybackManifest manifest,
            PlaybackCue activeCue,
            Optional<PlaybackCue> nextCue,
            Optional<Path> audioFile,
            double localPositionSeconds,
            double playbackRate,
            boolean playerPlaying,
            String queueLabel
    ) {
        PlaybackManifest safeManifest = manifest == null ? PlaybackManifest.empty() : manifest;
        PlaybackCue safeCue = activeCue;
        Path resolved = audioFile == null || audioFile.isEmpty() ? null : audioFile.get();
        boolean exists = resolved != null && Files.exists(resolved);
        Optional<PlaybackCue> safeNext = nextCue == null ? Optional.empty() : nextCue;
        return new PlaybackRuntimeDiagnostics(
                safeManifest.id(),
                safeManifest.sourceJobId(),
                safeManifest.cueCount(),
                safeCue == null ? "" : safeCue.unitId(),
                safeCue == null ? "" : safeCue.segmentId(),
                safeNext.map(PlaybackCue::unitId).orElse("fin"),
                safeNext.map(PlaybackCue::segmentId).orElse("fin"),
                safeCue == null ? "" : safeCue.audioRelativePath(),
                exists,
                safeCue == null ? 0.0 : safeCue.durationSeconds(),
                Math.max(0.0, localPositionSeconds),
                normalizeRate(playbackRate),
                playerPlaying,
                queueLabel == null ? "" : queueLabel.strip()
        );
    }

    public String compactLabel() {
        return "Diagnóstico playback: manifest " + shortId(manifestId) + " · job " + shortId(jobId)
                + " · cues " + cueCount
                + " · activo " + blank(activeUnitId) + "/" + blank(activeSegmentId)
                + " · siguiente " + blank(nextUnitId) + "/" + blank(nextSegmentId)
                + " · WAV " + (audioFileExists ? "existe" : "no existe") + " (" + blank(audioRelativePath) + ")"
                + " · pos " + seconds(localPositionSeconds) + "/" + seconds(cueDurationSeconds)
                + " · velocidad " + rateLabel(playbackRate)
                + " · player " + (playerPlaying ? "sonando" : "detenido")
                + (queueLabel.isBlank() ? "" : " · " + queueLabel);
    }

    private static String blank(String value) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? "—" : normalized;
    }

    private static String shortId(String value) {
        String normalized = blank(value);
        if (normalized.length() <= 18) {
            return normalized;
        }
        return normalized.substring(0, 18) + "…";
    }

    private static String seconds(double value) {
        return String.format(Locale.ROOT, "%.2fs", Math.max(0.0, value));
    }

    private static double normalizeRate(double value) {
        if (value >= 1.74) {
            return 1.75;
        }
        if (value >= 1.49) {
            return 1.5;
        }
        return 1.0;
    }

    private static String rateLabel(double value) {
        double normalized = normalizeRate(value);
        return normalized == 1.75 ? "1.75x" : normalized == 1.5 ? "1.5x" : "1x";
    }
}
