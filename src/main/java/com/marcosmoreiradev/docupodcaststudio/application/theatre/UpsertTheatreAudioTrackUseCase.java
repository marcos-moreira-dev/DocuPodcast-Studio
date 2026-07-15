package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Adds or updates one theatre background track while enforcing exclusive timeline occupancy. */
public final class UpsertTheatreAudioTrackUseCase {
    private final BuildTheatreAudioTrackTimelineUseCase timelineUseCase;

    public UpsertTheatreAudioTrackUseCase(BuildTheatreAudioTrackTimelineUseCase timelineUseCase) {
        this.timelineUseCase = Objects.requireNonNull(timelineUseCase, "timelineUseCase");
    }

    public Result execute(DocuPodcastProject project,
                          NarrationScriptDocument script,
                          PlaybackManifest manifest,
                          TheatreProjectLayer.TheatreAudioTrack candidate,
                          boolean replaceConflicts) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(candidate, "candidate");
        if (project.metadata().mode() != ProjectMode.THEATRE_PRODUCTION) {
            throw new IllegalStateException("Las pistas multimedia solo estan disponibles en Produccion teatral.");
        }
        ArrayList<TheatreProjectLayer.TheatreAudioTrack> proposed = new ArrayList<>(project.theatre().audioTracks());
        proposed.removeIf(track -> track.id().equals(candidate.id()));
        proposed.add(candidate);
        DocuPodcastProject projected = project.withTheatre(project.theatre().withAudioTracks(proposed));
        TheatreAudioTrackTimeline timeline = timelineUseCase.execute(projected, script, manifest);
        TheatreAudioTrackTimelineEntry candidateEntry = timeline.entries().stream()
                .filter(entry -> entry.track().id().equals(candidate.id()))
                .findFirst()
                .orElseThrow();
        if (!candidateEntry.valid()) {
            return new Result(project, List.of(), false, candidateEntry.message());
        }
        List<TheatreAudioTrackTimelineEntry> conflicts = timeline.conflictsWith(candidateEntry);
        if (!conflicts.isEmpty() && !replaceConflicts) {
            return new Result(project, conflicts, false,
                    "La pista se superpone con otra pista teatral. Confirma si deseas reemplazarla.");
        }
        if (!conflicts.isEmpty()) {
            List<String> conflictIds = conflicts.stream().map(entry -> entry.track().id()).toList();
            proposed.removeIf(track -> conflictIds.contains(track.id()));
            projected = project.withTheatre(project.theatre().withAudioTracks(proposed));
        }
        return new Result(projected, conflicts, true,
                conflicts.isEmpty() ? "Pista guardada." : "Pista guardada y solapes reemplazados.");
    }

    public record Result(DocuPodcastProject project,
                         List<TheatreAudioTrackTimelineEntry> conflicts,
                         boolean saved,
                         String message) {
        public Result {
            conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
            message = message == null ? "" : message.strip();
        }
    }
}
