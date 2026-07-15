package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;
import java.util.List;

/** Runtime status for a local theatre image generation batch or candidate job. */
public record TheatreImageGenerationJob(
        String id,
        TheatreFrameGenerationScope scope,
        FrameGenerationMode mode,
        TheatreImageGenerationPreset preset,
        Path outputDirectory,
        List<TheatreImageGenerationJobItem> items,
        List<String> appliedReferences,
        String status,
        boolean running) {
    public TheatreImageGenerationJob {
        id = id == null ? "" : id;
        scope = scope == null ? TheatreFrameGenerationScope.all() : scope;
        mode = mode == null ? FrameGenerationMode.SINGLE : mode;
        preset = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        items = items == null ? List.of() : List.copyOf(items);
        appliedReferences = appliedReferences == null ? List.of() : List.copyOf(appliedReferences);
        status = status == null ? "" : status;
    }

    public TheatreImageGenerationJob(String id,
                                     TheatreFrameGenerationScope scope,
                                     FrameGenerationMode mode,
                                     TheatreImageGenerationPreset preset,
                                     Path outputDirectory,
                                     List<TheatreImageGenerationJobItem> items,
                                     String status,
                                     boolean running) {
        this(id, scope, mode, preset, outputDirectory, items, List.of(), status, running);
    }

    public TheatreImageGenerationJob withStatus(String newStatus, boolean newRunning) {
        return new TheatreImageGenerationJob(id, scope, mode, preset, outputDirectory, items,
                appliedReferences, newStatus, newRunning);
    }
}
