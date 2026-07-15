package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.List;

/** Runtime data needed to render target-specific options inside Export Center. */
public record ExportCenterContext(
        ProjectMode projectMode,
        List<TheatreProjectLayer.TheatreAct> acts,
        List<TheatreProjectLayer.Scene> scenes,
        List<VideoEncoderPolicy> availableEncoders,
        VideoEncoderPolicy defaultEncoder
) {
    public ExportCenterContext {
        projectMode = projectMode == null ? ProjectMode.defaultMode() : projectMode;
        acts = acts == null ? List.of() : List.copyOf(acts);
        scenes = scenes == null ? List.of() : List.copyOf(scenes);
        availableEncoders = availableEncoders == null || availableEncoders.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : List.copyOf(availableEncoders);
        defaultEncoder = defaultEncoder == null ? VideoEncoderPolicy.AUTO : defaultEncoder;
    }

    public static ExportCenterContext defaults() {
        return new ExportCenterContext(ProjectMode.defaultMode(), List.of(), List.of(), List.of(), VideoEncoderPolicy.AUTO);
    }
}
