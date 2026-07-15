package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.nio.file.Path;

/** Copies and manages voice samples in controlled DocuPodcast folders. */
public interface VoiceSampleRepository {
    ProjectAssetReference importSample(Path projectFile, Path sourceAudioFile, String assetId, String displayName, String purpose, String notes) throws IOException;

    /**
     * Returns true when imported reference samples are stored in the application voice library instead of the project folder.
     * Project files may keep voice metadata, but the reusable sample audio remains application-level.
     */
    default boolean storesSamplesOutsideProject() {
        return false;
    }

    /**
     * Returns a safe project-file surrogate when samples are stored at application level.
     * Legacy project-local repositories still require a real project file.
     */
    default Path effectiveProjectFile(Path projectFile) throws IOException {
        if (projectFile == null || projectFile.getParent() == null) {
            throw new IOException("Guarda el proyecto antes de operar muestras de voz");
        }
        return projectFile;
    }

    /** Directory for temporary microphone recordings before they are imported as voice samples. */
    default Path temporaryRecordingDirectory(Path projectFile) throws IOException {
        Path effective = effectiveProjectFile(projectFile);
        return effective.toAbsolutePath().normalize().getParent().resolve("voices/tmp-recordings").normalize();
    }

    /** URI/path stored in {@code VoiceReferenceSample.fileUri()} after import. */
    default String referenceUriForImportedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException {
        return sampleAsset.relativePath();
    }

    /** Resolves a managed voice sample. Implementations may resolve either project-level or application-level samples. */
    Path resolveManagedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException;

    /** Copies a managed sample to a user-selected folder without moving or deleting the managed original. */
    Path downloadSample(Path projectFile, ProjectAssetReference sampleAsset, Path targetDirectory, String preferredFileName) throws IOException;

    /** Deletes a managed sample file only when it belongs to a controlled project folder. */
    boolean deleteManagedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException;
}
