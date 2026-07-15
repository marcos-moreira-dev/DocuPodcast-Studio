package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.nio.file.Path;

/** Copies user-selected image assets into media/images/ beside the project file. */
public interface ImageAssetRepository {
    ProjectAssetReference importImage(Path projectFile, Path sourceImageFile, String assetId, String displayName, String purpose, String notes) throws IOException;
}
