package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;

/** Encoding and companion options for a spatial theatre-map export. */
public record TheatreMapExportOptions(
        VideoExportOptions videoOptions,
        TheatreMapCompanionMode companionMode
) {
    public TheatreMapExportOptions {
        videoOptions = videoOptions == null ? new VideoExportOptions(null, 30, null) : videoOptions;
        companionMode = companionMode == null ? TheatreMapCompanionMode.FRAGMENT_VISUALS : companionMode;
    }
}
