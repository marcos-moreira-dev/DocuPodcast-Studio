package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceCapabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

/** Handles theatre-only simultaneous voice assignments outside the shell view-model. */
public final class TheatreChoralVoiceWorkflow {
    public List<Option> options(Optional<ProjectSession> session, Optional<String> interventionId) {
        return options(session, interventionId, null);
    }

    public List<Option> options(Optional<ProjectSession> session, Optional<String> interventionId,
                                AudioEngineDescriptor engineDescriptor) {
        if (session.isEmpty() || interventionId.isEmpty()) return List.of();
        TheatreProjectLayer theatre = session.get().project().theatre();
        Map<String, String> voiceByCharacter = new LinkedHashMap<>();
        for (TheatreProjectLayer.VoiceRoleAlias alias : theatre.voiceRoleAliases()) {
            if (!alias.characterId().isBlank() && !alias.voiceProfileId().isBlank()) {
                voiceByCharacter.putIfAbsent(alias.characterId(), alias.voiceProfileId());
            }
        }
        return theatre.characters().stream()
                .map(character -> {
                    String voiceId = voiceByCharacter.getOrDefault(character.id(), "");
                    var voice = session.get().project().voiceLibrary().voiceById(voiceId);
                    boolean ready = !voiceId.isBlank() && voice.isPresent();
                    String status = voiceId.isBlank() ? "Sin voz asignada"
                            : voice.isEmpty() ? "Perfil de voz inexistente"
                            : "Voz disponible";
                    if (ready && engineDescriptor != null) {
                        var capability = new VoiceCapabilityPolicy().evaluateVoice(voice.orElseThrow(), engineDescriptor);
                        ready = capability.synthesizableNow();
                        status = capability.status();
                    }
                    return new Option(character.id(), character.displayName(), voiceId, ready, status,
                            isNarrator(character));
                })
                .toList();
    }

    public State state(Optional<ProjectSession> session, Optional<String> interventionId) {
        return state(session, interventionId, Optional.empty());
    }

    public State state(Optional<ProjectSession> session, Optional<String> interventionId,
                       Optional<NarrationSegment> segment) {
        if (session.isEmpty() || interventionId.isEmpty()) return State.empty();
        TheatreProjectLayer theatre = session.get().project().theatre();
        Optional<TheatreProjectLayer.ChoralVoiceAssignment> assignment = theatre.choralVoiceAssignments().stream()
                .filter(item -> item.intervencionId().equals(interventionId.get()))
                .findFirst();
        if (assignment.isEmpty()) return State.empty();
        String audioLabel = assignment.get().mixedAudioAssetId().isBlank()
                ? "Mezcla pendiente"
                : session.get().project().assets().byId(assignment.get().mixedAudioAssetId())
                        .map(ProjectAssetReference::displayName)
                        .orElse(assignment.get().mixedAudioAssetId());
        boolean audioReady = !assignment.get().mixedAudioAssetId().isBlank()
                && session.get().project().assets().byId(assignment.get().mixedAudioAssetId())
                .filter(ProjectAssetReference::isAudio).isPresent();
        boolean stale = segment.isPresent()
                && TheatreChoralVoiceFingerprint.isGenerated(assignment.get().sourceFingerprint())
                && !TheatreChoralVoiceFingerprint.isCurrent(assignment.get(), session.get().project(), segment.get());
        return new State(assignment.get().intervencionId(),
                assignment.get().participantCharacterIds(), assignment.get().mixedAudioAssetId(), audioLabel,
                audioReady, stale);
    }

    public Result save(ProjectSession session, String interventionId, List<String> characterIds) {
        String id = interventionId == null ? "" : interventionId.strip();
        if (id.isBlank()) return new Result(false, "Selecciona una intervencion teatral antes de guardar voces simultaneas.");
        List<String> participants = characterIds == null ? List.of() : characterIds.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (participants.size() < 2) return new Result(false, "Selecciona al menos dos personajes para voces simultaneas.");
        TheatreProjectLayer theatre = session.project().theatre();
        Optional<TheatreProjectLayer.ChoralVoiceAssignment> previous = theatre.choralVoiceAssignments().stream()
                .filter(item -> item.intervencionId().equals(id))
                .findFirst();
        String mixedAudioAssetId = previous
                .filter(item -> item.participantCharacterIds().equals(participants))
                .map(TheatreProjectLayer.ChoralVoiceAssignment::mixedAudioAssetId)
                .orElse("");
        ArrayList<TheatreProjectLayer.ChoralVoiceAssignment> assignments =
                new ArrayList<>(theatre.choralVoiceAssignments().stream()
                        .filter(item -> !item.intervencionId().equals(id))
                        .toList());
        assignments.add(new TheatreProjectLayer.ChoralVoiceAssignment(
                id, participants, mixedAudioAssetId, "manual:" + id + ":" + String.join(",", participants),
                "Voces simultaneas configuradas manualmente."));
        session.replaceProject(session.project().withTheatre(theatre.withChoralVoiceAssignments(assignments)), true);
        return new Result(true, "Voces simultaneas guardadas para " + id + ".");
    }

    public Result clear(ProjectSession session, String interventionId) {
        String id = interventionId == null ? "" : interventionId.strip();
        if (id.isBlank()) return new Result(false, "Selecciona una intervencion teatral antes de quitar voces simultaneas.");
        TheatreProjectLayer theatre = session.project().theatre();
        List<TheatreProjectLayer.ChoralVoiceAssignment> assignments = theatre.choralVoiceAssignments().stream()
                .filter(item -> !item.intervencionId().equals(id))
                .toList();
        session.replaceProject(session.project().withTheatre(theatre.withChoralVoiceAssignments(assignments)), true);
        return new Result(true, "Voces simultaneas retiradas de " + id + ".");
    }

    public Result attachMixedAudio(ProjectSession session, String interventionId, String mixedAudioAssetId) {
        String id = interventionId == null ? "" : interventionId.strip();
        String assetId = mixedAudioAssetId == null ? "" : mixedAudioAssetId.strip();
        if (id.isBlank()) return new Result(false, "Selecciona una intervencion teatral antes de asociar la mezcla.");
        if (assetId.isBlank()) return new Result(false, "No se importo ningun audio mixto.");
        TheatreProjectLayer theatre = session.project().theatre();
        Optional<TheatreProjectLayer.ChoralVoiceAssignment> existing = theatre.choralVoiceAssignments().stream()
                .filter(item -> item.intervencionId().equals(id))
                .findFirst();
        if (existing.isEmpty()) return new Result(false, "Guarda primero los participantes de voces simultaneas.");
        ArrayList<TheatreProjectLayer.ChoralVoiceAssignment> assignments =
                new ArrayList<>(theatre.choralVoiceAssignments().stream()
                        .filter(item -> !item.intervencionId().equals(id))
                        .toList());
        TheatreProjectLayer.ChoralVoiceAssignment current = existing.get();
        assignments.add(new TheatreProjectLayer.ChoralVoiceAssignment(id, current.participantCharacterIds(), assetId,
                current.sourceFingerprint(), "Mezcla multivoz asociada manualmente."));
        session.replaceProject(session.project().withTheatre(theatre.withChoralVoiceAssignments(assignments)), true);
        return new Result(true, "Mezcla multivoz asociada a " + id + ".");
    }

    private static boolean isNarrator(TheatreProjectLayer.CharacterProfile character) {
        String searchable = (character.id() + " " + character.displayName() + " "
                + String.join(" ", character.aliases())).toUpperCase(Locale.ROOT);
        return searchable.contains("NARRADOR") || searchable.contains("NARRATOR");
    }

    public record Option(String characterId, String displayName, String voiceProfileId,
                         boolean voiceReady, String status, boolean narrator) {
        public Option(String characterId, String displayName, String voiceProfileId) {
            this(characterId, displayName, voiceProfileId, !normalize(voiceProfileId).isBlank(),
                    normalize(voiceProfileId).isBlank() ? "Sin voz asignada" : "Voz disponible", false);
        }

        public Option {
            characterId = characterId == null ? "" : characterId.strip();
            displayName = displayName == null || displayName.isBlank() ? characterId : displayName.strip();
            voiceProfileId = voiceProfileId == null ? "" : voiceProfileId.strip();
            status = status == null || status.isBlank() ? "Sin clasificar" : status.strip();
        }
    }

    public record State(String interventionId, List<String> participantCharacterIds, String mixedAudioAssetId,
                        String audioLabel, boolean audioReady, boolean stale) {
        public State(String interventionId, List<String> participantCharacterIds, String mixedAudioAssetId,
                     String audioLabel) {
            this(interventionId, participantCharacterIds, mixedAudioAssetId, audioLabel,
                    mixedAudioAssetId != null && !mixedAudioAssetId.isBlank(), false);
        }

        public State {
            interventionId = interventionId == null ? "" : interventionId.strip();
            participantCharacterIds = participantCharacterIds == null ? List.of() : List.copyOf(participantCharacterIds);
            mixedAudioAssetId = mixedAudioAssetId == null ? "" : mixedAudioAssetId.strip();
            audioLabel = audioLabel == null || audioLabel.isBlank() ? "Sin mezcla multivoz" : audioLabel.strip();
        }

        public static State empty() {
            return new State("", List.of(), "", "Sin voces simultaneas", false, false);
        }
    }

    public record Result(boolean saved, String message) {
        public Result {
            message = message == null ? "" : message.strip();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
