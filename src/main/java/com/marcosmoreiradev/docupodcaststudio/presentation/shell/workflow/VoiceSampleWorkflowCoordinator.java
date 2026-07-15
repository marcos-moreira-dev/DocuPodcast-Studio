package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.recording.RecordingActionPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceGeneratedTestRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceGeneratedTestResult;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceRegistrationWizardPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleImportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleImportResult;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleDownloadRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleDownloadResult;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleDeleteResult;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Coordinates voice sample and generated-test workflows outside the shell view-model. */
public final class VoiceSampleWorkflowCoordinator {
    private static final String OWN_VOICE_ID = "VOC-OWN-PLACEHOLDER";
    private final ApplicationServices applicationServices;

    public VoiceSampleWorkflowCoordinator(ApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public VoiceRegistrationWizardPlan registrationPlan(VoiceLibrary library, VoiceProfile voice) {
        VoiceProfile target = targetVoice(library, voice);
        return applicationServices.voice().buildVoiceRegistrationWizardPlan().build(target == null ? "Voz avanzada" : target.displayName(), true);
    }

    public VoiceToneRecordingPlan toneRecordingPlan(VoiceLibrary library, VoiceProfile voice, VoiceReferenceTone tone) {
        VoiceProfile target = targetVoice(library, voice);
        return applicationServices.voice().buildVoiceToneRecordingPlan().build(
                target == null ? OWN_VOICE_ID : target.id(),
                target == null ? "Voz avanzada" : target.displayName(),
                normalizeTone(tone));
    }

    public VoiceSampleImportResult importToneSample(DocuPodcastProject project, VoiceLibrary library, Path projectFile,
                                                    Path sourceAudioFile, String displayName, VoiceToneRecordingPlan plan) throws IOException {
        return importToneSample(project, library, projectFile, sourceAudioFile, displayName, plan, false);
    }

    public VoiceSampleImportResult importRecordedToneSample(DocuPodcastProject project, VoiceLibrary library, Path projectFile,
                                                            Path sourceAudioFile, String displayName, VoiceToneRecordingPlan plan) throws IOException {
        return importToneSample(project, library, projectFile, sourceAudioFile, displayName, plan, true);
    }

    private VoiceSampleImportResult importToneSample(DocuPodcastProject project, VoiceLibrary library, Path projectFile,
                                                     Path sourceAudioFile, String displayName, VoiceToneRecordingPlan plan,
                                                     boolean recordedInJava) throws IOException {
        VoiceReferenceTone tone = plan == null ? VoiceReferenceTone.NEUTRAL : plan.tone();
        String voiceId = plan == null ? OWN_VOICE_ID : plan.voiceProfileId();
        VoiceSampleImportRequest request = recordedInJava
                ? VoiceSampleImportRequest.forRecordedVoiceTone(voiceId, sourceAudioFile, displayName, tone)
                : VoiceSampleImportRequest.forOwnVoiceTone(voiceId, sourceAudioFile, displayName, tone);
        Path effectiveProjectFile = applicationServices.voice().importVoiceSample().effectiveProjectFile(projectFile);
        return applicationServices.voice().importVoiceSample().importSample(project, library, effectiveProjectFile, request);
    }


    public VoiceSampleDownloadResult downloadToneSample(DocuPodcastProject project, Path projectFile,
                                                        VoiceReferenceSample sample, Path targetDirectory) throws IOException {
        String fileName = sample.tone().name().toLowerCase(java.util.Locale.ROOT) + "-" + sample.id() + ".wav";
        java.util.Optional<ProjectAssetReference> asset = sampleAssetOptional(project, sample);
        if (asset.isPresent()) {
            return applicationServices.voice().downloadVoiceReferenceSample()
                    .download(new VoiceSampleDownloadRequest(projectFile, asset.get(), targetDirectory, fileName));
        }
        Path source = resolveSamplePath(projectFile, sample);
        if (!java.nio.file.Files.isDirectory(targetDirectory)) {
            throw new IOException("Selecciona una carpeta válida para descargar la muestra de voz.");
        }
        Path target = targetDirectory.toAbsolutePath().normalize().resolve(fileName).normalize();
        if (!target.startsWith(targetDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Destino inválido para descargar muestra de voz.");
        }
        java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return new VoiceSampleDownloadResult(target, "Muestra descargada correctamente desde la biblioteca de voces de la app.");
    }

    public VoiceSampleDeleteResult deleteToneSample(DocuPodcastProject project, Path projectFile,
                                                    VoiceReferenceSample sample) throws IOException {
        java.util.Optional<ProjectAssetReference> asset = sampleAssetOptional(project, sample);
        if (asset.isPresent()) {
            return applicationServices.voice().deleteVoiceReferenceSample().delete(projectFile, sample, asset.get());
        }
        if (!sample.canDeleteManagedFile()) {
            return VoiceSampleDeleteResult.skipped("DocuPodcast quitará la referencia, pero no borrará el archivo externo original.");
        }
        Path resolved = resolveSamplePath(projectFile, sample);
        boolean deleted = java.nio.file.Files.deleteIfExists(resolved);
        return deleted ? VoiceSampleDeleteResult.deleted(resolved)
                : VoiceSampleDeleteResult.skipped("La muestra ya no existe en la biblioteca de voces de la app.");
    }


    public VoiceReferenceSample requireExactSample(VoiceLibrary library, VoiceProfile voice, VoiceReferenceTone tone) throws IOException {
        VoiceProfile target = targetVoice(library, voice);
        if (target == null) {
            throw new IOException("Selecciona una voz antes de operar una muestra.");
        }
        VoiceReferenceTone targetTone = normalizeTone(tone);
        return library.referenceSampleSetByVoiceId(target.id())
                .flatMap(set -> set.sampleFor(targetTone))
                .orElseThrow(() -> new IOException("No hay muestra registrada para " + target.displayName() + " en tono " + targetTone.displayName() + "."));
    }

    public Path resolveSamplePath(Path projectFile, VoiceReferenceSample sample) throws IOException {
        Path raw = Path.of(sample.fileUri());
        if (!raw.isAbsolute()) {
            Path effectiveProjectFile = applicationServices.voice().importVoiceSample().effectiveProjectFile(projectFile);
            Path root = effectiveProjectFile.toAbsolutePath().normalize().getParent();
            if (root == null) {
                throw new IOException("La ruta de la biblioteca de voces no tiene carpeta contenedora.");
            }
            return VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot()
                    .resolve(root, sample, "muestra de voz");
        }
        if (!java.nio.file.Files.isRegularFile(raw.normalize())) {
            throw new IOException("La muestra de voz registrada no existe: " + raw.normalize());
        }
        return raw.normalize();
    }

    public VoiceLibrary removeReferenceSample(VoiceLibrary library, VoiceReferenceSample sample) {
        if (library == null || sample == null) {
            return library;
        }
        java.util.Optional<VoiceReferenceSampleSet> current = library.referenceSampleSetByVoiceId(sample.voiceProfileId());
        if (current.isEmpty()) {
            return library;
        }
        VoiceReferenceSampleSet updatedSet = current.get().withoutTone(sample.tone());
        VoiceLibrary updated = updatedSet.samples().isEmpty()
                ? library.withoutReferenceSampleSet(sample.voiceProfileId())
                : library.withReferenceSampleSet(updatedSet);
        if (sample.isNeutral()) {
            java.util.Optional<VoiceProfile> voice = updated.voiceById(sample.voiceProfileId());
            if (voice.isPresent() && sample.id().equals(voice.get().sampleAssetId())) {
                VoiceProfile existing = voice.get();
                updated = updated.withVoice(new VoiceProfile(existing.id(), existing.displayName(),
                        existing.type(), existing.engineType(), existing.language(), "", existing.modelAssetId(),
                        existing.qualityPreset(), existing.supportsStyleTransfer(), existing.consentNote(), existing.metadata()));
            }
        }
        return updated;
    }

    public String displayNameForVoice(VoiceProfile voice) {
        return voice == null ? "la voz seleccionada" : voice.displayName();
    }

    private static java.util.Optional<ProjectAssetReference> sampleAssetOptional(DocuPodcastProject project, VoiceReferenceSample sample) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(sample, "sample");
        return project.assets().byId(sample.id());
    }

    public Path startRecording(Path projectFile, VoiceToneRecordingPlan plan) throws IOException {
        return startRecording(projectFile, plan, "");
    }

    public Path startRecording(Path projectFile, VoiceToneRecordingPlan plan, String inputDeviceId) throws IOException {
        VoiceReferenceTone tone = plan == null ? VoiceReferenceTone.NEUTRAL : plan.tone();
        String voiceId = plan == null ? OWN_VOICE_ID : plan.voiceProfileId();
        RecordingActionPlan actionPlan = applicationServices.recording().prepareRecordingAction()
                .prepare(RecordingPurpose.VOICE_SAMPLE_FOR_TTS, new ScriptTextRange(voiceId + "-" + tone.name(), 0, 0));
        Path recordingDirectory = applicationServices.voice().importVoiceSample().temporaryRecordingDirectory(projectFile);
        return applicationServices.recording().startAudioRecording().startInDirectory(recordingDirectory, actionPlan, inputDeviceId);
    }

    public VoiceGeneratedTestResult generateTest(Path projectFile, VoiceLibrary library, VoiceProfile voice,
                                                 VoiceReferenceTone tone, String phrase, AudioEngineDescriptor engine) throws IOException {
        if (voice == null) {
            throw new IOException("Selecciona una voz antes de generar una prueba.");
        }
        return applicationServices.voice().generateVoiceTest()
                .generate(projectFile, library, new VoiceGeneratedTestRequest(voice.id(), tone, phrase, engine));
    }

    private VoiceProfile targetVoice(VoiceLibrary library, VoiceProfile voice) {
        return voice != null ? voice : library.voiceById(OWN_VOICE_ID).orElse(null);
    }

    private static VoiceReferenceTone normalizeTone(VoiceReferenceTone tone) {
        return tone == null ? VoiceReferenceTone.NEUTRAL : tone;
    }
}
