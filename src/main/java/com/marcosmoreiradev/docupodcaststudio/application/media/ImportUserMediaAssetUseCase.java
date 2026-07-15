package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioDurationProbe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Imports user media for narrative audio layers.
 *
 * <p>WAV is copied as an audio clip. MP3/M4A/FLAC/OGG can be normalized
 * to WAV through FFmpeg when a normalizer is configured. Supported video files
 * are accepted only to extract their audio track through FFmpeg or another configured gateway. The
 * original video is registered as provenance, while the derived WAV becomes the
 * assignable AUDIO_CLIP.</p>
 */
public final class ImportUserMediaAssetUseCase {
    private final UserMediaAssetRepository repository;
    private final VideoAudioExtractionGateway videoAudioExtractionGateway;
    private final AudioNormalizationGateway audioNormalizationGateway;
    private final UserMediaFormatPolicy formatPolicy;
    private final AudioDurationProbe durationProbe;

    public ImportUserMediaAssetUseCase(UserMediaAssetRepository repository,
                                       VideoAudioExtractionGateway videoAudioExtractionGateway) {
        this(repository, videoAudioExtractionGateway, null, new UserMediaFormatPolicy(), null);
    }

    public ImportUserMediaAssetUseCase(UserMediaAssetRepository repository,
                                       VideoAudioExtractionGateway videoAudioExtractionGateway,
                                       AudioNormalizationGateway audioNormalizationGateway) {
        this(repository, videoAudioExtractionGateway, audioNormalizationGateway, new UserMediaFormatPolicy(), null);
    }

    public ImportUserMediaAssetUseCase(UserMediaAssetRepository repository,
                                       VideoAudioExtractionGateway videoAudioExtractionGateway,
                                       UserMediaFormatPolicy formatPolicy) {
        this(repository, videoAudioExtractionGateway, null, formatPolicy, null);
    }

    public ImportUserMediaAssetUseCase(UserMediaAssetRepository repository,
                                       VideoAudioExtractionGateway videoAudioExtractionGateway,
                                       AudioNormalizationGateway audioNormalizationGateway,
                                       UserMediaFormatPolicy formatPolicy) {
        this(repository, videoAudioExtractionGateway, audioNormalizationGateway, formatPolicy, null);
    }

    public ImportUserMediaAssetUseCase(UserMediaAssetRepository repository,
                                       VideoAudioExtractionGateway videoAudioExtractionGateway,
                                       AudioNormalizationGateway audioNormalizationGateway,
                                       UserMediaFormatPolicy formatPolicy,
                                       AudioDurationProbe durationProbe) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.videoAudioExtractionGateway = Objects.requireNonNull(videoAudioExtractionGateway, "videoAudioExtractionGateway");
        this.audioNormalizationGateway = audioNormalizationGateway;
        this.formatPolicy = Objects.requireNonNull(formatPolicy, "formatPolicy");
        this.durationProbe = durationProbe;
    }

    public PreparedAudioAsset prepareAudio(Path projectFile, Path sourceAudioFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(sourceAudioFile, "sourceAudioFile");
        if (projectFile.getParent() == null) throw new IOException("Guarda el proyecto antes de preparar una pista de audio.");
        if (!Files.isRegularFile(sourceAudioFile)) throw new IOException("El archivo de audio no existe: " + sourceAudioFile);
        if (formatPolicy.classify(sourceAudioFile) != UserMediaAssetKind.AUDIO_FILE) {
            throw new IOException("Selecciona un archivo de audio compatible. " + formatPolicy.supportedFormatSummary());
        }
        String token = java.util.UUID.randomUUID().toString();
        String displayName = displayName(sourceAudioFile);
        Path pending = repository.preparePendingAudioTarget(projectFile, token, displayName);
        boolean normalized = formatPolicy.audioRequiresNormalization(sourceAudioFile);
        try {
            if (normalized) {
                if (audioNormalizationGateway == null || !audioNormalizationGateway.ready()) {
                    throw new IOException("Para importar este formato se requiere FFmpeg configurado."
                            + (audioNormalizationGateway == null ? "" : " " + audioNormalizationGateway.readinessMessage()));
                }
                AudioNormalizationResult result = audioNormalizationGateway.normalize(
                        sourceAudioFile, pending, AudioNormalizationProfile.ASSIGNABLE_AUDIO);
                if (!result.successful() || !Files.isRegularFile(pending)) {
                    throw new IOException("No se pudo preparar el audio seleccionado: " + result.message());
                }
            } else {
                repository.copyPendingAudio(sourceAudioFile, pending);
            }
            if (durationProbe == null) throw new IOException("No hay un medidor de duracion de audio configurado.");
            double duration = durationProbe.durationSeconds(pending);
            return new PreparedAudioAsset(token, pending, displayName, duration, normalized);
        } catch (IOException | RuntimeException ex) {
            try { repository.discardPendingAudio(projectFile, pending); } catch (IOException ignored) { }
            throw ex;
        }
    }

    public UserMediaImportResult commitPreparedAudio(DocuPodcastProject project, Path projectFile,
                                                      PreparedAudioAsset prepared) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(prepared, "prepared");
        String prefix = prepared.normalized() ? "AUDIO-NORMALIZED" : "AUDIO-USER";
        String assetId = nextAssetId(project, prefix, ProjectAssetKind.AUDIO_CLIP);
        Path committed = repository.commitPendingAudio(projectFile, prepared.pendingPath(), assetId,
                prepared.sourceDisplayName());
        ProjectAssetReference reference = repository.registerDerivedAudioClip(projectFile, committed, assetId,
                prepared.normalized() ? stripExtension(prepared.sourceDisplayName()) + " - audio preparado"
                        : prepared.sourceDisplayName(),
                "Pista de audio teatral propiedad exclusiva del track",
                "Importada desde " + prepared.sourceDisplayName());
        DocuPodcastProject updated = project.withAsset(reference);
        return new UserMediaImportResult(updated,
                prepared.normalized() ? UserMediaImportStatus.AUDIO_NORMALIZED : UserMediaImportStatus.AUDIO_IMPORTED,
                reference, null, "Audio confirmado dentro del proyecto: " + reference.displayName());
    }

    public void discardPreparedAudio(Path projectFile, PreparedAudioAsset prepared) throws IOException {
        if (prepared != null) repository.discardPendingAudio(projectFile, prepared.pendingPath());
    }

    public boolean deleteProjectAudio(Path projectFile, Path audioFile) throws IOException {
        return repository.deleteProjectAudioFile(projectFile, audioFile);
    }

    public UserMediaImportResult importMedia(DocuPodcastProject project, Path projectFile, Path sourceMediaFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(sourceMediaFile, "sourceMediaFile");
        if (projectFile.getParent() == null) {
            throw new IOException("Guarda el proyecto en una carpeta antes de importar audio o video.");
        }
        if (!Files.isRegularFile(sourceMediaFile)) {
            throw new IOException("El archivo de media no existe o no es un archivo: " + sourceMediaFile);
        }
        UserMediaAssetKind kind = formatPolicy.classify(sourceMediaFile);
        return switch (kind) {
            case AUDIO_FILE -> importAudio(project, projectFile, sourceMediaFile);
            case VIDEO_FOR_AUDIO -> importVideoAudio(project, projectFile, sourceMediaFile);
            case UNSUPPORTED -> throw new IOException("Formato de media no soportado. " + formatPolicy.supportedFormatSummary());
        };
    }

    private UserMediaImportResult importAudio(DocuPodcastProject project, Path projectFile, Path sourceAudioFile) throws IOException {
        if (formatPolicy.audioRequiresNormalization(sourceAudioFile) && audioNormalizationGateway != null) {
            return importNormalizedAudio(project, projectFile, sourceAudioFile);
        }
        String assetId = nextAssetId(project, "AUDIO-USER", ProjectAssetKind.AUDIO_CLIP);
        String displayName = displayName(sourceAudioFile);
        ProjectAssetReference audioAsset = repository.importAudioClip(
                projectFile,
                sourceAudioFile,
                assetId,
                displayName,
                "Audio seleccionado por el usuario para asignar a un fragmento narrativo",
                "Sugerencia UX: el usuario nombra el archivo según su intención, por ejemplo Lucía hablando o pajaritos cantando.");
        DocuPodcastProject updated = project.withAsset(audioAsset);
        return new UserMediaImportResult(updated, UserMediaImportStatus.AUDIO_IMPORTED, audioAsset, null,
                "Audio importado como capa asignable: " + audioAsset.displayName());
    }

    private UserMediaImportResult importNormalizedAudio(DocuPodcastProject project, Path projectFile, Path sourceAudioFile) throws IOException {
        if (!audioNormalizationGateway.ready()) {
            throw new IOException("Para importar este formato de audio se requiere FFmpeg configurado. "
                    + audioNormalizationGateway.readinessMessage());
        }
        String assetId = nextAssetId(project, "AUDIO-NORMALIZED", ProjectAssetKind.AUDIO_CLIP);
        String displayName = displayName(sourceAudioFile);
        Path target = repository.prepareDerivedAudioTarget(projectFile, assetId, displayName);
        AudioNormalizationResult normalization = audioNormalizationGateway.normalize(
                sourceAudioFile,
                target,
                AudioNormalizationProfile.ASSIGNABLE_AUDIO);
        if (!normalization.successful() || !Files.isRegularFile(target)) {
            throw new IOException("No se pudo preparar el audio seleccionado: " + normalization.message());
        }
        ProjectAssetReference audioAsset = repository.registerDerivedAudioClip(
                projectFile,
                target,
                assetId,
                stripExtension(displayName) + " — audio preparado",
                "Audio normalizado con FFmpeg para asignar a un fragmento narrativo",
                "Derivado de " + displayName + ". " + normalization.message());
        DocuPodcastProject updated = project.withAsset(audioAsset);
        return new UserMediaImportResult(updated, UserMediaImportStatus.AUDIO_NORMALIZED, audioAsset, null,
                "Audio preparado con FFmpeg y listo como capa asignable: " + audioAsset.displayName());
    }

    private UserMediaImportResult importVideoAudio(DocuPodcastProject project, Path projectFile, Path sourceVideoFile) throws IOException {
        if (!videoAudioExtractionGateway.ready()) {
            throw new IOException("Para extraer audio de video se requiere FFmpeg configurado. " + videoAudioExtractionGateway.readinessMessage());
        }
        String videoAssetId = nextAssetId(project, "VIDEO-SOURCE", ProjectAssetKind.VIDEO_SOURCE);
        String audioAssetId = nextAssetId(project, "AUDIO-EXTRACTED", ProjectAssetKind.AUDIO_CLIP);
        String displayName = displayName(sourceVideoFile);
        Path derivedAudioTarget = repository.prepareDerivedAudioTarget(projectFile, audioAssetId, displayName);
        VideoAudioExtractionResult extraction = videoAudioExtractionGateway.extractAudio(sourceVideoFile, derivedAudioTarget);
        if (!extraction.successful() || !Files.isRegularFile(derivedAudioTarget)) {
            throw new IOException("No se pudo extraer audio del video: " + extraction.message());
        }
        ProjectAssetReference videoAsset = repository.importVideoSource(
                projectFile,
                sourceVideoFile,
                videoAssetId,
                displayName,
                "Video original importado solo como fuente para extraer audio",
                "No es un video editable; se conserva como procedencia del audio extraído.");
        ProjectAssetReference audioAsset = repository.registerDerivedAudioClip(
                projectFile,
                derivedAudioTarget,
                audioAssetId,
                stripExtension(displayName) + " — audio extraído",
                "Audio extraído desde video para asignar a un fragmento narrativo",
                "Derivado de " + videoAsset.id() + ". " + extraction.message());
        DocuPodcastProject updated = project.withAsset(videoAsset).withAsset(audioAsset);
        return new UserMediaImportResult(updated, UserMediaImportStatus.VIDEO_AUDIO_EXTRACTED, audioAsset, videoAsset,
                "Se extrajo solo el audio del video y quedó como capa asignable: " + audioAsset.displayName());
    }

    private static String nextAssetId(DocuPodcastProject project, String prefix, ProjectAssetKind countedKind) {
        long count = project.assets().references().stream()
                .filter(asset -> asset.kind() == countedKind && asset.id().startsWith(prefix))
                .count() + 1L;
        return prefix + "-" + String.format(java.util.Locale.ROOT, "%03d", count);
    }

    private static String displayName(Path file) {
        return file.getFileName() == null ? "media" : file.getFileName().toString();
    }

    private static String stripExtension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? Objects.toString(name, "media") : name.substring(0, dot);
    }
}
