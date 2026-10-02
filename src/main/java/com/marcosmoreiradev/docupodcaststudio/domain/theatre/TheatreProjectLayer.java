package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional theatre metadata stored on top of the normal document project.
 *
 * <p>Cada intervencion (INTERVENCION-1, INTERVENCION-2...) identifica un
 * fragmento de dialogo en secuencia. No son identificadores de personaje.
 * Los alias dramaticos como "El villano" pertenecen a personajes/roles y
 * pueden apuntar a un perfil de voz real sin renombrar dicha voz.</p>
 */
public record TheatreProjectLayer(
        List<Intervencion> intervenciones,
        List<CharacterProfile> characters,
        List<VoiceRoleAlias> voiceRoleAliases,
        List<CharacterImage> characterImages,
        List<IntervencionVisual> intervencionesVisuales,
        List<IntermediateFrame> intermediateFrames,
        List<TheatreAct> acts,
        List<Scene> scenes,
        List<SpatialPosition> positions,
        List<TheatreAction> actions,
        List<TextActionPlacement> textActionPlacements,
        List<ObjectImage> objectImages,
        List<TheatreObject> objects,
        List<TheatreAudioTrack> audioTracks,
        List<CameraReference> cameraReferences,
        List<CameraCue> cameraCues,
        List<StageBackdrop> stageBackdrops,
        List<StageBackdropAssignment> stageBackdropAssignments,
        List<ChoralVoiceAssignment> choralVoiceAssignments,
        List<TheatreInterventionState> interventionStates
) {
    public static final String DEFAULT_CAMERA_ID = "CERCA_CENTRO_NIVEL";
    public static final String STAGE_BACKDROP_SCOPE_SCENE = "SCENE";
    public static final String STAGE_BACKDROP_SCOPE_INTERVENTION = "INTERVENTION";
    public static final String STAGE_BACKDROP_NONE = "SIN_FONDO";

    public TheatreProjectLayer {
        intervenciones = copy(intervenciones);
        characters = copy(characters);
        voiceRoleAliases = copy(voiceRoleAliases);
        characterImages = copy(characterImages);
        intervencionesVisuales = copy(intervencionesVisuales);
        intermediateFrames = copy(intermediateFrames);
        acts = copy(acts);
        scenes = copy(scenes);
        positions = copy(positions);
        actions = copy(actions);
        textActionPlacements = copy(textActionPlacements);
        objectImages = copy(objectImages);
        objects = copy(objects);
        audioTracks = copy(audioTracks);
        cameraReferences = copy(cameraReferences);
        cameraCues = copy(cameraCues);
        stageBackdrops = copy(stageBackdrops);
        stageBackdropAssignments = copy(stageBackdropAssignments);
        choralVoiceAssignments = copy(choralVoiceAssignments);
        interventionStates = copy(interventionStates);
    }

    /** Compatibility constructor for projects and callers created before deterministic stage state. */
    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<IntermediateFrame> intermediateFrames,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TextActionPlacement> textActionPlacements,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects,
            List<TheatreAudioTrack> audioTracks,
            List<CameraReference> cameraReferences,
            List<CameraCue> cameraCues,
            List<StageBackdrop> stageBackdrops,
            List<StageBackdropAssignment> stageBackdropAssignments,
            List<ChoralVoiceAssignment> choralVoiceAssignments
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales,
                intermediateFrames, acts, scenes, positions, actions, textActionPlacements, objectImages, objects,
                audioTracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, List.of());
    }

    /** Compatibility constructor for callers created before theatre choral voice metadata. */
    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<IntermediateFrame> intermediateFrames,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TextActionPlacement> textActionPlacements,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects,
            List<TheatreAudioTrack> audioTracks,
            List<CameraReference> cameraReferences,
            List<CameraCue> cameraCues,
            List<StageBackdrop> stageBackdrops,
            List<StageBackdropAssignment> stageBackdropAssignments
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales,
                intermediateFrames, acts, scenes, positions, actions, textActionPlacements, objectImages, objects,
                audioTracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments, List.of());
    }

    /** Compatibility constructor for callers created before theatre camera and backdrop metadata. */
    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<IntermediateFrame> intermediateFrames,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TextActionPlacement> textActionPlacements,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects,
            List<TheatreAudioTrack> audioTracks
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales,
                intermediateFrames, acts, scenes, positions, actions, textActionPlacements, objectImages, objects,
                audioTracks, List.of(), List.of(), List.of(), List.of());
    }

    /** Compatibility constructor for projects and callers created before persisted intermediate frames. */
    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TextActionPlacement> textActionPlacements,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects,
            List<TheatreAudioTrack> audioTracks
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), acts,
                scenes, positions, actions, textActionPlacements, objectImages, objects, audioTracks);
    }

    /** Compatibility constructor for projects and callers created before theatre audio tracks. */
    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TextActionPlacement> textActionPlacements,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), acts, scenes,
                positions, actions, textActionPlacements, objectImages, objects, List.of());
    }

    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TheatreObject> objects
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), acts, scenes, positions, actions, List.of(), List.of(), objects, List.of());
    }

    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<TheatreAct> acts,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<ObjectImage> objectImages,
            List<TheatreObject> objects
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), acts, scenes, positions, actions, List.of(), objectImages, objects, List.of());
    }

    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), List.of(), scenes, positions, actions, List.of(), List.of(), List.of(), List.of());
    }

    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<VoiceRoleAlias> voiceRoleAliases,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions,
            List<TheatreObject> objects
    ) {
        this(intervenciones, characters, voiceRoleAliases, characterImages, intervencionesVisuales, List.of(), List.of(), scenes, positions, actions, List.of(), List.of(), objects, List.of());
    }

    public TheatreProjectLayer(
            List<Intervencion> intervenciones,
            List<CharacterProfile> characters,
            List<CharacterImage> characterImages,
            List<IntervencionVisual> intervencionesVisuales,
            List<Scene> scenes,
            List<SpatialPosition> positions,
            List<TheatreAction> actions
    ) {
        this(intervenciones, characters, List.of(), characterImages, intervencionesVisuales, List.of(), List.of(), scenes, positions, actions, List.of(), List.of(), List.of(), List.of());
    }

    public static TheatreProjectLayer empty() {
        return new TheatreProjectLayer(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public TheatreProjectLayer withTextActionPlacements(List<TextActionPlacement> placements) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, placements, objectImages,
                objects, audioTracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withIntervencionesVisuales(List<IntervencionVisual> visuals) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                visuals, intermediateFrames, acts, scenes, positions, actions, textActionPlacements, objectImages,
                objects, audioTracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withCharacterImages(List<CharacterImage> images) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, images,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions,
                textActionPlacements, objectImages, objects, audioTracks, cameraReferences, cameraCues,
                stageBackdrops, stageBackdropAssignments, choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withObjectImages(List<ObjectImage> images) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions,
                textActionPlacements, images, objects, audioTracks, cameraReferences, cameraCues,
                stageBackdrops, stageBackdropAssignments, choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withScenes(List<Scene> updatedScenes) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, updatedScenes, positions, actions,
                textActionPlacements, objectImages, objects, audioTracks, cameraReferences, cameraCues,
                stageBackdrops, stageBackdropAssignments, choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withIntermediateFrames(List<IntermediateFrame> frames) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, frames, acts, scenes, positions, actions, textActionPlacements, objectImages,
                objects, audioTracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withAudioTracks(List<TheatreAudioTrack> tracks) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, tracks, cameraReferences, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withCameraReferences(List<CameraReference> references) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, references, cameraCues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withCameraCues(List<CameraCue> cues) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, cameraReferences, cues, stageBackdrops, stageBackdropAssignments,
                choralVoiceAssignments);
    }

    public TheatreProjectLayer withStageBackdrops(List<StageBackdrop> backdrops) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, cameraReferences, cameraCues, backdrops,
                stageBackdropAssignments, choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withStageBackdropAssignments(List<StageBackdropAssignment> assignments) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, cameraReferences, cameraCues, stageBackdrops, assignments,
                choralVoiceAssignments, interventionStates);
    }

    public TheatreProjectLayer withChoralVoiceAssignments(List<ChoralVoiceAssignment> assignments) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, cameraReferences, cameraCues, stageBackdrops,
                stageBackdropAssignments, assignments, interventionStates);
    }

    public TheatreProjectLayer withInterventionStates(List<TheatreInterventionState> states) {
        return new TheatreProjectLayer(intervenciones, characters, voiceRoleAliases, characterImages,
                intervencionesVisuales, intermediateFrames, acts, scenes, positions, actions, textActionPlacements,
                objectImages, objects, audioTracks, cameraReferences, cameraCues, stageBackdrops,
                stageBackdropAssignments, choralVoiceAssignments, states);
    }

    private static <T> List<T> copy(List<T> value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        value.forEach(item -> Objects.requireNonNull(item, "theatre item"));
        return List.copyOf(value);
    }

    public record Intervencion(String id, String blockId, int sequenceIndex) {
        public Intervencion {
            id = require(id, "id (INTERVENCION-N)");
            blockId = require(blockId, "blockId");
            if (!id.matches("INTERVENCION-\\d+")) {
                throw new IllegalArgumentException("id must use INTERVENCION-1, INTERVENCION-2...");
            }
            if (sequenceIndex < 1) {
                throw new IllegalArgumentException("sequenceIndex must be positive");
            }
        }

        public static Intervencion ofSequence(int sequenceIndex, String blockId) {
            return new Intervencion("INTERVENCION-" + sequenceIndex, blockId, sequenceIndex);
        }
    }

    public record CharacterProfile(String id, String displayName, List<String> aliases, String notes) {
        public CharacterProfile {
            id = require(id, "id");
            displayName = require(displayName, "displayName");
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            notes = normalize(notes);
        }
    }

    public record VoiceRoleAlias(
            String id,
            String displayName,
            String voiceProfileId,
            String characterId,
            String notes
    ) {
        public VoiceRoleAlias {
            id = require(id, "id");
            displayName = require(displayName, "displayName");
            voiceProfileId = require(voiceProfileId, "voiceProfileId");
            characterId = normalize(characterId);
            notes = normalize(notes);
        }
    }

    public record CharacterImage(String id, String characterId, String sceneId, String view, String assetId, String notes) {
        public CharacterImage(String characterId, String view, String assetId, String notes) {
            this("", characterId, "", view, assetId, notes);
        }

        public CharacterImage(String characterId, String sceneId, String view, String assetId, String notes) {
            this("", characterId, sceneId, view, assetId, notes);
        }

        public CharacterImage {
            id = normalize(id);
            characterId = require(characterId, "characterId");
            sceneId = normalize(sceneId);
            view = require(view, "view");
            assetId = require(assetId, "assetId");
            notes = normalize(notes);
        }
    }

    public record IntervencionVisual(String intervencionId, String assetId, String notes) {
        public IntervencionVisual {
            intervencionId = require(intervencionId, "intervencionId");
            assetId = require(assetId, "assetId");
            notes = normalize(notes);
        }
    }

    public record IntermediateFrame(String fromIntervencionId, String toIntervencionId, String assetId, String notes) {
        public IntermediateFrame {
            fromIntervencionId = require(fromIntervencionId, "fromIntervencionId");
            toIntervencionId = require(toIntervencionId, "toIntervencionId");
            assetId = require(assetId, "assetId");
            notes = normalize(notes);
            if (fromIntervencionId.equals(toIntervencionId)) {
                throw new IllegalArgumentException("Intermediate frame endpoints must be different");
            }
        }
    }

    public record ChoralVoiceAssignment(
            String intervencionId,
            List<String> participantCharacterIds,
            String mixedAudioAssetId,
            String sourceFingerprint,
            String notes
    ) {
        public ChoralVoiceAssignment {
            intervencionId = require(intervencionId, "choralVoiceAssignment.intervencionId");
            participantCharacterIds = participantCharacterIds == null ? List.of() : participantCharacterIds.stream()
                    .map(value -> require(value, "choralVoiceAssignment.participantCharacterId"))
                    .toList();
            mixedAudioAssetId = normalize(mixedAudioAssetId);
            sourceFingerprint = normalize(sourceFingerprint);
            notes = normalize(notes);
        }
    }

    public record CameraReference(
            String id,
            String displayName,
            String assetId,
            String distance,
            String orientation,
            String height,
            boolean defaultCamera,
            String notes
    ) {
        public CameraReference {
            id = require(id, "cameraReference.id");
            displayName = require(displayName, "cameraReference.displayName");
            assetId = require(assetId, "cameraReference.assetId");
            distance = normalize(distance);
            orientation = normalize(orientation);
            height = normalize(height);
            notes = normalize(notes);
        }
    }

    public record CameraCue(String intervencionId, String cameraId, String notes) {
        public CameraCue {
            intervencionId = require(intervencionId, "cameraCue.intervencionId");
            cameraId = require(cameraId, "cameraCue.cameraId");
            notes = normalize(notes);
        }
    }

    public record StageBackdrop(String id, String displayName, String assetId, String notes) {
        public StageBackdrop {
            id = require(id, "stageBackdrop.id");
            displayName = require(displayName, "stageBackdrop.displayName");
            assetId = require(assetId, "stageBackdrop.assetId");
            notes = normalize(notes);
        }
    }

    public record StageBackdropAssignment(String scope, String scopeId, String backdropId, String notes) {
        public StageBackdropAssignment {
            scope = require(scope, "stageBackdropAssignment.scope").toUpperCase(java.util.Locale.ROOT);
            if (!scope.equals(STAGE_BACKDROP_SCOPE_SCENE) && !scope.equals(STAGE_BACKDROP_SCOPE_INTERVENTION)) {
                throw new IllegalArgumentException("stageBackdropAssignment.scope must be SCENE or INTERVENTION");
            }
            scopeId = require(scopeId, "stageBackdropAssignment.scopeId");
            backdropId = require(backdropId, "stageBackdropAssignment.backdropId");
            notes = normalize(notes);
        }
    }

    public enum AudioTrackEndMode {
        FILE_END,
        SOURCE_TIME
    }

    /** Background audio anchored to one intervention and allowed to continue across later interventions. */
    public record TheatreAudioTrack(
            String id,
            String assetId,
            String startIntervencionId,
            String startSegmentId,
            double sourceStartSeconds,
            double sourceEndSeconds,
            AudioTrackEndMode endMode,
            double volume,
            double sourceDurationSeconds,
            boolean gentleFade
    ) {
        public TheatreAudioTrack(String id, String assetId, String startIntervencionId,
                                 double sourceStartSeconds, double sourceEndSeconds,
                                 AudioTrackEndMode endMode, double volume, double sourceDurationSeconds) {
            this(id, assetId, startIntervencionId, "", sourceStartSeconds, sourceEndSeconds,
                    endMode, volume, sourceDurationSeconds, false);
        }

        public TheatreAudioTrack {
            id = require(id, "id");
            assetId = require(assetId, "assetId");
            startIntervencionId = normalize(startIntervencionId);
            startSegmentId = normalize(startSegmentId);
            if (startIntervencionId.isBlank() && startSegmentId.isBlank()) {
                throw new IllegalArgumentException("A theatre audio track requires a segment or intervention anchor");
            }
            sourceStartSeconds = finiteNonNegative(sourceStartSeconds, "sourceStartSeconds");
            sourceEndSeconds = finiteNonNegative(sourceEndSeconds, "sourceEndSeconds");
            sourceDurationSeconds = finiteNonNegative(sourceDurationSeconds, "sourceDurationSeconds");
            endMode = endMode == null ? AudioTrackEndMode.FILE_END : endMode;
            volume = Double.isFinite(volume) ? Math.max(0.0, Math.min(1.0, volume)) : 0.30;
            if (sourceDurationSeconds <= sourceStartSeconds) {
                throw new IllegalArgumentException("sourceStartSeconds must be before the end of the audio file");
            }
            if (endMode == AudioTrackEndMode.SOURCE_TIME
                    && (sourceEndSeconds <= sourceStartSeconds || sourceEndSeconds > sourceDurationSeconds)) {
                throw new IllegalArgumentException("sourceEndSeconds must be after sourceStartSeconds and inside the audio file");
            }
        }

        public double effectiveEndSeconds() {
            return endMode == AudioTrackEndMode.FILE_END ? sourceDurationSeconds : sourceEndSeconds;
        }

        public double playbackDurationSeconds() {
            return Math.max(0.0, effectiveEndSeconds() - sourceStartSeconds);
        }

        public double fadeDurationSeconds() {
            if (!gentleFade) return 0.0;
            return Math.min(0.5, playbackDurationSeconds() * 0.25);
        }
    }

    public record ObjectImage(String id, String objectId, String sceneId, String view, String assetId, String notes) {
        public ObjectImage(String objectId, String sceneId, String view, String assetId, String notes) {
            this("", objectId, sceneId, view, assetId, notes);
        }

        public ObjectImage {
            id = normalize(id);
            objectId = require(objectId, "objectId");
            sceneId = normalize(sceneId);
            view = require(view, "view");
            assetId = require(assetId, "assetId");
            notes = normalize(notes);
        }
    }

    public record TheatreAct(String id, String displayName, String notes) {
        public TheatreAct {
            id = require(id, "id");
            displayName = require(displayName, "displayName");
            notes = normalize(notes);
        }
    }

    public record Scene(String id, String displayName, String notes, String actId, String spatialMapAssetId) {
        public Scene(String id, String displayName, String notes) {
            this(id, displayName, notes, "", "");
        }

        public Scene(String id, String displayName, String notes, String actId) {
            this(id, displayName, notes, actId, "");
        }

        public Scene {
            id = require(id, "id");
            displayName = require(displayName, "displayName");
            notes = normalize(notes);
            actId = normalize(actId);
            spatialMapAssetId = normalize(spatialMapAssetId);
        }
    }

    public record SpatialPosition(String sceneId, String alias, String characterId, double x, double y, String notes) {
        public SpatialPosition {
            sceneId = require(sceneId, "sceneId");
            alias = require(alias, "alias");
            characterId = normalize(characterId);
            notes = normalize(notes);
        }
    }

    public record TheatreAction(
            String sceneId,
            String fromAlias,
            String toAlias,
            String characterId,
            String description,
            boolean showArrow
    ) {
        public TheatreAction {
            sceneId = require(sceneId, "sceneId");
            fromAlias = require(fromAlias, "fromAlias");
            toAlias = require(toAlias, "toAlias");
            characterId = normalize(characterId);
            description = normalize(description);
        }
    }

    public record TextActionPlacement(
            String intervencionId,
            String sceneId,
            String characterId,
            String origin,
            String destination,
            String interactionTarget,
            Map<String, String> characterLocations
    ) {
        public TextActionPlacement {
            intervencionId = require(intervencionId, "intervencionId");
            sceneId = require(sceneId, "sceneId");
            characterId = normalize(characterId);
            origin = normalize(origin);
            destination = normalize(destination);
            interactionTarget = normalize(interactionTarget);
            characterLocations = characterLocations == null ? Map.of() : Map.copyOf(characterLocations);
        }

        public static TextActionPlacement empty() {
            return new TextActionPlacement("_", "_", "", "", "", "", Map.of());
        }
    }

    public record TheatreObject(String id, String displayName, String notes) {
        public TheatreObject {
            id = require(id, "id");
            displayName = require(displayName, "displayName");
            notes = normalize(notes);
        }
    }

    private static String require(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static double finiteNonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(field + " must be finite and non-negative");
        }
        return value;
    }
}
