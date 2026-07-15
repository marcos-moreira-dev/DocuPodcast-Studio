package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;

/** User-selected scope and target renderer for a partial theatre export. */
public record TheatrePortionExportOptions(
        TheatreExportScope scope,
        Output output,
        TheatreMapCompanionMode companionMode,
        VideoExportOptions videoOptions
) {
    public TheatrePortionExportOptions {
        scope = scope == null ? TheatreExportScope.all() : scope;
        output = output == null ? Output.THEATRE_MAP : output;
        companionMode = companionMode == null ? TheatreMapCompanionMode.FRAGMENT_VISUALS : companionMode;
        videoOptions = videoOptions == null
                ? new VideoExportOptions(null, 30, null)
                : videoOptions;
    }

    public enum Output {
        CLEAN_VIDEO,
        THEATRE_MAP
    }
}
