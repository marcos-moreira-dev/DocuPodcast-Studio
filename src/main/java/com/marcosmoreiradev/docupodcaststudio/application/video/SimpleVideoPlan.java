package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.time.Instant;
import java.util.List;

/** Exportable plan for a simple image+audio/silent-visual video, without embedding a video encoder in the core app. */
public record SimpleVideoPlan(
        String title,
        List<SimpleVideoFrame> frames,
        double silenceAfterFrameSeconds,
        Instant createdAt
) {
    public SimpleVideoPlan {
        title = title == null || title.isBlank() ? "Video simple DocuPodcast" : title.strip();
        frames = frames == null ? List.of() : List.copyOf(frames);
        silenceAfterFrameSeconds = Math.max(0.0, silenceAfterFrameSeconds);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public int frameCount() {
        return frames.size();
    }

    public double totalDurationSeconds() {
        return frames.stream().mapToDouble(SimpleVideoFrame::frameDurationSeconds).sum();
    }

    public long framesWithImage() {
        return frames.stream().filter(SimpleVideoFrame::imageAssigned).count();
    }

    public long framesWithAudio() {
        return frames.stream().filter(SimpleVideoFrame::audioReady).count();
    }

    public long framesSilentVisual() {
        return frames.stream().filter(SimpleVideoFrame::silentVisual).count();
    }

    public long framesMissingImage() {
        return frameCount() - framesWithImage();
    }

    public long framesMissingAudio() {
        return frames.stream().filter(SimpleVideoFrame::missingRequiredAudio).count();
    }

    public boolean renderUnitDriven() {
        return frames.stream().anyMatch(SimpleVideoFrame::silentVisual)
                || frames.stream().noneMatch(frame -> !frame.imageAssigned());
    }

    public boolean exportableAsRenderedVideo() {
        return frameCount() > 0 && framesMissingImage() == 0 && framesMissingAudio() == 0;
    }
}
