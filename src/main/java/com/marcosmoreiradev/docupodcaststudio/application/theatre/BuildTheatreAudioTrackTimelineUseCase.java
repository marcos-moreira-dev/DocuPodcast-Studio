package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolves segment-anchored background tracks against real or estimated narration time. */
public final class BuildTheatreAudioTrackTimelineUseCase {
    private static final double ESTIMATED_WORDS_PER_SECOND = 2.5;
    private static final double MIN_ESTIMATED_SEGMENT_SECONDS = 1.0;

    public TheatreAudioTrackTimeline execute(DocuPodcastProject project,
                                             NarrationScriptDocument script,
                                             PlaybackManifest manifest) {
        Objects.requireNonNull(project, "project");
        NarrationScriptDocument safeScript = script == null
                ? NarrationScriptDocument.create("Lectura", "es", "", List.of()) : script;
        PlaybackManifest safeManifest = manifest == null ? PlaybackManifest.empty() : manifest;
        Map<String, SegmentWindow> windows = segmentWindows(safeScript, cuesBySegment(safeManifest));
        List<TheatreProjectLayer.Intervencion> interventions = project.theatre().intervenciones().stream()
                .sorted(java.util.Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex))
                .toList();

        ArrayList<TheatreAudioTrackTimelineEntry> result = new ArrayList<>();
        for (TheatreProjectLayer.TheatreAudioTrack track : project.theatre().audioTracks()) {
            result.add(resolve(project, safeScript, track, interventions, windows));
        }
        return new TheatreAudioTrackTimeline(result);
    }

    private TheatreAudioTrackTimelineEntry resolve(DocuPodcastProject project,
                                                    NarrationScriptDocument script,
                                                    TheatreProjectLayer.TheatreAudioTrack track,
                                                    List<TheatreProjectLayer.Intervencion> interventions,
                                                    Map<String, SegmentWindow> windows) {
        Optional<TheatreProjectLayer.Intervencion> startIntervention = interventions.stream()
                .filter(item -> item.id().equals(track.startIntervencionId())).findFirst();
        Optional<NarrationSegment> startSegment = track.startSegmentId().isBlank()
                ? startIntervention.flatMap(item -> segmentForBlock(script, item.blockId()))
                : script.segmentById(track.startSegmentId());
        Optional<ProjectAssetReference> asset = project.assets().byId(track.assetId());
        if (startSegment.isEmpty() || !windows.containsKey(startSegment.get().id())) {
            return invalid(track, asset, "No se pudo ubicar el fragmento inicial en la lectura preparada.");
        }
        if (asset.isEmpty() || !asset.get().isAudio()) {
            return invalid(track, asset, "El archivo de audio asignado no existe en el proyecto.");
        }
        double duration = track.playbackDurationSeconds();
        if (duration <= 0.0) {
            return invalid(track, asset, "El recorte de la pista no tiene duracion reproducible.");
        }
        double start = windows.get(startSegment.get().id()).startSeconds();
        double end = start + duration;
        List<String> affectedSegments = windows.values().stream()
                .filter(window -> window.startSeconds() < end && start < window.endSeconds())
                .map(SegmentWindow::segmentId)
                .toList();
        List<String> affectedInterventions = interventions.stream()
                .filter(item -> segmentForBlock(script, item.blockId())
                        .map(segment -> affectedSegments.contains(segment.id()))
                        .orElse(false))
                .map(TheatreProjectLayer.Intervencion::id)
                .toList();
        return new TheatreAudioTrackTimelineEntry(track, asset.get().relativePath(), asset.get().displayName(),
                startSegment.get().id(), start, end, affectedSegments, affectedInterventions, true, "Pista lista.");
    }

    private static TheatreAudioTrackTimelineEntry invalid(TheatreProjectLayer.TheatreAudioTrack track,
                                                           Optional<ProjectAssetReference> asset,
                                                           String message) {
        return new TheatreAudioTrackTimelineEntry(track,
                asset.map(ProjectAssetReference::relativePath).orElse(""),
                asset.map(ProjectAssetReference::displayName).orElse(""),
                "", 0.0, 0.0, List.of(), List.of(), false, message);
    }

    private static Optional<NarrationSegment> segmentForBlock(NarrationScriptDocument script, String blockId) {
        return script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(blockId) || segment.id().equals(blockId))
                .findFirst();
    }

    private static Map<String, List<PlaybackCue>> cuesBySegment(PlaybackManifest manifest) {
        LinkedHashMap<String, List<PlaybackCue>> grouped = new LinkedHashMap<>();
        for (PlaybackCue cue : manifest.cues()) {
            grouped.computeIfAbsent(cue.segmentId(), ignored -> new ArrayList<>()).add(cue);
        }
        grouped.replaceAll((ignored, cues) -> List.copyOf(cues));
        return Map.copyOf(grouped);
    }

    private static Map<String, SegmentWindow> segmentWindows(
            NarrationScriptDocument script,
            Map<String, List<PlaybackCue>> cuesBySegment) {
        LinkedHashMap<String, SegmentWindow> result = new LinkedHashMap<>();
        double cursor = 0.0;
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) {
                continue;
            }
            List<PlaybackCue> cues = cuesBySegment.getOrDefault(segment.id(), List.of());
            double start;
            double end;
            if (!cues.isEmpty()) {
                start = cues.getFirst().startSeconds();
                end = cues.getLast().endSeconds();
                cursor = Math.max(cursor, end);
            } else {
                start = cursor;
                end = start + Math.max(MIN_ESTIMATED_SEGMENT_SECONDS,
                        segment.wordCount() / ESTIMATED_WORDS_PER_SECOND);
                cursor = end;
            }
            result.put(segment.id(), new SegmentWindow(segment.id(), start, end));
        }
        return result;
    }

    private record SegmentWindow(String segmentId, double startSeconds, double endSeconds) {
    }
}
