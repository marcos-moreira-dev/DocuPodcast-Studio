package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.audio.PcmWavDurationProbe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

/** Validates the complete documentary timeline before invoking the final renderer. */
public final class DocumentStudyVideoExportPreflight {
    private static final double WAV_DURATION_TOLERANCE_SECONDS = 0.005;
    private final PcmWavDurationProbe durationProbe = new PcmWavDurationProbe();

    public Report inspect(SimpleVideoPlan plan, Path projectDirectory) {
        return inspect(plan, projectDirectory, null);
    }

    public Report inspect(SimpleVideoPlan plan, Path projectDirectory,
                          NarrationScriptDocument script) {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Map<String, NarrationSegment> segments = segmentIndex(script);
        ArrayList<MissingFrame> missing = new ArrayList<>();
        ArrayList<DurationMismatch> durationMismatches = new ArrayList<>();
        ArrayList<InvalidVisualBinding> invalidBindings = new ArrayList<>();
        for (SimpleVideoFrame frame : plan.frames()) {
            boolean missingImage = !frame.imageAssigned() || !exists(root, frame.imageRelativePath());
            boolean missingAudio = frame.audioRequired()
                    && (!frame.audioReady() || !exists(root, frame.audioRelativePath()));
            if (missingImage || missingAudio) {
                missing.add(new MissingFrame(frame.id(), frame.segmentId(), frame.narrationPreview(),
                        frame.imageRelativePath(), frame.audioRelativePath(), missingImage, missingAudio));
            }
            if (!missingAudio && frame.audioRequired()) {
                Path wav = root.resolve(frame.audioRelativePath()).toAbsolutePath().normalize();
                try {
                    double actual = durationProbe.durationSeconds(wav);
                    if (Math.abs(actual - frame.audioDurationSeconds())
                            > WAV_DURATION_TOLERANCE_SECONDS) {
                        durationMismatches.add(new DurationMismatch(frame.id(), frame.segmentId(),
                                frame.audioDurationSeconds(), actual, frame.audioRelativePath()));
                    }
                } catch (Exception ex) {
                    durationMismatches.add(new DurationMismatch(frame.id(), frame.segmentId(),
                            frame.audioDurationSeconds(), Double.NaN, frame.audioRelativePath()));
                }
            }
            if (frame.audioRequired()) {
                var binding = frame.visualBinding();
                String problem = bindingProblem(frame, binding);
                if (problem.isBlank() && !segments.isEmpty()) {
                    NarrationSegment segment = narrationSegment(segments, frame.segmentId());
                    if (segment == null) {
                        problem = "NARRATION_SEGMENT_NOT_FOUND";
                    } else {
                        String explicit = segment.metadata().getOrDefault("sourceBlockId", "").strip();
                        String primary = explicit.isBlank()
                                ? segment.sourceBlockIds().stream().findFirst().orElse("") : explicit;
                        if (!primary.equals(binding.sourceBlockId())) {
                            problem = "PRIMARY_SOURCE_BLOCK_ID_MISMATCH";
                        }
                    }
                }
                if (!problem.isBlank()) {
                    invalidBindings.add(new InvalidVisualBinding(frame.id(), frame.segmentId(),
                            binding == null ? "" : binding.sourceBlockId(),
                            binding == null ? "" : binding.regionId(), problem));
                }
            }
        }
        return new Report(plan.frameCount(), List.copyOf(missing),
                List.copyOf(durationMismatches), List.copyOf(invalidBindings));
    }

    private static Map<String, NarrationSegment> segmentIndex(NarrationScriptDocument script) {
        LinkedHashMap<String, NarrationSegment> result = new LinkedHashMap<>();
        if (script != null) script.segments().forEach(segment -> result.put(segment.id(), segment));
        return result;
    }

    private static NarrationSegment narrationSegment(Map<String, NarrationSegment> segments,
                                                     String frameSegmentId) {
        NarrationSegment exact = segments.get(frameSegmentId);
        if (exact != null) return exact;
        return segments.entrySet().stream()
                .filter(entry -> frameSegmentId.startsWith(entry.getKey() + "-"))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private static String bindingProblem(SimpleVideoFrame frame,
                                         com.marcosmoreiradev.docupodcaststudio.application.video
                                                 .NarratedFrameBinding binding) {
        if (binding == null) return "MISSING_NARRATED_FRAME_BINDING";
        if (!binding.fragmentCovered()) return "WRONG_FRAGMENT_VISUAL: fragment geometry is not covered";
        if (binding.wrongVisual()) return "WRONG_VISUAL: " + binding.reason();
        if (!frame.id().equals(binding.frameId())) return "FRAME_ID_MISMATCH";
        if (!frame.segmentId().equals(binding.segmentId())) return "SEGMENT_ID_MISMATCH";
        if (!frame.imageRelativePath().equals(binding.imageRelativePath())) return "IMAGE_PATH_MISMATCH";
        if (!frame.audioRelativePath().equals(binding.audioPath())) return "AUDIO_PATH_MISMATCH";
        if (Math.abs(frame.audioDurationSeconds() * 1000.0 - binding.durationMillis()) > 1.0) {
            return "DURATION_MISMATCH";
        }
        return "";
    }

    private static boolean exists(Path root, String relative) {
        if (relative == null || relative.isBlank()) return false;
        Path path = root.resolve(relative).toAbsolutePath().normalize();
        return path.startsWith(root) && Files.isRegularFile(path);
    }

    public record MissingFrame(String frameId, String narrationSegmentId, String spokenText,
                               String imageRelativePath, String audioRelativePath,
                               boolean missingImage, boolean missingAudio) { }

    public record DurationMismatch(String frameId, String narrationSegmentId,
                                   double plannedAudioDurationSeconds,
                                   double actualWavDurationSeconds,
                                   String audioRelativePath) { }

    public record InvalidVisualBinding(String frameId, String narrationSegmentId,
                                       String sourceBlockId, String regionId,
                                       String reason) { }

    public record Report(int frameCount, List<MissingFrame> missingFrames,
                         List<DurationMismatch> durationMismatches,
                         List<InvalidVisualBinding> invalidVisualBindings) {
        public long framesMissingImage() { return missingFrames.stream().filter(MissingFrame::missingImage).count(); }
        public long spokenFramesMissingAudio() { return missingFrames.stream().filter(MissingFrame::missingAudio).count(); }
        public boolean ready() { return missingFrames.isEmpty()
                && durationMismatches.isEmpty() && invalidVisualBindings.isEmpty()
                && frameCount > 0; }
    }
}
