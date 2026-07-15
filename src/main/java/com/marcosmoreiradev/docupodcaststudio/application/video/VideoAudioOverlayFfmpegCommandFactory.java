package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the generic second FFmpeg pass that mixes audio overlays under narration. */
public final class VideoAudioOverlayFfmpegCommandFactory {
    public List<String> build(Path ffmpeg, Path narrationVideo, Path target, VideoAudioOverlayPlan plan) {
        ArrayList<String> command = new ArrayList<>(List.of(ffmpeg.toString(), "-y", "-i", narrationVideo.toString()));
        for (VideoAudioOverlayPlan.Input input : plan.inputs()) {
            command.add("-i"); command.add(input.audioFile().toString());
        }
        StringBuilder filters = new StringBuilder();
        ArrayList<String> mixInputs = new ArrayList<>(List.of("[0:a]"));
        for (int i = 0; i < plan.inputs().size(); i++) {
            VideoAudioOverlayPlan.Input input = plan.inputs().get(i);
            String label = "bg" + i;
            long delayMillis = Math.round(input.timelineStartSeconds() * 1000.0);
            double duration = input.sourceEndSeconds() - input.sourceStartSeconds();
            filters.append('[').append(i + 1).append(":a]")
                    .append("atrim=start=").append(number(input.sourceStartSeconds()))
                    .append(":end=").append(number(input.sourceEndSeconds()))
                    .append(",asetpts=PTS-STARTPTS");
            if (input.fadeDurationSeconds() > 0.0) {
                filters.append(",afade=t=in:st=0:d=").append(number(input.fadeDurationSeconds()))
                        .append(",afade=t=out:st=").append(number(duration - input.fadeDurationSeconds()))
                        .append(":d=").append(number(input.fadeDurationSeconds()));
            }
            filters.append(",volume=").append(number(input.volume()))
                    .append(",adelay=delays=").append(delayMillis).append(":all=1[").append(label).append("]; ");
            mixInputs.add("[" + label + "]");
        }
        filters.append(String.join("", mixInputs)).append("amix=inputs=").append(mixInputs.size())
                .append(":duration=first:dropout_transition=0:normalize=0[mix]");
        command.addAll(List.of("-filter_complex", filters.toString(), "-map", "0:v:0", "-map", "[mix]",
                "-c:v", "copy", "-c:a", "aac", "-shortest", "-movflags", "+faststart", target.toString()));
        return command;
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.3f", value); }
}
