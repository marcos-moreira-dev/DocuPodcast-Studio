package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Imports a local audio file as a voice reference sample and links it to an existing voice profile. */
public final class ImportVoiceSampleUseCase {
    private final VoiceSampleRepository repository;

    public ImportVoiceSampleUseCase(VoiceSampleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VoiceSampleImportResult importSample(DocuPodcastProject project,
                                                VoiceLibrary library,
                                                Path projectFile,
                                                VoiceSampleImportRequest request) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(request, "request");
        Path effectiveProjectFile = repository.effectiveProjectFile(projectFile);
        if (!Files.isRegularFile(request.sourceAudioFile())) {
            throw new IOException("La muestra de voz no existe o no es un archivo: " + request.sourceAudioFile());
        }
        if (!request.supportedAudioExtension()) {
            throw new IOException("Formato de muestra de voz no soportado: ." + request.normalizedExtension());
        }
        VoiceProfile existing = library.voiceById(request.targetVoiceProfileId())
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil de voz: " + request.targetVoiceProfileId()));
        String consentNote = request.consentNote().isBlank() ? existing.consentNote() : request.consentNote();
        String assetId = "VOICE-SAMPLE-" + sanitizeId(existing.id()) + "-" + request.tone().name();
        String displayName = request.displayName().isBlank()
                ? existing.displayName() + " — muestra " + request.tone().displayName().toLowerCase(java.util.Locale.ROOT)
                : request.displayName();
        ProjectAssetReference asset = repository.importSample(
                effectiveProjectFile,
                request.sourceAudioFile(),
                assetId,
                displayName,
                "Muestra de voz " + request.tone().displayName() + " para " + existing.id(),
                consentNote
        );
        boolean applicationManaged = repository.storesSamplesOutsideProject();
        VoiceReferenceSample referenceSample = new VoiceReferenceSample(
                asset.id(),
                existing.id(),
                request.tone(),
                repository.referenceUriForImportedSample(effectiveProjectFile, asset),
                request.origin(),
                applicationManaged ? VoiceFileOwnership.USER_APPDATA : VoiceFileOwnership.PROJECT_ASSET,
                0,
                Instant.now(),
                consentNote
        );
        String legacySampleAssetId = request.tone().isNeutral() ? asset.id() : existing.sampleAssetId();
        VoiceProfile updatedVoice = new VoiceProfile(
                existing.id(),
                existing.displayName(),
                request.profileType(),
                existing.engineType() == VoiceEngineType.UNKNOWN ? VoiceEngineType.HUMAN_AUDIO : existing.engineType(),
                existing.language(),
                legacySampleAssetId,
                existing.modelAssetId(),
                existing.qualityPreset() == VoiceQualityPreset.BALANCED ? VoiceQualityPreset.HUMAN_REFERENCE : existing.qualityPreset(),
                existing.supportsStyleTransfer(),
                consentNote,
                mergeMetadata(existing.metadata(), request)
        );
        VoiceLibrary updatedLibrary = library.withVoice(updatedVoice).withReferenceSample(referenceSample);
        DocuPodcastProject updatedProject = project.withVoiceLibrary(updatedLibrary);
        if (!applicationManaged) {
            updatedProject = updatedProject.withAsset(asset);
        }
        return new VoiceSampleImportResult(updatedProject, updatedLibrary, updatedVoice, asset, referenceSample);
    }

    public Path effectiveProjectFile(Path projectFile) throws IOException {
        return repository.effectiveProjectFile(projectFile);
    }

    public Path temporaryRecordingDirectory(Path projectFile) throws IOException {
        return repository.temporaryRecordingDirectory(projectFile);
    }

    public boolean storesSamplesOutsideProject() {
        return repository.storesSamplesOutsideProject();
    }

    private static Map<String, String> mergeMetadata(Map<String, String> current, VoiceSampleImportRequest request) {
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>(current == null ? Map.of() : current);
        metadata.put("sampleImported", "true");
        metadata.put("sampleExtension", request.normalizedExtension());
        metadata.put("sampleOrigin", request.origin().name());
        metadata.put("sampleTone", request.tone().name());
        metadata.put("sampleStorage", "application-voice-library");
        return Map.copyOf(metadata);
    }

    private static String sanitizeId(String id) {
        String cleaned = id == null ? "VOICE" : id.replaceAll("[^A-Za-z0-9_-]", "-");
        return cleaned.isBlank() ? "VOICE" : cleaned;
    }
}
