package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.voice.EffectiveVoiceAssignmentResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds a cross-mode audio/voice production view from the canonical fragment projection. */
public final class BuildAudioVoiceProductionProjectionUseCase {
    private static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";
    private static final EffectiveVoiceAssignmentResolver VOICE_RESOLVER =
            new EffectiveVoiceAssignmentResolver();

    public AudioVoiceProductionProjection build(
            FragmentWorkspaceProjection projection,
            NarrationScriptDocument script,
            DocuPodcastProject project,
            VoiceLibrary voiceLibrary,
            List<AudioEngineReadinessUiItem> engineReadiness
    ) {
        if (projection == null) {
            return new AudioVoiceProductionProjection(List.of(), readiness(List.of(), engineReadiness), engineReadiness);
        }
        VoiceLibrary resolvedLibrary = voiceLibrary == null
                ? (project == null ? VoiceLibrary.defaults() : project.voiceLibrary())
                : voiceLibrary;
        Map<String, NarrationSegment> segments = segmentsById(script);
        ArrayList<FragmentAudioVoiceState> states = new ArrayList<>();
        for (DocumentFragment fragment : projection.fragments()) {
            List<FragmentAssetBinding> bindings = projection.bindingsForFragment(fragment.fragmentId());
            AudioChoice audio = audioChoice(bindings);
            VoiceChoice voice = voiceChoice(fragment, bindings, segments.get(fragment.segmentId()), project, resolvedLibrary, engineReadiness);
            ArrayList<String> blockers = new ArrayList<>();
            if (fragment.hasSegment() && !audio.ready()) {
                blockers.add("Falta audio listo para el fragmento.");
            }
            if (fragment.hasSegment() && !voice.availability().usable()) {
                blockers.add(voice.message());
            }
            states.add(new FragmentAudioVoiceState(
                    fragment.fragmentId(),
                    fragment.order(),
                    fragment.segmentId(),
                    fragment.sourceBlockId(),
                    fragment.sourceLocation(),
                    fragment.hasSegment(),
                    audio.kind(),
                    audio.ready(),
                    audio.path(),
                    audio.status(),
                    audio.durationSeconds(),
                    voice.voiceId(),
                    voice.displayName(),
                    voice.availability(),
                    voice.message(),
                    blockers));
        }
        states.sort(Comparator.comparingInt(FragmentAudioVoiceState::order));
        return new AudioVoiceProductionProjection(states, readiness(states, engineReadiness), engineReadiness);
    }

    private static Map<String, NarrationSegment> segmentsById(NarrationScriptDocument script) {
        LinkedHashMap<String, NarrationSegment> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            result.put(segment.id(), segment);
        }
        return result;
    }

    private static AudioChoice audioChoice(List<FragmentAssetBinding> bindings) {
        AudioChoice recorded = firstAudio(bindings, FragmentAssetRole.AUDIO_RECORDED, AudioSourceKind.RECORDED_HUMAN);
        if (recorded.kind() != AudioSourceKind.NONE) {
            return recorded;
        }
        AudioChoice imported = firstAudio(bindings, FragmentAssetRole.AUDIO_IMPORTED, AudioSourceKind.IMPORTED_AUDIO);
        if (imported.kind() != AudioSourceKind.NONE) {
            return imported;
        }
        AudioChoice tts = firstAudio(bindings, FragmentAssetRole.AUDIO_TTS, AudioSourceKind.GENERATED_TTS);
        if (tts.kind() != AudioSourceKind.NONE) {
            return tts;
        }
        AudioChoice ambient = firstAudio(bindings, FragmentAssetRole.AMBIENT_AUDIO, AudioSourceKind.AMBIENT);
        if (ambient.kind() != AudioSourceKind.NONE) {
            return ambient;
        }
        AudioChoice playback = firstAudio(bindings, FragmentAssetRole.PLAYBACK_CUE, AudioSourceKind.GENERATED_TTS);
        if (playback.kind() != AudioSourceKind.NONE) {
            return playback;
        }
        return new AudioChoice(AudioSourceKind.NONE, false, "", "MISSING", 0.0);
    }

    private static AudioChoice firstAudio(List<FragmentAssetBinding> bindings, FragmentAssetRole role, AudioSourceKind kind) {
        for (FragmentAssetBinding binding : bindings == null ? List.<FragmentAssetBinding>of() : bindings) {
            if (binding.role() != role) {
                continue;
            }
            boolean ready = readyAudioStatus(binding.status()) && (!binding.assetPath().isBlank() || !binding.assetId().isBlank());
            return new AudioChoice(kind, ready, binding.assetPath(), binding.status(), durationSeconds(binding));
        }
        return new AudioChoice(AudioSourceKind.NONE, false, "", "", 0.0);
    }

    private static boolean readyAudioStatus(String status) {
        String normalized = normalize(status).toUpperCase(java.util.Locale.ROOT);
        return normalized.equals("READY") || normalized.equals("COMPLETED") || normalized.equals("EXPORT_READY");
    }

    private static double durationSeconds(FragmentAssetBinding binding) {
        try {
            return Double.parseDouble(binding.metadata().getOrDefault("durationSeconds", "0"));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static VoiceChoice voiceChoice(
            DocumentFragment fragment,
            List<FragmentAssetBinding> bindings,
            NarrationSegment segment,
            DocuPodcastProject project,
            VoiceLibrary library,
            List<AudioEngineReadinessUiItem> engineReadiness
    ) {
        String voiceId = VOICE_RESOLVER.resolve(
                explicitVoiceId(bindings),
                theatreVoiceId(fragment, project),
                effectiveDocumentDefaultVoice(segment, project),
                DEFAULT_NARRATOR_VOICE_ID).voiceProfileId();
        if (normalize(voiceId).isBlank()) {
            voiceId = DEFAULT_NARRATOR_VOICE_ID;
        }
        Optional<VoiceProfile> profile = library == null ? Optional.empty() : library.voiceById(voiceId);
        if (profile.isEmpty()) {
            return new VoiceChoice(voiceId, voiceId, VoiceReferenceAvailability.MISSING_PROFILE,
                    "La voz asignada no existe en la biblioteca del proyecto.");
        }
        VoiceProfile voice = profile.get();
        VoiceReferenceAvailability availability = availability(voice, engineReadiness);
        return new VoiceChoice(voice.id(), voice.displayName(), availability, messageFor(voice, availability));
    }

    private static String effectiveDocumentDefaultVoice(
            NarrationSegment segment,
            DocuPodcastProject project) {
        String segmentVoice = segment == null || segment.voiceProfileId() == null
                ? "" : segment.voiceProfileId().strip();
        if (project == null
                || (!segmentVoice.isBlank()
                && !DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(segmentVoice))) {
            return segmentVoice;
        }
        String projectVoice = project.documentDefaultVoiceProfileId();
        return projectVoice.isBlank() ? segmentVoice : projectVoice;
    }

    private static Optional<String> explicitVoiceId(List<FragmentAssetBinding> bindings) {
        return (bindings == null ? List.<FragmentAssetBinding>of() : bindings).stream()
                .filter(binding -> binding.role() == FragmentAssetRole.VOICE_TRACK)
                .map(FragmentAssetBinding::assetId)
                .filter(value -> !normalize(value).isBlank())
                .findFirst();
    }

    private static Optional<String> theatreVoiceId(DocumentFragment fragment, DocuPodcastProject project) {
        if (fragment == null || project == null || project.theatre() == null) {
            return Optional.empty();
        }
        TheatreProjectLayer theatre = project.theatre();
        Optional<String> interventionId = theatre.intervenciones().stream()
                .filter(intervention -> intervention.blockId().equals(fragment.sourceBlockId()))
                .map(TheatreProjectLayer.Intervencion::id)
                .findFirst();
        Optional<String> characterId = interventionId.flatMap(id -> theatre.textActionPlacements().stream()
                .filter(placement -> placement.intervencionId().equals(id))
                .map(TheatreProjectLayer.TextActionPlacement::characterId)
                .filter(value -> !normalize(value).isBlank())
                .findFirst());
        return characterId.flatMap(id -> theatre.voiceRoleAliases().stream()
                .filter(alias -> alias.characterId().equals(id))
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId)
                .filter(value -> !normalize(value).isBlank())
                .findFirst());
    }

    private static VoiceReferenceAvailability availability(VoiceProfile voice, List<AudioEngineReadinessUiItem> readiness) {
        if (voice == null) {
            return VoiceReferenceAvailability.MISSING_PROFILE;
        }
        if (!voice.usableForTts()) {
            return VoiceReferenceAvailability.MISSING_SAMPLE;
        }
        if (readiness != null && !readiness.isEmpty() && readiness.stream().noneMatch(AudioEngineReadinessUiItem::usableInDocument)) {
            return VoiceReferenceAvailability.ENGINE_UNAVAILABLE;
        }
        if (!engineCompatible(voice, readiness)) {
            return VoiceReferenceAvailability.UNSUPPORTED_FOR_MODE;
        }
        if (voice.type() == VoiceProfileType.PREDEFINED || Boolean.parseBoolean(voice.metadata().getOrDefault("builtIn", "false"))) {
            return VoiceReferenceAvailability.BUILT_IN;
        }
        return VoiceReferenceAvailability.AVAILABLE;
    }

    private static boolean engineCompatible(VoiceProfile voice, List<AudioEngineReadinessUiItem> readiness) {
        if (readiness == null || readiness.isEmpty() || voice.engineType() == VoiceEngineType.MOCK) {
            return true;
        }
        boolean piperReady = hasReadyEngine(readiness, "piper");
        boolean xttsReady = hasReadyEngine(readiness, "xtts");
        return switch (voice.engineType()) {
            case PIPER, LOCAL_TTS_PROCESS -> piperReady || anyReadyEngine(readiness);
            case XTTS, HUMAN_AUDIO -> xttsReady || anyReadyEngine(readiness);
            case UNKNOWN -> anyReadyEngine(readiness);
            case MOCK -> true;
        };
    }

    private static boolean hasReadyEngine(List<AudioEngineReadinessUiItem> readiness, String engineId) {
        return readiness.stream()
                .anyMatch(item -> item.usableInDocument() && item.engineId().equalsIgnoreCase(engineId));
    }

    private static boolean anyReadyEngine(List<AudioEngineReadinessUiItem> readiness) {
        return readiness.stream().anyMatch(AudioEngineReadinessUiItem::usableInDocument);
    }

    private static String messageFor(VoiceProfile voice, VoiceReferenceAvailability availability) {
        return switch (availability) {
            case AVAILABLE -> "Voz lista para el fragmento.";
            case BUILT_IN -> "Voz predisenada lista para el fragmento.";
            case MISSING_PROFILE -> "La voz asignada no existe en la biblioteca del proyecto.";
            case MISSING_SAMPLE -> "La voz asignada necesita una muestra antes de sintetizar.";
            case ENGINE_UNAVAILABLE -> "No hay motor de audio listo para generar esta voz.";
            case UNSUPPORTED_FOR_MODE -> "La voz asignada no es compatible con el motor activo.";
        };
    }

    private static AudioVoiceReadiness readiness(List<FragmentAudioVoiceState> states, List<AudioEngineReadinessUiItem> engineReadiness) {
        List<FragmentAudioVoiceState> fragments = states == null ? List.of() : states;
        int total = fragments.size();
        int narratable = (int) fragments.stream().filter(FragmentAudioVoiceState::narratable).count();
        int audioReady = (int) fragments.stream().filter(FragmentAudioVoiceState::narratable).filter(FragmentAudioVoiceState::audioReady).count();
        int voiceReady = (int) fragments.stream().filter(FragmentAudioVoiceState::narratable).filter(FragmentAudioVoiceState::voiceReady).count();
        int audioMissing = Math.max(0, narratable - audioReady);
        int unavailableVoice = Math.max(0, narratable - voiceReady);
        boolean engineReady = engineReadiness == null || engineReadiness.isEmpty()
                || engineReadiness.stream().anyMatch(AudioEngineReadinessUiItem::usableInDocument);
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        if (narratable == 0) {
            blockers.add("No hay fragmentos narrables preparados.");
        }
        if (audioMissing > 0) {
            blockers.add("Falta audio listo en " + audioMissing + " fragmento(s).");
        }
        if (unavailableVoice > 0) {
            blockers.add("Hay " + unavailableVoice + " fragmento(s) con voz no disponible.");
        }
        if (!engineReady) {
            blockers.add("No hay motor de audio listo para Documento.");
        }
        if (engineReadiness != null) {
            engineReadiness.stream()
                    .filter(item -> !item.usableInDocument())
                    .map(AudioEngineReadinessUiItem::compactLine)
                    .forEach(warnings::add);
        }
        return new AudioVoiceReadiness(total, narratable, audioReady, audioMissing, voiceReady,
                unavailableVoice, engineReady, blockers, warnings);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private record AudioChoice(AudioSourceKind kind, boolean ready, String path, String status, double durationSeconds) {
    }

    private record VoiceChoice(String voiceId, String displayName, VoiceReferenceAvailability availability, String message) {
    }
}
