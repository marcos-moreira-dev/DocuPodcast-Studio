package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.application.compatibility.voice.LegacyVoiceProfileEngineCompatibility;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.util.Map;

/** Coordinates sober create/update/delete operations for the Voices workspace. */
public final class VoiceProfileAdministrationCoordinator {
    public SaveResult save(ProjectSession session, VoiceLibrary library, String voiceId, String displayName) throws IOException {
        String name = displayName == null ? "" : displayName.strip();
        if (name.isBlank()) {
            throw new IOException("Escribe el nombre de la voz antes de guardarla.");
        }
        String id = voiceId == null || voiceId.isBlank() ? nextVoiceProfileId(library, name) : voiceId.strip();
        VoiceProfile existing = library.voiceById(id).orElse(null);
        VoiceProfile updatedVoice = new VoiceProfile(
                id,
                name,
                VoiceProfileType.OWN,
                LegacyVoiceProfileEngineCompatibility.advancedEngineTypeAlias(),
                existing == null ? "es" : existing.language(),
                existing == null ? "" : existing.sampleAssetId(),
                existing == null ? "" : existing.modelAssetId(),
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Voz creada por el usuario para Voz IA avanzada; sus emociones se guardan como muestras de referencia.",
                existing == null ? Map.of("userManaged", "true") : existing.metadata());
        VoiceLibrary updatedLibrary = library.withVoice(updatedVoice);
        return new SaveResult(session.project().withVoiceLibrary(updatedLibrary), updatedLibrary, updatedVoice);
    }

    public DeleteResult delete(ProjectSession session, VoiceLibrary library, VoiceProfile voice,
                               VoiceSampleWorkflowCoordinator sampleWorkflow) throws IOException {
        if (voice == null) {
            throw new IOException("Selecciona una voz antes de eliminarla.");
        }
        if (protectedVoiceProfile(voice)) {
            throw new IOException("Esta voz es parte del programa o del motor activo y no se puede eliminar desde Gestionar voces.");
        }
        var projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de eliminar una voz."));
        DocuPodcastProject updatedProject = session.project();
        int removedSamples = 0;
        var sampleSet = library.referenceSampleSetByVoiceId(voice.id());
        if (sampleSet.isPresent()) {
            for (VoiceReferenceSample sample : sampleSet.get().samples()) {
                try {
                    sampleWorkflow.deleteToneSample(updatedProject, projectFile, sample);
                } catch (IOException ignored) {
                    // La voz se elimina de la biblioteca aunque un archivo gestionado ya no exista.
                }
                updatedProject = updatedProject.withoutAsset(sample.id());
                removedSamples++;
            }
        }
        VoiceLibrary updatedLibrary = library.withoutVoice(voice.id());
        updatedProject = updatedProject.withVoiceLibrary(updatedLibrary);
        return new DeleteResult(updatedProject, updatedLibrary, removedSamples);
    }

    public String impactLabel(VoiceLibrary library, VoiceProfile voice) {
        if (voice == null) {
            return "Selecciona una voz para revisar qué se eliminará.";
        }
        int samples = library.referenceSampleSetByVoiceId(voice.id())
                .map(set -> set.samples().size())
                .orElse(0);
        return "Eliminar voz \"" + voice.displayName() + "\" también eliminará " + samples
                + " muestra(s) de audio asociada(s) y sus emociones registradas.";
    }

    private static boolean protectedVoiceProfile(VoiceProfile voice) {
        return voice.metadata().containsKey("builtIn")
                || voice.metadata().containsKey("builtInAdvancedReference")
                || voice.metadata().containsKey("officialPreset")
                || voice.metadata().containsKey("userVoiceSlot");
    }

    private static String nextVoiceProfileId(VoiceLibrary library, String displayName) {
        String base = "VOC-" + (displayName == null ? "VOZ" : displayName)
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.equals("VOC-")) {
            base = "VOC-VOZ";
        }
        String candidate = base;
        int suffix = 2;
        while (library.voiceById(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    public record SaveResult(DocuPodcastProject project, VoiceLibrary library, VoiceProfile voice) {
    }

    public record DeleteResult(DocuPodcastProject project, VoiceLibrary library, int removedSamples) {
    }
}
