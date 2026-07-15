package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.List;
import java.util.Optional;

/** Resolves previous/next fragment targets for the floating document playbar. */
public final class PlaybackFragmentNavigator {
    public NavigationResult target(PlaybackManifest manifest, PlaybackCursor cursor, Optional<NarrationSegment> selected, int direction) {
        if (manifest == null || manifest.emptyManifest()) return NavigationResult.blocked("Primero genera audio para poder saltar entre fragmentos.");
        Optional<PlaybackCue> current = currentCue(manifest, cursor, selected);
        if (current.isEmpty()) return NavigationResult.blocked("Selecciona un fragmento o inicia la lectura antes de saltar.");
        List<PlaybackCue> cues = manifest.cues();
        int index = indexOf(cues, current.get().unitId());
        if (index < 0) return NavigationResult.blocked("No se encontró el fragmento actual en el audio preparado.");
        int targetIndex = index + (direction < 0 ? -1 : 1);
        if (targetIndex < 0 || targetIndex >= cues.size()) {
            return NavigationResult.blocked(direction < 0 ? "Ya estás en el primer fragmento preparado." : "Ya estás en el último fragmento preparado.");
        }
        PlaybackCue target = cues.get(targetIndex);
        return NavigationResult.ready(target, direction < 0 ? "Reproduciendo fragmento anterior: " : "Reproduciendo siguiente fragmento: ");
    }

    private static Optional<PlaybackCue> currentCue(PlaybackManifest manifest, PlaybackCursor cursor, Optional<NarrationSegment> selected) {
        if (cursor != null && !cursor.segmentId().isBlank()) {
            Optional<PlaybackCue> cue = manifest.cueAt(cursor.positionSeconds()).filter(found -> found.segmentId().equals(cursor.segmentId()))
                    .or(() -> manifest.cueForSegment(cursor.segmentId()));
            if (cue.isPresent()) return cue;
        }
        return (selected == null ? Optional.<NarrationSegment>empty() : selected).flatMap(segment -> manifest.cueForSegment(segment.id())).or(manifest::firstCue);
    }

    private static int indexOf(List<PlaybackCue> cues, String unitId) {
        for (int i = 0; i < cues.size(); i++) if (cues.get(i).unitId().equals(unitId)) return i;
        return -1;
    }

    public record NavigationResult(boolean ready, PlaybackCue cue, String message) {
        static NavigationResult ready(PlaybackCue cue, String prefix) { return new NavigationResult(true, cue, prefix + cue.segmentId() + "."); }
        static NavigationResult blocked(String message) { return new NavigationResult(false, null, message); }
    }
}
