package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the generic second FFmpeg pass that mixes audio overlays under narration. */
public final class VideoAudioOverlayFfmpegCommandFactory {
    public List<String> build(Path ffmpeg, Path narrationVideo, Path target, VideoAudioOverlayPlan plan) {
        double timelineDurationSeconds = plan.inputs().stream()
                .mapToDouble(VideoAudioOverlayPlan.Input::timelineEndSeconds)
                .max()
                .orElse(0.0);
        return build(ffmpeg, narrationVideo, target, plan, timelineDurationSeconds);
    }

    public List<String> build(Path ffmpeg, Path narrationVideo, Path target, VideoAudioOverlayPlan plan,
                              double timelineDurationSeconds) {
        ArrayList<String> command = new ArrayList<>(List.of(ffmpeg.toString(), "-y", "-i", narrationVideo.toString()));
        for (VideoAudioOverlayPlan.Input input : plan.inputs()) {
            command.add("-i"); command.add(input.audioFile().toString());
        }
        double timelineDuration = Double.isFinite(timelineDurationSeconds)
                ? Math.max(0.001, timelineDurationSeconds)
                : 0.001;
        StringBuilder filters = new StringBuilder();
        filters.append("[0:a]apad=pad_dur=").append(number(timelineDuration))
                .append(",atrim=duration=").append(number(timelineDuration))
                .append(",asetpts=PTS-STARTPTS,aresample=48000")
                .append(",aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[base]; ");
        ArrayList<String> mixInputs = new ArrayList<>(List.of("[base]"));
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
                    .append(",aresample=48000")
                    .append(",aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo")
                    .append(",adelay=delays=").append(delayMillis).append(":all=1[").append(label).append("]; ");
            mixInputs.add("[" + label + "]");
        }
        filters.append(String.join("", mixInputs)).append("amix=inputs=").append(mixInputs.size())
                .append(":duration=longest:dropout_transition=0:normalize=0")
                .append(",atrim=duration=").append(number(timelineDuration))
                .append(",asetpts=PTS-STARTPTS,aresample=48000")
                .append(",aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[mix]");
        command.addAll(List.of("-filter_complex", filters.toString(), "-map", "0:v:0", "-map", "[mix]",
                "-c:v", "copy", "-c:a", "aac", "-ar", "48000", "-ac", "2",
                "-t", number(timelineDuration), "-avoid_negative_ts", "make_zero",
                "-movflags", "+faststart", target.toString()));
        return command;
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.3f", value); }
}
