package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreMapExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatrePortionExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;

import java.util.Objects;

/** Selection made in the central export dialog. */
public record ExportCenterSelection(
        ExportCenterAction action,
        AppCommandId commandId,
        AudioExportFormat audioFormat,
        DocumentTextVideoOptions documentTextVideoOptions,
        VideoExportOptions videoOptions,
        TheatreMapExportOptions theatreMapOptions,
        TheatrePortionExportOptions theatrePortionOptions
) {
    public ExportCenterSelection(ExportCenterAction action, AppCommandId commandId) {
        this(action, commandId, AudioExportFormat.WAV, DocumentTextVideoOptions.defaults(), null, null, null);
    }

    public ExportCenterSelection(ExportCenterAction action,
                                 AppCommandId commandId,
                                 AudioExportFormat audioFormat,
                                 DocumentTextVideoOptions documentTextVideoOptions) {
        this(action, commandId, audioFormat, documentTextVideoOptions, null, null, null);
    }

    public ExportCenterSelection {
        action = Objects.requireNonNull(action, "action");
        audioFormat = audioFormat == null ? AudioExportFormat.WAV : audioFormat;
        documentTextVideoOptions = documentTextVideoOptions == null ? DocumentTextVideoOptions.defaults() : documentTextVideoOptions;
    }
}
