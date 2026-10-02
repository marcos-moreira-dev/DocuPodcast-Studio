package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.*;

/** Resolves persisted stage deltas into a complete deterministic snapshot. */
public final class ResolveTheatreInterventionSnapshotUseCase {
    public TheatreInterventionSnapshot execute(DocuPodcastProject project, NarrationScriptDocument script,
                                               String interventionId) {
        TheatreInterventionSnapshot base = execute(project, interventionId, script);
        String blockId = project.theatre().intervenciones().stream().filter(value -> value.id().equals(interventionId))
                .map(TheatreProjectLayer.Intervencion::blockId).findFirst().orElse("");
        String text = script == null ? "" : script.segments().stream()
                .filter(segment -> segment.id().equals(blockId) || segment.sourceBlockIds().contains(blockId))
                .map(segment -> segment.narrationText()).findFirst().orElse("");
        if (text.isBlank()) return base;
        return new TheatreInterventionSnapshot(base.interventionId(), base.sequence(), base.actId(), base.sceneId(), text,
                base.speakerCharacterId(), base.voiceId(), base.tone(), base.microexpression(), base.emoji(),
                base.interactionTargets(), base.characters(), base.actions(), base.objects(), base.backdropId(),
                base.spatialMapAssetId(), base.cameraId(), base.chorusCharacterIds(), base.audioAssetIds(),
                base.frameAssetId(), base.intermediateFrameAssetIds(), base.resolvedAssetReferences(), base.invalidReferences());
    }

    public TheatreInterventionSnapshot execute(DocuPodcastProject project, String interventionId) {
        return execute(project, interventionId, null);
    }

    private TheatreInterventionSnapshot execute(DocuPodcastProject project, String interventionId, NarrationScriptDocument script) {
        TheatreInterventionSnapshot base = execute(project.theatre(), interventionId);
        String blockId = project.theatre().intervenciones().stream().filter(value -> value.id().equals(interventionId))
                .map(TheatreProjectLayer.Intervencion::blockId).findFirst().orElse("");
        String segmentId = script == null ? blockId : script.segments().stream()
                .filter(s -> s.sourceBlockIds().contains(blockId) || s.id().equals(blockId)).map(s -> s.id()).findFirst().orElse(blockId);
        List<String> humanAudio = project.narrativeLayerAssignments().stream()
                .filter(value -> value.kind() == NarrativeLayerKind.HUMAN_AUDIO && value.textRange().segmentId().equals(segmentId))
                .map(value -> value.targetId()).toList();
        String tone = project.narrativeLayerAssignments().stream()
                .filter(value -> value.kind() == NarrativeLayerKind.EMOTION && value.textRange().segmentId().equals(segmentId))
                .map(value -> value.targetId()).findFirst().orElse(base.tone());
        if (humanAudio.isEmpty() && tone.equals(base.tone())) return base;
        LinkedHashSet<String> audio = new LinkedHashSet<>(base.audioAssetIds()); audio.addAll(humanAudio);
        LinkedHashSet<String> assets = new LinkedHashSet<>(base.resolvedAssetReferences()); assets.addAll(humanAudio);
        return new TheatreInterventionSnapshot(base.interventionId(), base.sequence(), base.actId(), base.sceneId(),
                base.text(), base.speakerCharacterId(), base.voiceId(), tone, base.microexpression(), base.emoji(),
                base.interactionTargets(), base.characters(), base.actions(), base.objects(), base.backdropId(),
                base.spatialMapAssetId(), base.cameraId(), base.chorusCharacterIds(), List.copyOf(audio),
                base.frameAssetId(), base.intermediateFrameAssetIds(), List.copyOf(assets), base.invalidReferences());
    }

    public TheatreInterventionSnapshot execute(TheatreProjectLayer layer, String interventionId) {
        Objects.requireNonNull(layer, "layer");
        TheatreProjectLayer.Intervencion intervention = layer.intervenciones().stream()
                .filter(value -> value.id().equals(interventionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown intervention: " + interventionId));
        Map<String, TheatreInterventionState> states = indexStates(layer);
        ResolvedStage stage = resolve(layer, states, intervention, new LinkedHashSet<>());
        TheatreProjectLayer.TextActionPlacement placement = layer.textActionPlacements().stream()
                .filter(value -> value.intervencionId().equals(interventionId)).findFirst().orElse(null);
        String sceneId = placement == null ? "" : placement.sceneId();
        TheatreProjectLayer.Scene scene = layer.scenes().stream().filter(value -> value.id().equals(sceneId))
                .findFirst().orElse(null);
        String speaker = placement == null ? "" : placement.characterId();
        String voice = layer.voiceRoleAliases().stream().filter(value -> value.characterId().equals(speaker))
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId).findFirst().orElse("");
        List<String> targets = placement == null || placement.interactionTarget().isBlank() ? List.of()
                : Arrays.stream(placement.interactionTarget().split("\\s*(?:,|;|/|\\s+y\\s+)\\s*"))
                .filter(value -> !value.isBlank()).toList();
        String camera = layer.cameraCues().stream().filter(value -> value.intervencionId().equals(interventionId))
                .map(TheatreProjectLayer.CameraCue::cameraId).findFirst().orElse("");
        String backdrop = resolveBackdrop(layer, interventionId, sceneId);
        List<String> chorus = layer.choralVoiceAssignments().stream()
                .filter(value -> value.intervencionId().equals(interventionId))
                .flatMap(value -> value.participantCharacterIds().stream()).distinct().toList();
        List<String> audio = layer.audioTracks().stream()
                .filter(value -> value.startIntervencionId().equals(interventionId))
                .map(TheatreProjectLayer.TheatreAudioTrack::assetId).toList();
        String frame = layer.intervencionesVisuales().stream()
                .filter(value -> value.intervencionId().equals(interventionId))
                .map(TheatreProjectLayer.IntervencionVisual::assetId).findFirst().orElse("");
        List<String> intermediate = layer.intermediateFrames().stream()
                .filter(value -> value.fromIntervencionId().equals(interventionId)
                        || value.toIntervencionId().equals(interventionId))
                .map(TheatreProjectLayer.IntermediateFrame::assetId).distinct().toList();
        LinkedHashSet<String> assets = new LinkedHashSet<>();
        if (scene != null && !scene.spatialMapAssetId().isBlank()) assets.add(scene.spatialMapAssetId());
        if (!backdrop.isBlank()) layer.stageBackdrops().stream().filter(value -> value.id().equals(backdrop))
                .map(TheatreProjectLayer.StageBackdrop::assetId).findFirst().ifPresent(assets::add);
        assets.addAll(audio); if (!frame.isBlank()) assets.add(frame); assets.addAll(intermediate);
        return new TheatreInterventionSnapshot(interventionId, intervention.sequenceIndex(),
                scene == null ? "" : scene.actId(), sceneId, "", speaker, voice, stage.tone,
                stage.microexpression, stage.emoji, targets, stage.characters, stage.events, stage.objects,
                backdrop, scene == null ? "" : scene.spatialMapAssetId(), camera, chorus, audio, frame,
                intermediate, List.copyOf(assets), stage.invalidReferences);
    }

    private ResolvedStage resolve(TheatreProjectLayer layer, Map<String, TheatreInterventionState> states,
                                  TheatreProjectLayer.Intervencion intervention, Set<String> stack) {
        if (!stack.add(intervention.id())) throw new IllegalArgumentException("Cyclic theatre state inheritance at " + intervention.id());
        TheatreInterventionState delta = states.get(intervention.id());
        ResolvedStage result = new ResolvedStage();
        if (delta != null && delta.inheritanceMode() != TheatreInterventionState.InheritanceMode.RESET) {
            TheatreProjectLayer.Intervencion parent = parent(layer, states, intervention, delta);
            if (parent != null) result.copyFrom(resolve(layer, states, parent, stack));
        }
        if (delta != null) apply(layer, result, delta);
        stack.remove(intervention.id());
        return result;
    }

    private static TheatreProjectLayer.Intervencion parent(TheatreProjectLayer layer,
            Map<String, TheatreInterventionState> states, TheatreProjectLayer.Intervencion current,
            TheatreInterventionState delta) {
        if (delta.inheritanceMode() == TheatreInterventionState.InheritanceMode.EXPLICIT) {
            TheatreProjectLayer.Intervencion parent = layer.intervenciones().stream()
                    .filter(value -> value.id().equals(delta.inheritsFromInterventionId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown inherited intervention: " + delta.inheritsFromInterventionId()));
            if (parent.sequenceIndex() >= current.sequenceIndex()) throw new IllegalArgumentException("Inheritance must reference an earlier intervention");
            return parent;
        }
        return layer.intervenciones().stream().filter(value -> value.sequenceIndex() == current.sequenceIndex() - 1)
                .filter(previous -> sameScene(layer, previous.id(), current.id())).findFirst().orElse(null);
    }

    private static boolean sameScene(TheatreProjectLayer layer, String left, String right) {
        String a = layer.textActionPlacements().stream().filter(value -> value.intervencionId().equals(left))
                .map(TheatreProjectLayer.TextActionPlacement::sceneId).findFirst().orElse("");
        String b = layer.textActionPlacements().stream().filter(value -> value.intervencionId().equals(right))
                .map(TheatreProjectLayer.TextActionPlacement::sceneId).findFirst().orElse("");
        return !a.isBlank() && a.equals(b);
    }

    private static void apply(TheatreProjectLayer layer, ResolvedStage result, TheatreInterventionState delta) {
        Set<String> characterIds = layer.characters().stream().map(TheatreProjectLayer.CharacterProfile::id).collect(java.util.stream.Collectors.toSet());
        Set<String> objectIds = layer.objects().stream().map(TheatreProjectLayer.TheatreObject::id).collect(java.util.stream.Collectors.toSet());
        for (TheatreInterventionState.CharacterState value : delta.characters()) {
            if (!characterIds.contains(value.characterId())) { result.invalidReferences.add("character:" + value.characterId()); continue; }
            TheatreInterventionSnapshot.CharacterSnapshot old = result.characters.getOrDefault(value.characterId(),
                    new TheatreInterventionSnapshot.CharacterSnapshot(false, "", "", "", "", ""));
            boolean present = value.presence() == TheatreInterventionState.Presence.INHERIT ? old.present()
                    : value.presence() == TheatreInterventionState.Presence.PRESENT;
            result.characters.put(value.characterId(), new TheatreInterventionSnapshot.CharacterSnapshot(present,
                    pick(value.position(), old.position()), pick(value.orientation(), old.orientation()),
                    pick(value.gazeTarget(), old.gazeTarget()), pick(value.visualVariantId(), old.visualVariantId()),
                    pick(value.costume(), old.costume())));
        }
        for (TheatreInterventionState.ObjectState value : delta.objects()) {
            if (!objectIds.contains(value.objectId())) { result.invalidReferences.add("object:" + value.objectId()); continue; }
            TheatreInterventionSnapshot.ObjectSnapshot old = result.objects.getOrDefault(value.objectId(),
                    new TheatreInterventionSnapshot.ObjectSnapshot(false, "", "", ""));
            boolean present = value.presence() == TheatreInterventionState.Presence.INHERIT ? old.present()
                    : value.presence() == TheatreInterventionState.Presence.PRESENT;
            String holder = pick(value.holderCharacterId(), old.holderCharacterId());
            if (!holder.isBlank() && !characterIds.contains(holder)) result.invalidReferences.add("holder:" + holder);
            result.objects.put(value.objectId(), new TheatreInterventionSnapshot.ObjectSnapshot(present,
                    pick(value.position(), old.position()), holder, pick(value.manipulation(), old.manipulation())));
        }
        for (TheatreInterventionState.StageEvent event : delta.events()) applyEvent(result, event);
        result.events.addAll(delta.events());
        result.microexpression = pick(delta.microexpression(), result.microexpression);
        result.emoji = pick(delta.emoji(), result.emoji);
        result.tone = pick(delta.tone(), result.tone);
    }

    private static void applyEvent(ResolvedStage result, TheatreInterventionState.StageEvent event) {
        if (event.type() == TheatreInterventionState.EventType.ENTER || event.type() == TheatreInterventionState.EventType.MOVE) {
            var old = result.characters.getOrDefault(event.characterId(), new TheatreInterventionSnapshot.CharacterSnapshot(false,"","","","",""));
            result.characters.put(event.characterId(), new TheatreInterventionSnapshot.CharacterSnapshot(true,
                    pick(event.toPosition(), old.position()), old.orientation(), old.gazeTarget(), old.visualVariantId(), old.costume()));
        } else if (event.type() == TheatreInterventionState.EventType.EXIT) {
            var old = result.characters.getOrDefault(event.characterId(), new TheatreInterventionSnapshot.CharacterSnapshot(false,"","","","",""));
            result.characters.put(event.characterId(), new TheatreInterventionSnapshot.CharacterSnapshot(false,"",old.orientation(),old.gazeTarget(),old.visualVariantId(),old.costume()));
        } else if (event.type() == TheatreInterventionState.EventType.TAKE || event.type() == TheatreInterventionState.EventType.CARRY) {
            var old = result.objects.getOrDefault(event.objectId(), new TheatreInterventionSnapshot.ObjectSnapshot(false,"","",""));
            result.objects.put(event.objectId(), new TheatreInterventionSnapshot.ObjectSnapshot(true,"",event.characterId(), event.type().name()));
        } else if (event.type() == TheatreInterventionState.EventType.GIVE) {
            result.objects.put(event.objectId(), new TheatreInterventionSnapshot.ObjectSnapshot(true,"",event.targetCharacterId(),"GIVE"));
        } else if (event.type() == TheatreInterventionState.EventType.DROP) {
            result.objects.put(event.objectId(), new TheatreInterventionSnapshot.ObjectSnapshot(true,event.toPosition(),"","DROP"));
        }
    }

    private static Map<String, TheatreInterventionState> indexStates(TheatreProjectLayer layer) {
        LinkedHashMap<String, TheatreInterventionState> result = new LinkedHashMap<>();
        for (TheatreInterventionState value : layer.interventionStates()) {
            if (result.put(value.interventionId(), value) != null) throw new IllegalArgumentException("Duplicate intervention state: " + value.interventionId());
        }
        return result;
    }

    private static String resolveBackdrop(TheatreProjectLayer layer, String interventionId, String sceneId) {
        String result = layer.stageBackdropAssignments().stream()
                .filter(value -> value.scope().equals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE) && value.scopeId().equals(sceneId))
                .map(TheatreProjectLayer.StageBackdropAssignment::backdropId).findFirst().orElse("");
        return layer.stageBackdropAssignments().stream()
                .filter(value -> value.scope().equals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION) && value.scopeId().equals(interventionId))
                .map(TheatreProjectLayer.StageBackdropAssignment::backdropId).findFirst().orElse(result);
    }

    private static String pick(String candidate, String inherited) { return candidate == null || candidate.isBlank() ? inherited : candidate; }

    private static final class ResolvedStage {
        final LinkedHashMap<String, TheatreInterventionSnapshot.CharacterSnapshot> characters = new LinkedHashMap<>();
        final LinkedHashMap<String, TheatreInterventionSnapshot.ObjectSnapshot> objects = new LinkedHashMap<>();
        final ArrayList<TheatreInterventionState.StageEvent> events = new ArrayList<>();
        final ArrayList<String> invalidReferences = new ArrayList<>();
        String microexpression = ""; String emoji = ""; String tone = "";
        void copyFrom(ResolvedStage other) { characters.putAll(other.characters); objects.putAll(other.objects); events.addAll(other.events); invalidReferences.addAll(other.invalidReferences); microexpression=other.microexpression; emoji=other.emoji; tone=other.tone; }
    }
}
