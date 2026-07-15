package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/** Exports a managed voice sample to a folder selected by the user. */
public final class DownloadVoiceReferenceSampleUseCase {
    private final VoiceSampleRepository repository;

    public DownloadVoiceReferenceSampleUseCase(VoiceSampleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VoiceSampleDownloadResult download(VoiceSampleDownloadRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        if (request.sampleAsset().kind() != ProjectAssetKind.VOICE_SAMPLE) {
            throw new IOException("El recurso seleccionado no es una muestra de voz.");
        }
        if (!Files.isDirectory(request.targetDirectory())) {
            throw new IOException("Selecciona una carpeta válida para descargar la muestra de voz.");
        }
        var copied = repository.downloadSample(
                request.projectFile(),
                request.sampleAsset(),
                request.targetDirectory(),
                request.preferredFileName()
        );
        return new VoiceSampleDownloadResult(copied, "Muestra descargada correctamente.");
    }
}
