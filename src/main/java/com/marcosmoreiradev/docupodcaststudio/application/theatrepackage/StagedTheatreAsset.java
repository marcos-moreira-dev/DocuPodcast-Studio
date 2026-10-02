package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;

import java.nio.file.Path;
import java.util.Objects;

public record StagedTheatreAsset(
        TheatrePackageEntry entry,
        ProjectAssetReference reference,
        Path stagedFile,
        Path targetFile,
        boolean targetExisted
) {
    public StagedTheatreAsset {
        entry = Objects.requireNonNull(entry, "entry");
        reference = Objects.requireNonNull(reference, "reference");
        stagedFile = Objects.requireNonNull(stagedFile, "stagedFile");
        targetFile = Objects.requireNonNull(targetFile, "targetFile");
    }
}
