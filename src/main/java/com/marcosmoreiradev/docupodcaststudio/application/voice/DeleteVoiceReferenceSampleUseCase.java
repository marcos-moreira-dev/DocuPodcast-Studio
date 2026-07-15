package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Deletes only voice sample files owned and managed by DocuPodcast. */
public final class DeleteVoiceReferenceSampleUseCase {
    private final VoiceSampleRepository repository;

    public DeleteVoiceReferenceSampleUseCase(VoiceSampleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VoiceSampleDeleteResult delete(Path projectFile,
                                          VoiceReferenceSample sample,
                                          ProjectAssetReference sampleAsset) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(sample, "sample");
        Objects.requireNonNull(sampleAsset, "sampleAsset");
        if (sampleAsset.kind() != ProjectAssetKind.VOICE_SAMPLE) {
            return VoiceSampleDeleteResult.skipped("El recurso seleccionado no es una muestra de voz.");
        }
        if (!sample.canDeleteManagedFile()) {
            return VoiceSampleDeleteResult.skipped("DocuPodcast quitará la referencia, pero no borrará el archivo externo original.");
        }
        Path managedFile = repository.resolveManagedSample(projectFile, sampleAsset);
        boolean deleted = repository.deleteManagedSample(projectFile, sampleAsset);
        return deleted ? VoiceSampleDeleteResult.deleted(managedFile)
                : VoiceSampleDeleteResult.skipped("La muestra ya no existe en la carpeta gestionada.");
    }
}
