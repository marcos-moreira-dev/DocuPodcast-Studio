package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Request for the user-facing MP4 export flow. */
public record FinalVideoExportRequest(
        DocuPodcastProject project,
        RenderUnitPlan renderUnitPlan,
        NarrationScriptDocument script,
        StoryboardDocument storyboard,
        List<AudioJobSnapshot> jobs,
        Path projectDirectory,
        Path targetMp4,
        SimpleVideoExportSettings settings,
        Path applicationRoot,
        Path configuredFfmpeg
) {
    public FinalVideoExportRequest {
        projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
        targetMp4 = Objects.requireNonNull(targetMp4, "targetMp4").toAbsolutePath().normalize();
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
        settings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        applicationRoot = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        configuredFfmpeg = configuredFfmpeg == null ? null : configuredFfmpeg.toAbsolutePath().normalize();
    }
}
