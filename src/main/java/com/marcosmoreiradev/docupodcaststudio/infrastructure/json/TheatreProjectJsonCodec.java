package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.*;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;
import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.*;

final class TheatreProjectJsonCodec implements ProjectJsonSectionCodec {
    static TheatreProjectLayer readTheatre(Map<String, Object> theatre) throws IOException {
        if (theatre.isEmpty()) {
            return TheatreProjectLayer.empty();
        }
        return new TheatreProjectLayer(
                readIntervenciones(theatre.get("intervenciones")),
                readTheatreCharacters(theatre.get("characters")),
                readVoiceRoleAliases(theatre.get("voiceRoleAliases")),
                readCharacterImages(theatre.get("characterImages")),
                readIntervencionesVisuales(theatre.get("intervencionesVisuales")),
                readIntermediateFrames(theatre.get("intermediateFrames")),
                readTheatreActs(theatre.get("acts")),
                readScenes(theatre.get("scenes")),
                readSpatialPositions(theatre.get("positions")),
                readTheatreActions(theatre.get("actions")),
                readTextActionPlacements(theatre.get("textActionPlacements")),
                readObjectImages(theatre.get("objectImages")),
                readTheatreObjects(theatre.get("objects")),
                readTheatreAudioTracks(theatre.get("audioTracks")),
                readCameraReferences(theatre.get("cameraReferences")),
                readCameraCues(theatre.get("cameraCues")),
                readStageBackdrops(theatre.get("stageBackdrops")),
                readStageBackdropAssignments(theatre.get("stageBackdropAssignments")),
                readChoralVoiceAssignments(theatre.get("choralVoiceAssignments")),
                readInterventionStates(theatre.get("interventionStates"))
        );
    }

    static List<TheatreProjectLayer.Intervencion> readIntervenciones(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intervenciones must be an array");
        }
        ArrayList<TheatreProjectLayer.Intervencion> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intervenciones entries must be objects");
            }
            Map<String, Object> alias = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.Intervencion(
                    string(alias.get("id"), "theatre.intervencion.id"),
                    string(alias.get("blockId"), "theatre.intervencion.blockId"),
                    intOrDefault(alias.get("sequenceIndex"), result.size() + 1)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.CharacterProfile> readTheatreCharacters(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.characters must be an array");
        }
        ArrayList<TheatreProjectLayer.CharacterProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.characters entries must be objects");
            }
            Map<String, Object> character = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CharacterProfile(
                    string(character.get("id"), "theatre.character.id"),
                    string(character.get("displayName"), "theatre.character.displayName"),
                    stringListOrDefault(character.get("aliases"), List.of()),
                    stringOrDefault(character.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.VoiceRoleAlias> readVoiceRoleAliases(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.voiceRoleAliases must be an array");
        }
        ArrayList<TheatreProjectLayer.VoiceRoleAlias> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.voiceRoleAliases entries must be objects");
            }
            Map<String, Object> alias = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.VoiceRoleAlias(
                    string(alias.get("id"), "theatre.voiceRoleAlias.id"),
                    string(alias.get("displayName"), "theatre.voiceRoleAlias.displayName"),
                    string(alias.get("voiceProfileId"), "theatre.voiceRoleAlias.voiceProfileId"),
                    stringOrDefault(alias.get("characterId"), ""),
                    stringOrDefault(alias.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.CharacterImage> readCharacterImages(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.characterImages must be an array");
        }
        ArrayList<TheatreProjectLayer.CharacterImage> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.characterImages entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            String id = stringOrDefault(image.get("id"), "");
            if (id.isBlank()) {
                id = "CHARIMG-" + String.format(java.util.Locale.ROOT, "%03d", result.size() + 1);
            }
            result.add(new TheatreProjectLayer.CharacterImage(
                    id,
                    string(image.get("characterId"), "theatre.characterImage.characterId"),
                    stringOrDefault(image.get("sceneId"), ""),
                    string(image.get("view"), "theatre.characterImage.view"),
                    string(image.get("assetId"), "theatre.characterImage.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.IntervencionVisual> readIntervencionesVisuales(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intervencionesVisuales must be an array");
        }
        ArrayList<TheatreProjectLayer.IntervencionVisual> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intervencionesVisuales entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.IntervencionVisual(
                    string(image.get("intervencionId"), "theatre.intervencionVisual.intervencionId"),
                    string(image.get("assetId"), "theatre.intervencionVisual.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.IntermediateFrame> readIntermediateFrames(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intermediateFrames must be an array");
        }
        ArrayList<TheatreProjectLayer.IntermediateFrame> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intermediateFrames entries must be objects");
            }
            Map<String, Object> frame = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.IntermediateFrame(
                    string(frame.get("fromIntervencionId"), "theatre.intermediateFrame.fromIntervencionId"),
                    string(frame.get("toIntervencionId"), "theatre.intermediateFrame.toIntervencionId"),
                    string(frame.get("assetId"), "theatre.intermediateFrame.assetId"),
                    stringOrDefault(frame.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.CameraReference> readCameraReferences(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.cameraReferences must be an array");
        }
        ArrayList<TheatreProjectLayer.CameraReference> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.cameraReferences entries must be objects");
            }
            Map<String, Object> camera = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CameraReference(
                    string(camera.get("id"), "theatre.cameraReference.id"),
                    string(camera.get("displayName"), "theatre.cameraReference.displayName"),
                    string(camera.get("assetId"), "theatre.cameraReference.assetId"),
                    stringOrDefault(camera.get("distance"), ""),
                    stringOrDefault(camera.get("orientation"), ""),
                    stringOrDefault(camera.get("height"), ""),
                    booleanOrDefault(camera.get("defaultCamera"), false),
                    stringOrDefault(camera.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.CameraCue> readCameraCues(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.cameraCues must be an array");
        }
        ArrayList<TheatreProjectLayer.CameraCue> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.cameraCues entries must be objects");
            }
            Map<String, Object> cue = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CameraCue(
                    string(cue.get("intervencionId"), "theatre.cameraCue.intervencionId"),
                    string(cue.get("cameraId"), "theatre.cameraCue.cameraId"),
                    stringOrDefault(cue.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.StageBackdrop> readStageBackdrops(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.stageBackdrops must be an array");
        }
        ArrayList<TheatreProjectLayer.StageBackdrop> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.stageBackdrops entries must be objects");
            }
            Map<String, Object> backdrop = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.StageBackdrop(
                    string(backdrop.get("id"), "theatre.stageBackdrop.id"),
                    string(backdrop.get("displayName"), "theatre.stageBackdrop.displayName"),
                    string(backdrop.get("assetId"), "theatre.stageBackdrop.assetId"),
                    stringOrDefault(backdrop.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.StageBackdropAssignment> readStageBackdropAssignments(Object value)
            throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.stageBackdropAssignments must be an array");
        }
        ArrayList<TheatreProjectLayer.StageBackdropAssignment> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.stageBackdropAssignments entries must be objects");
            }
            Map<String, Object> assignment = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.StageBackdropAssignment(
                    string(assignment.get("scope"), "theatre.stageBackdropAssignment.scope"),
                    string(assignment.get("scopeId"), "theatre.stageBackdropAssignment.scopeId"),
                    string(assignment.get("backdropId"), "theatre.stageBackdropAssignment.backdropId"),
                    stringOrDefault(assignment.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.ChoralVoiceAssignment> readChoralVoiceAssignments(Object value)
            throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.choralVoiceAssignments must be an array");
        }
        ArrayList<TheatreProjectLayer.ChoralVoiceAssignment> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.choralVoiceAssignments entries must be objects");
            }
            Map<String, Object> assignment = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.ChoralVoiceAssignment(
                    string(assignment.get("intervencionId"), "theatre.choralVoiceAssignment.intervencionId"),
                    stringListOrDefault(assignment.get("participantCharacterIds"), List.of()),
                    stringOrDefault(assignment.get("mixedAudioAssetId"), ""),
                    stringOrDefault(assignment.get("sourceFingerprint"), ""),
                    stringOrDefault(assignment.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.TheatreAudioTrack> readTheatreAudioTracks(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.audioTracks must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAudioTrack> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.audioTracks entries must be objects");
            }
            Map<String, Object> track = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAudioTrack(
                    string(track.get("id"), "theatre.audioTrack.id"),
                    string(track.get("assetId"), "theatre.audioTrack.assetId"),
                    stringOrDefault(track.get("startIntervencionId"), ""),
                    stringOrDefault(track.get("startSegmentId"), ""),
                    doubleOrDefault(track.get("sourceStartSeconds"), 0.0),
                    doubleOrDefault(track.get("sourceEndSeconds"), 0.0),
                    enumValue(TheatreProjectLayer.AudioTrackEndMode.class,
                            stringOrDefault(track.get("endMode"), TheatreProjectLayer.AudioTrackEndMode.FILE_END.name()),
                            "theatre.audioTrack.endMode"),
                    doubleOrDefault(track.get("volume"), 0.30),
                    doubleOrDefault(track.get("sourceDurationSeconds"), 0.0),
                    booleanOrDefault(track.get("gentleFade"), false)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.TheatreAct> readTheatreActs(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.acts must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAct> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.acts entries must be objects");
            }
            Map<String, Object> act = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAct(
                    string(act.get("id"), "theatre.act.id"),
                    string(act.get("displayName"), "theatre.act.displayName"),
                    stringOrDefault(act.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.Scene> readScenes(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.scenes must be an array");
        }
        ArrayList<TheatreProjectLayer.Scene> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.scenes entries must be objects");
            }
            Map<String, Object> scene = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.Scene(
                    string(scene.get("id"), "theatre.scene.id"),
                    string(scene.get("displayName"), "theatre.scene.displayName"),
                    stringOrDefault(scene.get("notes"), ""),
                    stringOrDefault(scene.get("actId"), ""),
                    stringOrDefault(scene.get("spatialMapAssetId"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.SpatialPosition> readSpatialPositions(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.positions must be an array");
        }
        ArrayList<TheatreProjectLayer.SpatialPosition> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.positions entries must be objects");
            }
            Map<String, Object> position = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.SpatialPosition(
                    string(position.get("sceneId"), "theatre.position.sceneId"),
                    string(position.get("alias"), "theatre.position.alias"),
                    stringOrDefault(position.get("characterId"), ""),
                    doubleOrDefault(position.get("x"), 0),
                    doubleOrDefault(position.get("y"), 0),
                    stringOrDefault(position.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.TheatreAction> readTheatreActions(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.actions must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAction> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.actions entries must be objects");
            }
            Map<String, Object> action = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAction(
                    string(action.get("sceneId"), "theatre.action.sceneId"),
                    string(action.get("fromAlias"), "theatre.action.fromAlias"),
                    string(action.get("toAlias"), "theatre.action.toAlias"),
                    stringOrDefault(action.get("characterId"), ""),
                    stringOrDefault(action.get("description"), ""),
                    booleanOrDefault(action.get("showArrow"), true)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.TextActionPlacement> readTextActionPlacements(Object value) throws IOException {
        List<Map<String, Object>> items = castList(value, "theatre.textActionPlacements");
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        ArrayList<TheatreProjectLayer.TextActionPlacement> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, String> characterLocations = new LinkedHashMap<>();
            Object locs = item.get("characterLocations");
            if (locs instanceof Map<?, ?> rawLocs) {
                for (var entry : rawLocs.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        characterLocations.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }
            result.add(new TheatreProjectLayer.TextActionPlacement(
                    string(item.get("intervencionId"), "theatre.textActionPlacement.intervencionId"),
                    string(item.get("sceneId"), "theatre.textActionPlacement.sceneId"),
                    stringOrDefault(item.get("characterId"), ""),
                    stringOrDefault(item.get("origin"), ""),
                    stringOrDefault(item.get("destination"), ""),
                    stringOrDefault(item.get("interactionTarget"), ""),
                    characterLocations));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.TheatreObject> readTheatreObjects(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.objects must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreObject> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.objects entries must be objects");
            }
            Map<String, Object> object = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreObject(
                    string(object.get("id"), "theatre.object.id"),
                    string(object.get("displayName"), "theatre.object.displayName"),
                    stringOrDefault(object.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreProjectLayer.ObjectImage> readObjectImages(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.objectImages must be an array");
        }
        ArrayList<TheatreProjectLayer.ObjectImage> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.objectImages entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            String id = stringOrDefault(image.get("id"), "");
            if (id.isBlank()) {
                id = "OBJIMG-" + String.format(java.util.Locale.ROOT, "%03d", result.size() + 1);
            }
            result.add(new TheatreProjectLayer.ObjectImage(
                    id,
                    string(image.get("objectId"), "theatre.objectImage.objectId"),
                    stringOrDefault(image.get("sceneId"), ""),
                    string(image.get("view"), "theatre.objectImage.view"),
                    string(image.get("assetId"), "theatre.objectImage.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<TheatreInterventionState> readInterventionStates(Object value) throws IOException {
        List<Map<String, Object>> items = castList(value, "theatre.interventionStates");
        if (items == null || items.isEmpty()) return List.of();
        ArrayList<TheatreInterventionState> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            ArrayList<TheatreInterventionState.CharacterState> characters = new ArrayList<>();
            List<Map<String, Object>> rawCharacters = castList(item.get("characters"), "theatre.interventionState.characters");
            if (rawCharacters != null) for (Map<String, Object> character : rawCharacters) {
                characters.add(new TheatreInterventionState.CharacterState(
                        string(character.get("characterId"), "theatre.characterState.characterId"),
                        enumValue(TheatreInterventionState.Presence.class,
                                stringOrDefault(character.get("presence"), "INHERIT"),
                                "theatre.characterState.presence"),
                        stringOrDefault(character.get("position"), ""),
                        stringOrDefault(character.get("orientation"), ""),
                        stringOrDefault(character.get("gazeTarget"), ""),
                        stringOrDefault(character.get("visualVariantId"), ""),
                        stringOrDefault(character.get("costume"), "")));
            }
            ArrayList<TheatreInterventionState.ObjectState> objects = new ArrayList<>();
            List<Map<String, Object>> rawObjects = castList(item.get("objects"), "theatre.interventionState.objects");
            if (rawObjects != null) for (Map<String, Object> object : rawObjects) {
                objects.add(new TheatreInterventionState.ObjectState(
                        string(object.get("objectId"), "theatre.objectState.objectId"),
                        enumValue(TheatreInterventionState.Presence.class,
                                stringOrDefault(object.get("presence"), "INHERIT"),
                                "theatre.objectState.presence"),
                        stringOrDefault(object.get("position"), ""),
                        stringOrDefault(object.get("holderCharacterId"), ""),
                        stringOrDefault(object.get("manipulation"), "")));
            }
            ArrayList<TheatreInterventionState.StageEvent> events = new ArrayList<>();
            List<Map<String, Object>> rawEvents = castList(item.get("events"), "theatre.interventionState.events");
            if (rawEvents != null) for (Map<String, Object> event : rawEvents) {
                events.add(new TheatreInterventionState.StageEvent(
                        readStageEventType(event),
                        stageEventString(event, "characterId", "source"),
                        stringOrDefault(event.get("objectId"), ""),
                        stageEventString(event, "targetCharacterId", "target"),
                        stringOrDefault(event.get("fromPosition"), ""),
                        stringOrDefault(event.get("toPosition"), "")));
            }
            result.add(new TheatreInterventionState(
                    string(item.get("interventionId"), "theatre.interventionState.interventionId"),
                    enumValue(TheatreInterventionState.InheritanceMode.class,
                            stringOrDefault(item.get("inheritanceMode"), "PREVIOUS"),
                            "theatre.interventionState.inheritanceMode"),
                    stringOrDefault(item.get("inheritsFromInterventionId"), ""),
                    characters, objects, events,
                    stringOrDefault(item.get("microexpression"), ""),
                    stringOrDefault(item.get("emoji"), ""),
                    stringOrDefault(item.get("tone"), "")));
        }
        return List.copyOf(result);
    }

    private static TheatreInterventionState.EventType readStageEventType(Map<String, Object> event)
            throws IOException {
        Object raw = event.get("type");
        if (raw == null) raw = event.get("kind");
        String value = string(raw, "theatre.stageEvent.type");
        if ("SILENT_INTERACTION".equalsIgnoreCase(value)
                || "INTERACTION".equalsIgnoreCase(value)) {
            value = TheatreInterventionState.EventType.INTERACT.name();
        }
        return enumValue(TheatreInterventionState.EventType.class, value, "theatre.stageEvent.type");
    }

    private static String stageEventString(
            Map<String, Object> event,
            String canonicalKey,
            String compatibilityKey) throws IOException {
        Object value = event.get(canonicalKey);
        if (value == null) value = event.get(compatibilityKey);
        return stringOrDefault(value, "");
    }

    static void writeTheatre(StringBuilder out, TheatreProjectLayer theatre) {
        indent(out, 1).append("\"theatre\": {\n");
        writeIntervenciones(out, theatre.intervenciones(), 2);
        out.append(",\n");
        writeTheatreCharacters(out, theatre.characters(), 2);
        out.append(",\n");
        writeVoiceRoleAliases(out, theatre.voiceRoleAliases(), 2);
        out.append(",\n");
        writeCharacterImages(out, theatre.characterImages(), 2);
        out.append(",\n");
        writeIntervencionesVisuales(out, theatre.intervencionesVisuales(), 2);
        out.append(",\n");
        writeIntermediateFrames(out, theatre.intermediateFrames(), 2);
        out.append(",\n");
        writeCameraReferences(out, theatre.cameraReferences(), 2);
        out.append(",\n");
        writeCameraCues(out, theatre.cameraCues(), 2);
        out.append(",\n");
        writeStageBackdrops(out, theatre.stageBackdrops(), 2);
        out.append(",\n");
        writeStageBackdropAssignments(out, theatre.stageBackdropAssignments(), 2);
        out.append(",\n");
        writeChoralVoiceAssignments(out, theatre.choralVoiceAssignments(), 2);
        out.append(",\n");
        writeTheatreActs(out, theatre.acts(), 2);
        out.append(",\n");
        writeScenes(out, theatre.scenes(), 2);
        out.append(",\n");
        writeSpatialPositions(out, theatre.positions(), 2);
        out.append(",\n");
        writeTheatreActions(out, theatre.actions(), 2);
        out.append(",\n");
        writeTextActionPlacements(out, theatre.textActionPlacements(), 2);
        out.append(",\n");
        writeObjectImages(out, theatre.objectImages(), 2);
        out.append(",\n");
        writeTheatreObjects(out, theatre.objects(), 2);
        out.append(",\n");
        writeTheatreAudioTracks(out, theatre.audioTracks(), 2);
        out.append(",\n");
        writeInterventionStates(out, theatre.interventionStates(), 2);
        out.append("\n");
        indent(out, 1).append("}");
    }

    static void writeTheatreAudioTracks(StringBuilder out,
                                                List<TheatreProjectLayer.TheatreAudioTrack> tracks,
                                                int level) {
        indent(out, level).append("\"audioTracks\": [");
        if (tracks != null && !tracks.isEmpty()) {
            out.append("\n");
        }
        List<TheatreProjectLayer.TheatreAudioTrack> safeTracks = tracks == null ? List.of() : tracks;
        for (int i = 0; i < safeTracks.size(); i++) {
            TheatreProjectLayer.TheatreAudioTrack track = safeTracks.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(track.id())); out.append(",\n");
            field(out, level + 2, "assetId", quote(track.assetId())); out.append(",\n");
            field(out, level + 2, "startIntervencionId", quote(track.startIntervencionId())); out.append(",\n");
            field(out, level + 2, "startSegmentId", quote(track.startSegmentId())); out.append(",\n");
            field(out, level + 2, "sourceStartSeconds", Double.toString(track.sourceStartSeconds())); out.append(",\n");
            field(out, level + 2, "sourceEndSeconds", Double.toString(track.sourceEndSeconds())); out.append(",\n");
            field(out, level + 2, "endMode", quote(track.endMode().name())); out.append(",\n");
            field(out, level + 2, "volume", Double.toString(track.volume())); out.append(",\n");
            field(out, level + 2, "sourceDurationSeconds", Double.toString(track.sourceDurationSeconds())); out.append(",\n");
            field(out, level + 2, "gentleFade", Boolean.toString(track.gentleFade())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeTracks.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeIntervenciones(StringBuilder out, List<TheatreProjectLayer.Intervencion> intervenciones, int level) {
        indent(out, level).append("\"intervenciones\": [");
        if (!intervenciones.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < intervenciones.size(); i++) {
            TheatreProjectLayer.Intervencion intervencion = intervenciones.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(intervencion.id())); out.append(",\n");
            field(out, level + 2, "blockId", quote(intervencion.blockId())); out.append(",\n");
            field(out, level + 2, "sequenceIndex", Integer.toString(intervencion.sequenceIndex())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < intervenciones.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeTheatreCharacters(StringBuilder out, List<TheatreProjectLayer.CharacterProfile> characters, int level) {
        indent(out, level).append("\"characters\": [");
        if (!characters.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < characters.size(); i++) {
            TheatreProjectLayer.CharacterProfile character = characters.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(character.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(character.displayName())); out.append(",\n");
            stringArrayField(out, level + 2, "aliases", character.aliases()); out.append(",\n");
            field(out, level + 2, "notes", quote(character.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < characters.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeVoiceRoleAliases(StringBuilder out, List<TheatreProjectLayer.VoiceRoleAlias> aliases, int level) {
        indent(out, level).append("\"voiceRoleAliases\": [");
        if (!aliases.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < aliases.size(); i++) {
            TheatreProjectLayer.VoiceRoleAlias alias = aliases.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(alias.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(alias.displayName())); out.append(",\n");
            field(out, level + 2, "voiceProfileId", quote(alias.voiceProfileId())); out.append(",\n");
            field(out, level + 2, "characterId", quote(alias.characterId())); out.append(",\n");
            field(out, level + 2, "notes", quote(alias.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < aliases.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeCharacterImages(StringBuilder out, List<TheatreProjectLayer.CharacterImage> images, int level) {
        indent(out, level).append("\"characterImages\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.CharacterImage image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(image.id())); out.append(",\n");
            field(out, level + 2, "characterId", quote(image.characterId())); out.append(",\n");
            field(out, level + 2, "sceneId", quote(image.sceneId())); out.append(",\n");
            field(out, level + 2, "view", quote(image.view())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeIntervencionesVisuales(StringBuilder out, List<TheatreProjectLayer.IntervencionVisual> images, int level) {
        indent(out, level).append("\"intervencionesVisuales\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.IntervencionVisual image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(image.intervencionId())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeIntermediateFrames(StringBuilder out, List<TheatreProjectLayer.IntermediateFrame> frames, int level) {
        List<TheatreProjectLayer.IntermediateFrame> safeFrames = frames == null ? List.of() : frames;
        indent(out, level).append("\"intermediateFrames\": [");
        if (!safeFrames.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeFrames.size(); i++) {
            TheatreProjectLayer.IntermediateFrame frame = safeFrames.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "fromIntervencionId", quote(frame.fromIntervencionId())); out.append(",\n");
            field(out, level + 2, "toIntervencionId", quote(frame.toIntervencionId())); out.append(",\n");
            field(out, level + 2, "assetId", quote(frame.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(frame.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeFrames.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeCameraReferences(StringBuilder out,
                                              List<TheatreProjectLayer.CameraReference> references,
                                              int level) {
        List<TheatreProjectLayer.CameraReference> safeReferences = references == null ? List.of() : references;
        indent(out, level).append("\"cameraReferences\": [");
        if (!safeReferences.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeReferences.size(); i++) {
            TheatreProjectLayer.CameraReference reference = safeReferences.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(reference.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(reference.displayName())); out.append(",\n");
            field(out, level + 2, "assetId", quote(reference.assetId())); out.append(",\n");
            field(out, level + 2, "distance", quote(reference.distance())); out.append(",\n");
            field(out, level + 2, "orientation", quote(reference.orientation())); out.append(",\n");
            field(out, level + 2, "height", quote(reference.height())); out.append(",\n");
            field(out, level + 2, "defaultCamera", Boolean.toString(reference.defaultCamera())); out.append(",\n");
            field(out, level + 2, "notes", quote(reference.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeReferences.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeCameraCues(StringBuilder out, List<TheatreProjectLayer.CameraCue> cues, int level) {
        List<TheatreProjectLayer.CameraCue> safeCues = cues == null ? List.of() : cues;
        indent(out, level).append("\"cameraCues\": [");
        if (!safeCues.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeCues.size(); i++) {
            TheatreProjectLayer.CameraCue cue = safeCues.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(cue.intervencionId())); out.append(",\n");
            field(out, level + 2, "cameraId", quote(cue.cameraId())); out.append(",\n");
            field(out, level + 2, "notes", quote(cue.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeCues.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeStageBackdrops(StringBuilder out,
                                            List<TheatreProjectLayer.StageBackdrop> backdrops,
                                            int level) {
        List<TheatreProjectLayer.StageBackdrop> safeBackdrops = backdrops == null ? List.of() : backdrops;
        indent(out, level).append("\"stageBackdrops\": [");
        if (!safeBackdrops.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeBackdrops.size(); i++) {
            TheatreProjectLayer.StageBackdrop backdrop = safeBackdrops.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(backdrop.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(backdrop.displayName())); out.append(",\n");
            field(out, level + 2, "assetId", quote(backdrop.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(backdrop.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeBackdrops.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeStageBackdropAssignments(StringBuilder out,
                                                      List<TheatreProjectLayer.StageBackdropAssignment> assignments,
                                                      int level) {
        List<TheatreProjectLayer.StageBackdropAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        indent(out, level).append("\"stageBackdropAssignments\": [");
        if (!safeAssignments.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeAssignments.size(); i++) {
            TheatreProjectLayer.StageBackdropAssignment assignment = safeAssignments.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "scope", quote(assignment.scope())); out.append(",\n");
            field(out, level + 2, "scopeId", quote(assignment.scopeId())); out.append(",\n");
            field(out, level + 2, "backdropId", quote(assignment.backdropId())); out.append(",\n");
            field(out, level + 2, "notes", quote(assignment.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeAssignments.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeChoralVoiceAssignments(StringBuilder out,
                                                    List<TheatreProjectLayer.ChoralVoiceAssignment> assignments,
                                                    int level) {
        List<TheatreProjectLayer.ChoralVoiceAssignment> safeAssignments =
                assignments == null ? List.of() : assignments;
        indent(out, level).append("\"choralVoiceAssignments\": [");
        if (!safeAssignments.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeAssignments.size(); i++) {
            TheatreProjectLayer.ChoralVoiceAssignment assignment = safeAssignments.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(assignment.intervencionId())); out.append(",\n");
            field(out, level + 2, "participantCharacterIds", stringArray(assignment.participantCharacterIds())); out.append(",\n");
            field(out, level + 2, "mixedAudioAssetId", quote(assignment.mixedAudioAssetId())); out.append(",\n");
            field(out, level + 2, "sourceFingerprint", quote(assignment.sourceFingerprint())); out.append(",\n");
            field(out, level + 2, "notes", quote(assignment.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeAssignments.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeTheatreActs(StringBuilder out, List<TheatreProjectLayer.TheatreAct> acts, int level) {
        indent(out, level).append("\"acts\": [");
        if (!acts.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < acts.size(); i++) {
            TheatreProjectLayer.TheatreAct act = acts.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(act.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(act.displayName())); out.append(",\n");
            field(out, level + 2, "notes", quote(act.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < acts.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeScenes(StringBuilder out, List<TheatreProjectLayer.Scene> scenes, int level) {
        indent(out, level).append("\"scenes\": [");
        if (!scenes.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < scenes.size(); i++) {
            TheatreProjectLayer.Scene scene = scenes.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(scene.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(scene.displayName())); out.append(",\n");
            field(out, level + 2, "actId", quote(scene.actId())); out.append(",\n");
            field(out, level + 2, "spatialMapAssetId", quote(scene.spatialMapAssetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(scene.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < scenes.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeSpatialPositions(StringBuilder out, List<TheatreProjectLayer.SpatialPosition> positions, int level) {
        indent(out, level).append("\"positions\": [");
        if (!positions.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < positions.size(); i++) {
            TheatreProjectLayer.SpatialPosition position = positions.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "sceneId", quote(position.sceneId())); out.append(",\n");
            field(out, level + 2, "alias", quote(position.alias())); out.append(",\n");
            field(out, level + 2, "characterId", quote(position.characterId())); out.append(",\n");
            field(out, level + 2, "x", Double.toString(position.x())); out.append(",\n");
            field(out, level + 2, "y", Double.toString(position.y())); out.append(",\n");
            field(out, level + 2, "notes", quote(position.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < positions.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeTheatreActions(StringBuilder out, List<TheatreProjectLayer.TheatreAction> actions, int level) {
        indent(out, level).append("\"actions\": [");
        if (!actions.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < actions.size(); i++) {
            TheatreProjectLayer.TheatreAction action = actions.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "sceneId", quote(action.sceneId())); out.append(",\n");
            field(out, level + 2, "fromAlias", quote(action.fromAlias())); out.append(",\n");
            field(out, level + 2, "toAlias", quote(action.toAlias())); out.append(",\n");
            field(out, level + 2, "characterId", quote(action.characterId())); out.append(",\n");
            field(out, level + 2, "description", quote(action.description())); out.append(",\n");
            field(out, level + 2, "showArrow", Boolean.toString(action.showArrow())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < actions.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeTextActionPlacements(StringBuilder out, List<TheatreProjectLayer.TextActionPlacement> placements, int level) {
        indent(out, level).append("\"textActionPlacements\": [\n");
        for (int i = 0; i < placements.size(); i++) {
            var p = placements.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(p.intervencionId()));
            out.append(",\n");
            field(out, level + 2, "sceneId", quote(p.sceneId()));
            out.append(",\n");
            field(out, level + 2, "characterId", quote(p.characterId()));
            out.append(",\n");
            field(out, level + 2, "origin", quote(p.origin()));
            out.append(",\n");
            field(out, level + 2, "destination", quote(p.destination()));
            out.append(",\n");
            field(out, level + 2, "interactionTarget", quote(p.interactionTarget()));
            out.append(",\n");
            // write characterLocations map
            indent(out, level + 2).append("\"characterLocations\": {\n");
            var locs = p.characterLocations();
            int locIdx = 0;
            for (var entry : locs.entrySet()) {
                indent(out, level + 3).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
                if (locIdx < locs.size() - 1) {
                    out.append(",\n");
                }
                locIdx++;
            }
            out.append("\n");
            indent(out, level + 2).append("}\n");
            indent(out, level + 1).append("}");
            if (i < placements.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeInterventionStates(StringBuilder out, List<TheatreInterventionState> states, int level) {
        indent(out, level).append("\"interventionStates\": [");
        if (!states.isEmpty()) out.append("\n");
        for (int i = 0; i < states.size(); i++) {
            TheatreInterventionState state = states.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "interventionId", quote(state.interventionId())); out.append(",\n");
            field(out, level + 2, "inheritanceMode", quote(state.inheritanceMode().name())); out.append(",\n");
            field(out, level + 2, "inheritsFromInterventionId", quote(state.inheritsFromInterventionId())); out.append(",\n");
            field(out, level + 2, "microexpression", quote(state.microexpression())); out.append(",\n");
            field(out, level + 2, "emoji", quote(state.emoji())); out.append(",\n");
            field(out, level + 2, "tone", quote(state.tone())); out.append(",\n");
            writeCharacterStates(out, state.characters(), level + 2); out.append(",\n");
            writeObjectStates(out, state.objects(), level + 2); out.append(",\n");
            writeStageEvents(out, state.events(), level + 2); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < states.size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeCharacterStates(StringBuilder out, List<TheatreInterventionState.CharacterState> states, int level) {
        indent(out, level).append("\"characters\": ["); if (!states.isEmpty()) out.append("\n");
        for (int i = 0; i < states.size(); i++) { var s = states.get(i); indent(out, level + 1).append("{");
            out.append("\"characterId\": ").append(quote(s.characterId())).append(", ")
                    .append("\"presence\": ").append(quote(s.presence().name())).append(", ")
                    .append("\"position\": ").append(quote(s.position())).append(", ")
                    .append("\"orientation\": ").append(quote(s.orientation())).append(", ")
                    .append("\"gazeTarget\": ").append(quote(s.gazeTarget())).append(", ")
                    .append("\"visualVariantId\": ").append(quote(s.visualVariantId())).append(", ")
                    .append("\"costume\": ").append(quote(s.costume())).append("}");
            if (i < states.size() - 1) out.append(","); out.append("\n"); }
        indent(out, level).append("]");
    }

    private static void writeObjectStates(StringBuilder out, List<TheatreInterventionState.ObjectState> states, int level) {
        indent(out, level).append("\"objects\": ["); if (!states.isEmpty()) out.append("\n");
        for (int i = 0; i < states.size(); i++) { var s = states.get(i); indent(out, level + 1).append("{")
                .append("\"objectId\": ").append(quote(s.objectId())).append(", ")
                .append("\"presence\": ").append(quote(s.presence().name())).append(", ")
                .append("\"position\": ").append(quote(s.position())).append(", ")
                .append("\"holderCharacterId\": ").append(quote(s.holderCharacterId())).append(", ")
                .append("\"manipulation\": ").append(quote(s.manipulation())).append("}");
            if (i < states.size() - 1) out.append(","); out.append("\n"); }
        indent(out, level).append("]");
    }

    private static void writeStageEvents(StringBuilder out, List<TheatreInterventionState.StageEvent> events, int level) {
        indent(out, level).append("\"events\": ["); if (!events.isEmpty()) out.append("\n");
        for (int i = 0; i < events.size(); i++) { var e = events.get(i); indent(out, level + 1).append("{")
                .append("\"type\": ").append(quote(e.type().name())).append(", ")
                .append("\"characterId\": ").append(quote(e.characterId())).append(", ")
                .append("\"objectId\": ").append(quote(e.objectId())).append(", ")
                .append("\"targetCharacterId\": ").append(quote(e.targetCharacterId())).append(", ")
                .append("\"fromPosition\": ").append(quote(e.fromPosition())).append(", ")
                .append("\"toPosition\": ").append(quote(e.toPosition())).append("}");
            if (i < events.size() - 1) out.append(","); out.append("\n"); }
        indent(out, level).append("]");
    }

    static void writeTheatreObjects(StringBuilder out, List<TheatreProjectLayer.TheatreObject> objects, int level) {
        indent(out, level).append("\"objects\": [");
        if (!objects.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < objects.size(); i++) {
            TheatreProjectLayer.TheatreObject object = objects.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(object.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(object.displayName())); out.append(",\n");
            field(out, level + 2, "notes", quote(object.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < objects.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeObjectImages(StringBuilder out, List<TheatreProjectLayer.ObjectImage> images, int level) {
        indent(out, level).append("\"objectImages\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.ObjectImage image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(image.id())); out.append(",\n");
            field(out, level + 2, "objectId", quote(image.objectId())); out.append(",\n");
            field(out, level + 2, "sceneId", quote(image.sceneId())); out.append(",\n");
            field(out, level + 2, "view", quote(image.view())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    @Override public String sectionName() { return "theatre"; }
}
