package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.nio.file.Path;

/** Stores user-selected media inside the project container as portable assets. */
public interface UserMediaAssetRepository {
    ProjectAssetReference importAudioClip(Path projectFile, Path sourceAudioFile, String assetId,
                                          String displayName, String purpose, String notes) throws IOException;

    ProjectAssetReference importVideoSource(Path projectFile, Path sourceVideoFile, String assetId,
                                            String displayName, String purpose, String notes) throws IOException;

    Path prepareDerivedAudioTarget(Path projectFile, String assetId, String sourceDisplayName) throws IOException;

    ProjectAssetReference registerDerivedAudioClip(Path projectFile, Path derivedAudioFile, String assetId,
                                                   String displayName, String purpose, String notes) throws IOException;

    default Path preparePendingAudioTarget(Path projectFile, String token, String sourceDisplayName) throws IOException {
        throw new IOException("Pending audio imports are not supported by this repository");
    }

    default void copyPendingAudio(Path sourceAudioFile, Path pendingTarget) throws IOException {
        throw new IOException("Pending audio imports are not supported by this repository");
    }

    default Path commitPendingAudio(Path projectFile, Path pendingFile, String assetId, String displayName) throws IOException {
        throw new IOException("Pending audio imports are not supported by this repository");
    }

    default void discardPendingAudio(Path projectFile, Path pendingFile) throws IOException {
        java.nio.file.Files.deleteIfExists(pendingFile);
    }

    default boolean deleteProjectAudioFile(Path projectFile, Path audioFile) throws IOException {
        return java.nio.file.Files.deleteIfExists(audioFile);
    }
}
