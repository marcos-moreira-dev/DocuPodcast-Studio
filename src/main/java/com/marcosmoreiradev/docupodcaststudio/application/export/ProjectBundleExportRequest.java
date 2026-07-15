package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Request for a portable project bundle export. */
public record ProjectBundleExportRequest(
        DocuPodcastProject project,
        Path projectFile,
        NarrationScriptDocument script,
        StoryboardDocument storyboard,
        List<AudioJobSnapshot> audioJobs,
        Path targetDirectory
) {
    public ProjectBundleExportRequest {
        project = Objects.requireNonNull(project, "project");
        projectFile = Objects.requireNonNull(projectFile, "projectFile");
        audioJobs = audioJobs == null ? List.of() : List.copyOf(audioJobs);
        targetDirectory = Objects.requireNonNull(targetDirectory, "targetDirectory");
    }

    public Path projectDirectory() {
        return projectFile.toAbsolutePath().normalize().getParent();
    }
}
