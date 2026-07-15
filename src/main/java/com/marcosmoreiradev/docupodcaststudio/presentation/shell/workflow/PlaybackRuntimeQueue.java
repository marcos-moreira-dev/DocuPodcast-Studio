package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.util.List;
import java.util.Optional;

/**
 * Runtime queue for continuous read-aloud playback.
 *
 * <p>The manifest is the persisted synchronization contract; this queue is the live transport
 * contract. Once the user starts from a cue, continuation must use ordered cue identity instead of
 * re-resolving the next audio from the selected sentence. This avoids falling back to the same
 * selected unit after every chunk.</p>
 */
public final class PlaybackRuntimeQueue {
    private List<PlaybackCue> cues = List.of();
    private String activeUnitId = "";
    private String sourceJobId = "";

    public void reset() {
        cues = List.of();
        activeUnitId = "";
        sourceJobId = "";
    }

    public void start(PlaybackManifest manifest, PlaybackCue cue) {
        if (manifest == null || manifest.emptyManifest() || cue == null) {
            reset();
            return;
        }
        cues = manifest.cues();
        sourceJobId = manifest.sourceJobId();
        activeUnitId = cue.unitId();
    }

    public void refresh(PlaybackManifest manifest) {
        if (manifest == null || manifest.emptyManifest()) {
            cues = List.of();
            sourceJobId = "";
            return;
        }
        cues = manifest.cues();
        sourceJobId = manifest.sourceJobId();
    }

    public void activate(PlaybackCue cue) {
        activeUnitId = cue == null ? "" : cue.unitId();
    }

    public Optional<PlaybackCue> activeCue() {
        if (activeUnitId.isBlank()) {
            return Optional.empty();
        }
        return cues.stream().filter(cue -> cue.unitId().equals(activeUnitId)).findFirst();
    }

    public Optional<PlaybackCue> nextAfter(PlaybackCue completedCue) {
        if (completedCue == null || cues.isEmpty()) {
            return Optional.empty();
        }
        String completedUnit = completedCue.unitId();
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).unitId().equals(completedUnit)) {
                return i + 1 < cues.size() ? Optional.of(cues.get(i + 1)) : Optional.empty();
            }
        }
        return cues.stream()
                .filter(cue -> cue.startSeconds() > completedCue.startSeconds() + 0.000_001)
                .findFirst();
    }

    public String diagnosticLabel() {
        if (cues.isEmpty()) {
            return "Cola de lectura: sin fragmentos reproducibles.";
        }
        int index = -1;
        for (int i = 0; i < cues.size(); i++) {
            if (cues.get(i).unitId().equals(activeUnitId)) {
                index = i;
                break;
            }
        }
        String position = index >= 0 ? (index + 1) + "/" + cues.size() : "?/" + cues.size();
        String next = index >= 0 && index + 1 < cues.size() ? cues.get(index + 1).unitId() : "fin";
        String source = sourceJobId == null || sourceJobId.isBlank() ? "job desconocido" : sourceJobId;
        return "Cola de lectura " + source + ": " + position + " · siguiente: " + next + ".";
    }
}
