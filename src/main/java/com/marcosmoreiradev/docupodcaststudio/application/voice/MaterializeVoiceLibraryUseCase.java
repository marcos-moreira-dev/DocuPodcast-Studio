package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Writes the voice library snapshot and registers it as a relative asset. */
public final class MaterializeVoiceLibraryUseCase {
    private final VoiceLibraryWorkspaceRepository repository;

    public MaterializeVoiceLibraryUseCase(VoiceLibraryWorkspaceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocuPodcastProject materialize(DocuPodcastProject project, VoiceLibrary voiceLibrary, Path projectFile) throws IOException {
        MaterializedVoiceLibrary materialized = repository.materialize(voiceLibrary, projectFile);
        return project.withVoiceLibrary(voiceLibrary).withAsset(new ProjectAssetReference(
                "VOICE-LIBRARY-001",
                ProjectAssetKind.VOICE_LIBRARY,
                materialized.displayName(),
                materialized.relativePath(),
                "application/json",
                "Biblioteca de voces, personajes y estilos del proyecto",
                "",
                "Snapshot generado por DocuPodcast Studio"
        ));
    }
}
