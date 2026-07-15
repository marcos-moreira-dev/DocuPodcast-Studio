package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCueKind;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Builds a synchronized playback manifest from script segments, persisted audio and optional unit-level layers. */
public final class BuildPlaybackManifestUseCase {
    public PlaybackManifest build(NarrationScriptDocument script, AudioJobSnapshot snapshot, StoryboardDocument storyboard) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, AudioSegmentSnapshot> completedBySegment = completedBySegment(snapshot);
        ArrayList<PlaybackCue> cues = new ArrayList<>();
        double cursor = 0.0;
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) {
                continue;
            }
            Optional<PlaybackCue> cue = segmentCue(segment, completedBySegment.get(segment.id()), storyboard, cursor);
            if (cue.isEmpty()) {
                continue;
            }
            cues.add(cue.get());
            cursor = cue.get().endSeconds();
        }
        return new PlaybackManifest("PLAYBACK-" + snapshot.jobId(), snapshot.jobId(), cues, snapshot.finalAudioPath(), Instant.now());
    }

    /**
     * Builds playback from the T91 render plan when external audio clips are present.
     *
     * <p>T92 intentionally keeps segment-level TTS compatibility: segments without audio-clip units
     * still use their generated segment WAV. A segment that has user audio units uses those units as
     * the effective playable material, so Audio del computador stops being only metadata.</p>
     */
    public PlaybackManifest build(NarrationScriptDocument script,
                                  AudioJobSnapshot snapshot,
                                  StoryboardDocument storyboard,
                                  NarrationRenderPlan renderPlan,
                                  DocuPodcastProject project) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(snapshot, "snapshot");
        if (renderPlan == null || renderPlan.empty() || project == null) {
            return build(script, snapshot, storyboard);
        }
        Map<String, AudioSegmentSnapshot> completedByUnitOrSegment = completedBySegment(snapshot);
        ArrayList<PlaybackCue> cues = new ArrayList<>();
        double cursor = 0.0;
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) {
                continue;
            }
            List<NarrationRenderUnit> units = renderPlan.unitsForSegment(segment.id());
            int cueCountBeforeSegment = cues.size();
            if (units.isEmpty()) {
                Optional<PlaybackCue> cue = segmentCue(segment, completedByUnitOrSegment.get(segment.id()), storyboard, cursor);
                if (cue.isPresent()) {
                    cues.add(cue.get());
                    cursor = cue.get().endSeconds();
                }
                continue;
            }
            boolean hasExternalAudioClip = units.stream().anyMatch(NarrationRenderUnit::usesAudioClip);
            for (NarrationRenderUnit unit : units) {
                if (hasExternalAudioClip && !unit.usesAudioClip()) {
                    continue;
                }
                Optional<PlaybackCue> cue = cueForNarrationUnit(segment, unit, completedByUnitOrSegment, storyboard, project, cursor);
                if (cue.isPresent()) {
                    cues.add(cue.get());
                    cursor = cue.get().endSeconds();
                }
            }
            if (cues.size() == cueCountBeforeSegment) {
                Optional<PlaybackCue> fallback = segmentCue(segment, completedByUnitOrSegment.get(segment.id()), storyboard, cursor);
                if (fallback.isPresent()) {
                    cues.add(fallback.get());
                    cursor = fallback.get().endSeconds();
                }
            }
        }
        return new PlaybackManifest("PLAYBACK-" + snapshot.jobId(), snapshot.jobId(), cues, snapshot.finalAudioPath(), Instant.now());
    }

    private static Optional<PlaybackCue> cueForNarrationUnit(NarrationSegment segment,
                                                             NarrationRenderUnit unit,
                                                             Map<String, AudioSegmentSnapshot> completedByUnitOrSegment,
                                                             StoryboardDocument storyboard,
                                                             DocuPodcastProject project,
                                                             double cursor) {
        if (unit == null) {
            return Optional.empty();
        }
        String imageAssetId = !unit.imageAssetId().isBlank()
                ? unit.imageAssetId()
                : storyboardImage(storyboard, segment.id());
        String title = segment.title().isBlank() ? unit.id() : segment.title() + " · " + unit.id();
        if (unit.usesAudioClip()) {
            Optional<ProjectAssetReference> audioAsset = project.assets().byId(unit.audioAssetId())
                    .filter(ProjectAssetReference::isAudio);
            if (audioAsset.isEmpty()) {
                return Optional.empty();
            }
            double duration = estimatedDuration(unit);
            return Optional.of(new PlaybackCue(
                    segment.id(),
                    unit.id(),
                    cursor,
                    cursor + duration,
                    unit.audioAssetId(),
                    audioAsset.get().relativePath(),
                    imageAssetId,
                    title,
                    unit.text(),
                    cueKind(segment)
            ));
        }
        if (!unit.usesTts()) {
            return Optional.empty();
        }
        AudioSegmentSnapshot audio = completedByUnitOrSegment.get(unit.id());
        if (audio == null) {
            audio = completedByUnitOrSegment.get(segment.id());
        }
        if (audio == null || !audio.completed() || audio.audioRelativePath().isBlank()) {
            return Optional.empty();
        }
        double duration = audio.durationSeconds() > 0.0 ? audio.durationSeconds() : estimatedDuration(unit);
        return Optional.of(new PlaybackCue(
                segment.id(),
                unit.id(),
                cursor,
                cursor + duration,
                "AUD-" + unit.id(),
                audio.audioRelativePath(),
                imageAssetId,
                title,
                unit.text(),
                cueKind(segment)
        ));
    }

    /**
     * Builds playback directly from the TI1/TI2 RenderUnitPlan.
     *
     * <p>TI7 makes the playback manifest unit-aware: generated TTS audio is
     * matched by RenderUnit id, external human/computer audio is resolved from
     * project assets, and visual-silent/omitted units are not exposed as playable
     * audio cues. Segment-level playback remains available through the legacy
     * overloads above.</p>
     */
    public PlaybackManifest build(RenderUnitPlan renderUnitPlan, AudioJobSnapshot snapshot, DocuPodcastProject project) {
        Objects.requireNonNull(renderUnitPlan, "renderUnitPlan");
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, AudioSegmentSnapshot> completedByUnit = completedBySegment(snapshot);
        ArrayList<PlaybackCue> cues = new ArrayList<>();
        double cursor = 0.0;
        for (RenderUnit unit : renderUnitPlan.audioUnits()) {
            Optional<PlaybackCue> cue = cueForUnit(unit, completedByUnit, project, cursor);
            if (cue.isEmpty()) {
                continue;
            }
            cues.add(cue.get());
            cursor = cue.get().endSeconds();
        }
        return new PlaybackManifest("PLAYBACK-" + snapshot.jobId(), snapshot.jobId(), cues, snapshot.finalAudioPath(), Instant.now());
    }

    private static Optional<PlaybackCue> cueForUnit(RenderUnit unit,
                                                         Map<String, AudioSegmentSnapshot> completedByUnit,
                                                         DocuPodcastProject project,
                                                         double cursor) {
        if (unit == null || !unit.kind().spoken()) {
            return Optional.empty();
        }
        if (unit.usesExternalAudio()) {
            if (project == null) {
                return Optional.empty();
            }
            Optional<ProjectAssetReference> audioAsset = project.assets().byId(unit.audioAssetId())
                    .filter(ProjectAssetReference::isAudio);
            if (audioAsset.isEmpty()) {
                return Optional.empty();
            }
            double duration = estimatedDuration(unit);
            return Optional.of(new PlaybackCue(
                    unit.segmentId(),
                    unit.id(),
                    cursor,
                    cursor + duration,
                    unit.audioAssetId(),
                    audioAsset.get().relativePath(),
                    unit.imageAssetId(),
                    unit.title().isBlank() ? unit.id() : unit.title(),
                    unit.text()
            ));
        }
        if (!unit.requiresAudioGeneration()) {
            return Optional.empty();
        }
        AudioSegmentSnapshot audio = completedByUnit.get(unit.id());
        if (audio == null && unit.id().equals(unit.segmentId())) {
            audio = completedByUnit.get(unit.segmentId());
        }
        if (audio == null || !audio.completed() || audio.audioRelativePath().isBlank()) {
            return Optional.empty();
        }
        double duration = audio.durationSeconds() > 0.0 ? audio.durationSeconds() : estimatedDuration(unit);
        return Optional.of(new PlaybackCue(
                unit.segmentId(),
                unit.id(),
                cursor,
                cursor + duration,
                "AUD-" + unit.id(),
                audio.audioRelativePath(),
                unit.imageAssetId(),
                unit.title().isBlank() ? unit.id() : unit.title(),
                unit.text()
        ));
    }

    private static Map<String, AudioSegmentSnapshot> completedBySegment(AudioJobSnapshot snapshot) {
        Map<String, AudioSegmentSnapshot> completedBySegment = new LinkedHashMap<>();
        for (AudioSegmentSnapshot segment : snapshot.segments()) {
            if (segment.completed() && !segment.audioRelativePath().isBlank()) {
                completedBySegment.put(segment.segmentId(), segment);
            }
        }
        return completedBySegment;
    }

    private static Optional<PlaybackCue> segmentCue(NarrationSegment segment,
                                                    AudioSegmentSnapshot audio,
                                                    StoryboardDocument storyboard,
                                                    double cursor) {
        if (audio == null) {
            return Optional.empty();
        }
        double duration = audio.durationSeconds() > 0.0 ? audio.durationSeconds() : estimatedDuration(segment);
        return Optional.of(new PlaybackCue(
                segment.id(),
                segment.id(),
                cursor,
                cursor + duration,
                "AUD-" + segment.id(),
                audio.audioRelativePath(),
                storyboardImage(storyboard, segment.id()),
                segment.title().isBlank() ? segment.id() : segment.title(),
                segment.narrationText(),
                cueKind(segment)
        ));
    }

    private static PlaybackCueKind cueKind(NarrationSegment segment) {
        return segment == null ? PlaybackCueKind.NARRATION : PlaybackCueKind.fromSegmentType(segment.type());
    }

    private static String storyboardImage(StoryboardDocument storyboard, String segmentId) {
        return storyboard == null ? "" : storyboard.bindingForSegment(segmentId)
                .map(binding -> binding.imageAssetId())
                .orElse("");
    }

    private static double estimatedDuration(NarrationSegment segment) {
        return Math.max(0.25, Math.min(30.0, segment.narrationText().length() / 22.0));
    }

    private static double estimatedDuration(NarrationRenderUnit unit) {
        return Math.max(0.25, Math.min(30.0, unit.text().length() / 22.0));
    }

    private static double estimatedDuration(RenderUnit unit) {
        return Math.max(0.25, Math.min(30.0, unit.text().length() / 22.0));
    }

}
