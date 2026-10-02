package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;

import java.io.IOException;
import java.nio.file.Path;

public interface TheatreAssetStager {
    StagedTheatreAsset stage(Path sourceRoot, Path projectFile, Path transactionRoot,
                             TheatrePackageEntry entry) throws IOException;
    void publish(StagedTheatreAsset staged) throws IOException;
    void rollbackPublished(StagedTheatreAsset staged) throws IOException;
}
