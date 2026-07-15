package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.nio.file.Path;
import java.util.Objects;

/** Request to export/download a managed voice sample to a user-selected folder. */
public record VoiceSampleDownloadRequest(
        Path projectFile,
        ProjectAssetReference sampleAsset,
        Path targetDirectory,
        String preferredFileName
) {
    public VoiceSampleDownloadRequest {
        projectFile = Objects.requireNonNull(projectFile, "projectFile");
        sampleAsset = Objects.requireNonNull(sampleAsset, "sampleAsset");
        targetDirectory = Objects.requireNonNull(targetDirectory, "targetDirectory");
        preferredFileName = preferredFileName == null ? "" : preferredFileName.strip();
    }
}
