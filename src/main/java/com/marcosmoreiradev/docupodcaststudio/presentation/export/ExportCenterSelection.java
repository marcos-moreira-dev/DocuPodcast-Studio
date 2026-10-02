package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreMapExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatrePortionExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;

import java.util.Objects;

/** Selection made in the central export dialog. */
public record ExportCenterSelection(
        ExportCenterAction action,
        AppCommandId commandId,
        ExportExecutionMode executionMode,
        AudioExportFormat audioFormat,
        DocumentTextVideoOptions documentTextVideoOptions,
        VideoExportOptions videoOptions,
        TheatreMapExportOptions theatreMapOptions,
        TheatrePortionExportOptions theatrePortionOptions
) {
    public ExportCenterSelection(ExportCenterAction action, AppCommandId commandId) {
        this(action, commandId, ExportExecutionMode.READY_ONLY, AudioExportFormat.WAV,
                DocumentTextVideoOptions.defaults(), null, null, null);
    }

    public ExportCenterSelection(ExportCenterAction action,
                                 AppCommandId commandId,
                                 AudioExportFormat audioFormat,
                                 DocumentTextVideoOptions documentTextVideoOptions) {
        this(action, commandId, ExportExecutionMode.READY_ONLY, audioFormat,
                documentTextVideoOptions, null, null, null);
    }

    public ExportCenterSelection {
        action = Objects.requireNonNull(action, "action");
        executionMode = executionMode == null ? ExportExecutionMode.READY_ONLY : executionMode;
        audioFormat = audioFormat == null ? AudioExportFormat.WAV : audioFormat;
        documentTextVideoOptions = documentTextVideoOptions == null ? DocumentTextVideoOptions.defaults() : documentTextVideoOptions;
    }
}
