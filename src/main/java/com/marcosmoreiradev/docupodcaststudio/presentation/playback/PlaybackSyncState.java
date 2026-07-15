package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.List;

/** Cross-workspace playback synchronization state used by Script, Audio and Storyboard. */
public record PlaybackSyncState(
        boolean scriptAvailable,
        boolean manifestAvailable,
        boolean playing,
        boolean paused,
        String activeSegmentId,
        String activeTitle,
        double currentPositionSeconds,
        double totalDurationSeconds,
        int totalSegments,
        int cueCount,
        int audioReadySegments,
        int storyboardReadySegments,
        List<PlaybackSegmentSync> segments
) {
    public PlaybackSyncState {
        activeSegmentId = normalize(activeSegmentId);
        activeTitle = normalize(activeTitle);
        currentPositionSeconds = Math.max(0.0, currentPositionSeconds);
        totalDurationSeconds = Math.max(0.0, totalDurationSeconds);
        totalSegments = Math.max(0, totalSegments);
        cueCount = Math.max(0, cueCount);
        audioReadySegments = Math.max(0, audioReadySegments);
        storyboardReadySegments = Math.max(0, storyboardReadySegments);
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    public static PlaybackSyncState empty() {
        return new PlaybackSyncState(false, false, false, true, "", "", 0.0, 0.0, 0, 0, 0, 0, List.of());
    }

    public static PlaybackSyncState from(
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            PlaybackManifest manifest,
            PlaybackCursor cursor
    ) {
        if (script == null || script.empty()) {
            return empty();
        }
        PlaybackManifest safeManifest = manifest == null ? PlaybackManifest.empty() : manifest;
        PlaybackCursor safeCursor = cursor == null ? PlaybackCursor.stopped() : cursor;
        List<PlaybackSegmentSync> rows = script.segments().stream()
                .filter(segment -> segment.narratable())
                .map(segment -> PlaybackSegmentSync.from(segment, safeManifest, storyboard, safeCursor))
                .toList();
        String activeId = safeCursor.segmentId();
        String activeTitle = rows.stream()
                .filter(row -> row.segmentId().equals(activeId))
                .map(PlaybackSegmentSync::title)
                .findFirst()
                .orElse("");
        return new PlaybackSyncState(
                true,
                !safeManifest.emptyManifest(),
                safeCursor.playing(),
                !safeCursor.stoppedState() && safeCursor.paused(),
                activeId,
                activeTitle,
                safeCursor.positionSeconds(),
                safeManifest.totalDurationSeconds(),
                rows.size(),
                safeManifest.cueCount(),
                (int) rows.stream().filter(PlaybackSegmentSync::audioReady).count(),
                (int) rows.stream().filter(PlaybackSegmentSync::storyboardReady).count(),
                rows
        );
    }

    public String summaryLabel() {
        if (!scriptAvailable) {
            return "Playback: sin lectura preparada.";
        }
        if (!manifestAvailable) {
            return "Playback: sin manifest. Genera audio para activar sincronización transversal.";
        }
        String cursor = activeSegmentId.isBlank()
                ? "cursor detenido"
                : activeSegmentId + " @ " + Math.round(currentPositionSeconds) + " s";
        return "Playback: " + cueCount + " cues · " + Math.round(totalDurationSeconds) + " s · " + cursor;
    }

    public String activeLabel() {
        if (activeSegmentId.isBlank()) {
            return "Segmento activo: ninguno.";
        }
        String title = activeTitle.isBlank() ? activeSegmentId : activeTitle;
        return "Segmento activo: " + activeSegmentId + " — " + title + " · "
                + (playing ? "reproduciendo" : paused ? "pausado" : "preparado");
    }

    public String readinessLabel() {
        if (!scriptAvailable) {
            return "Sin lectura preparada para sincronizar.";
        }
        return "Audio " + audioReadySegments + "/" + totalSegments
                + " · Visuales " + storyboardReadySegments + "/" + totalSegments;
    }

    public List<String> cueLabels() {
        if (!scriptAvailable) {
            return List.of("No hay lectura preparada cargada.");
        }
        if (!manifestAvailable) {
            return List.of("No hay manifest de playback. Genera audio para sincronizar texto, audio e imagen.");
        }
        return segments.stream().map(PlaybackSegmentSync::cueLabel).toList();
    }

    public List<String> segmentLabels() {
        if (!scriptAvailable) {
            return List.of("Prepara la lectura del documento antes de revisar sincronización.");
        }
        return segments.stream()
                .map(row -> row.segmentId() + " · " + row.stateLabel() + " · audio "
                        + (row.audioReady() ? "OK" : "pendiente") + " · visuales "
                        + (row.storyboardReady() ? "OK" : "pendiente"))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
