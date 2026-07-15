package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Projects segment-anchored tracks onto the concrete frame timeline used by final video. */
public final class BuildTheatreVideoAudioOverlayPlanUseCase {
    public VideoAudioOverlayPlan build(DocuPodcastProject project, NarrationScriptDocument script,
                                       SimpleVideoPlan video, Path projectDirectory) throws IOException {
        if (project == null || project.theatre().audioTracks().isEmpty()) return VideoAudioOverlayPlan.emptyPlan();
        Map<String, Double> segmentStarts = segmentStarts(video, script);
        ArrayList<VideoAudioOverlayPlan.Input> inputs = new ArrayList<>();
        for (TheatreProjectLayer.TheatreAudioTrack track : project.theatre().audioTracks()) {
            Optional<NarrationSegment> resolvedSegment = startSegment(project, script, track);
            if (resolvedSegment.isEmpty()) {
                continue;
            }
            NarrationSegment segment = resolvedSegment.get();
            Double timelineStart = segmentStarts.get(segment.id());
            if (timelineStart == null) {
                continue;
            }
            ProjectAssetReference asset = project.assets().byId(track.assetId())
                    .orElseThrow(() -> new IOException("No existe el asset de audio de la pista " + track.id() + "."));
            Path audio = projectDirectory.resolve(asset.relativePath()).toAbsolutePath().normalize();
            if (!audio.startsWith(projectDirectory.toAbsolutePath().normalize()) || !Files.isRegularFile(audio)) {
                throw new IOException("No existe el archivo de la pista " + track.id() + ": " + asset.relativePath());
            }
            inputs.add(new VideoAudioOverlayPlan.Input(track.id(), audio, track.sourceStartSeconds(),
                    track.effectiveEndSeconds(), timelineStart, track.volume(), track.fadeDurationSeconds()));
        }
        inputs.sort(Comparator.comparingDouble(VideoAudioOverlayPlan.Input::timelineStartSeconds));
        validateNoOverlap(inputs);
        return new VideoAudioOverlayPlan(inputs);
    }

    private static Map<String, Double> segmentStarts(SimpleVideoPlan video, NarrationScriptDocument script) {
        LinkedHashMap<String, Double> starts = new LinkedHashMap<>();
        double cursor = 0.0;
        for (SimpleVideoFrame frame : video.frames()) {
            starts.putIfAbsent(frame.segmentId(), cursor);
            if (script != null) {
                double frameStart = cursor;
                script.segments().stream()
                        .filter(segment -> frame.segmentId().equals(segment.id())
                                || frame.segmentId().startsWith(segment.id() + "-"))
                        .forEach(segment -> starts.putIfAbsent(segment.id(), frameStart));
            }
            cursor += frame.frameDurationSeconds();
        }
        return starts;
    }

    private static Optional<NarrationSegment> segmentForBlock(NarrationScriptDocument script, String blockId) {
        if (script == null) return Optional.empty();
        return script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(blockId) || segment.id().equals(blockId)).findFirst();
    }

    private static Optional<NarrationSegment> startSegment(DocuPodcastProject project,
                                                            NarrationScriptDocument script,
                                                            TheatreProjectLayer.TheatreAudioTrack track) {
        if (script == null) return Optional.empty();
        if (!track.startSegmentId().isBlank()) return script.segmentById(track.startSegmentId());
        return project.theatre().intervenciones().stream()
                .filter(item -> item.id().equals(track.startIntervencionId())).findFirst()
                .flatMap(item -> segmentForBlock(script, item.blockId()));
    }

    private static void validateNoOverlap(List<VideoAudioOverlayPlan.Input> inputs) throws IOException {
        for (int i = 1; i < inputs.size(); i++) {
            VideoAudioOverlayPlan.Input previous = inputs.get(i - 1);
            VideoAudioOverlayPlan.Input current = inputs.get(i);
            if (current.timelineStartSeconds() < previous.timelineEndSeconds()) {
                throw new IOException("Las pistas " + previous.overlayId() + " y " + current.overlayId()
                        + " se superponen. Edita o reemplaza una antes de exportar.");
            }
        }
    }
}
