package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Deterministic composition request; it is deliberately not generative video. */
public record VideoRenderRequest(
        List<VideoTimelineFrame> timeline,
        int width,
        int height,
        int framesPerSecond,
        Path outputFile,
        Map<String, String> options,
        VideoTimelinePlan plan) {
    public VideoRenderRequest {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        width = Math.max(320, width);
        height = Math.max(180, height);
        framesPerSecond = Math.max(1, Math.min(120, framesPerSecond));
        options = options == null ? Map.of() : Map.copyOf(options);
        if (timeline.isEmpty()) throw new IllegalArgumentException("video timeline is required");
        if (outputFile == null) throw new IllegalArgumentException("video output file is required");
    }

    public VideoRenderRequest(List<VideoTimelineFrame> timeline, int width, int height,
                              int framesPerSecond, Path outputFile, Map<String, String> options) {
        this(timeline, width, height, framesPerSecond, outputFile, options, null);
    }

    public VideoRenderRequest(VideoTimelinePlan plan, Path outputFile, Map<String, String> options) {
        this(legacyFrames(plan), plan.width(), plan.height(), plan.framesPerSecond(), outputFile, options, plan);
    }

    public VideoTimelinePlan effectivePlan() {
        if (plan != null) return plan;
        return fromLegacy(timeline, width, height, framesPerSecond);
    }

    private static List<VideoTimelineFrame> legacyFrames(VideoTimelinePlan plan) {
        if (plan == null) throw new IllegalArgumentException("video timeline plan is required");
        return plan.items().stream().map(item -> new VideoTimelineFrame(
                item.visuals().getFirst().file(), item.narrationAudio(), item.durationSeconds(), item.id())).toList();
    }

    private static VideoTimelinePlan fromLegacy(List<VideoTimelineFrame> frames, int width, int height, int fps) {
        List<VideoTimelineItem> items = frames.stream().map(frame -> new VideoTimelineItem(
                frame.label().isBlank() ? "frame-" + Integer.toHexString(System.identityHashCode(frame)) : frame.label(),
                List.of(new TimelineVisualSource(TimelineVisualKind.STILL_IMAGE, frame.image(), 0,
                        frame.durationSeconds(), frame.label(), Map.of())),
                frame.audio(), frame.durationSeconds(), Map.of())).toList();
        return new VideoTimelinePlan(items, List.of(), width, height, fps,
                VideoEncodingPreference.AUTO, Map.of("compatibility", "legacy-frames"));
    }
}
