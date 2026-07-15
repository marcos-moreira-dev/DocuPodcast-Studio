package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.List;

/** Mode-independent FFmpeg-ready audio overlays for a rendered video timeline. */
public record VideoAudioOverlayPlan(List<Input> inputs) {
    public VideoAudioOverlayPlan {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
    }

    public static VideoAudioOverlayPlan emptyPlan() { return new VideoAudioOverlayPlan(List.of()); }

    public boolean empty() { return inputs.isEmpty(); }

    public record Input(String overlayId, Path audioFile, double sourceStartSeconds,
                        double sourceEndSeconds, double timelineStartSeconds, double volume,
                        double fadeDurationSeconds) {
        public Input(String overlayId, Path audioFile, double sourceStartSeconds,
                     double sourceEndSeconds, double timelineStartSeconds, double volume) {
            this(overlayId, audioFile, sourceStartSeconds, sourceEndSeconds, timelineStartSeconds, volume, 0.0);
        }

        public Input {
            overlayId = overlayId == null ? "" : overlayId.strip();
            audioFile = audioFile == null ? null : audioFile.toAbsolutePath().normalize();
            sourceStartSeconds = Math.max(0.0, sourceStartSeconds);
            sourceEndSeconds = Math.max(sourceStartSeconds, sourceEndSeconds);
            timelineStartSeconds = Math.max(0.0, timelineStartSeconds);
            volume = Double.isFinite(volume) ? Math.max(0.0, Math.min(1.0, volume)) : 0.30;
            fadeDurationSeconds = Double.isFinite(fadeDurationSeconds)
                    ? Math.max(0.0, Math.min(fadeDurationSeconds, (sourceEndSeconds - sourceStartSeconds) * 0.25)) : 0.0;
        }

        public double timelineEndSeconds() { return timelineStartSeconds + sourceEndSeconds - sourceStartSeconds; }
    }
}
