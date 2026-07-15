package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.List;

/** Decides whether an export format is available from the currently loaded artifact state. */
public final class ProjectExportFormatPolicy {
    public boolean canExportPodcastWav(List<AudioJobSnapshot> jobs) {
        return new ExportPodcastWavUseCase().canExport(jobs);
    }

    public boolean canExportStoryboardPackage(NarrationScriptDocument script, StoryboardDocument storyboard) {
        return script != null && !script.empty() && storyboard != null;
    }

    public boolean canExportProjectBundle(boolean hasProjectFile) {
        return hasProjectFile;
    }

    public boolean canExportSimpleVideo(NarrationScriptDocument script) {
        return script != null && !script.empty();
    }
}
