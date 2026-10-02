package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInteractionTargetPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreStageZone;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Transforms an {@link ImportPlan} into a full theatre project configuration. */
public final class TheatreImportUseCase {

    public ImportResult execute(
            ImportPlan plan,
            NarrationScriptDocument script,
            VoiceLibrary voiceLibrary,
            Map<String, String> importedAssetIds) {
        if (plan == null) {
            return new ImportResult(TheatreProjectLayer.empty(), Map.of(), Map.of(), List.of(), List.of());
        }
        Map<String, String> assetIds = importedAssetIds == null ? Map.of() : importedAssetIds;
        List<TheatreProjectLayer.CharacterProfile> characters = new ArrayList<>();
        List<TheatreProjectLayer.VoiceRoleAlias> voiceRoleAliases = new ArrayList<>();
        List<TheatreProjectLayer.CharacterImage> characterImages = new ArrayList<>();
        List<TheatreProjectLayer.ObjectImage> objectImages = new ArrayList<>();
        List<TheatreProjectLayer.TheatreAct> acts = new ArrayList<>();
        List<TheatreProjectLayer.Scene> scenes = new ArrayList<>();
        List<TheatreProjectLayer.TheatreObject> objects = new ArrayList<>();
        List<TheatreProjectLayer.SpatialPosition> positions = new ArrayList<>();
        List<TheatreProjectLayer.TheatreAction> actions = new ArrayList<>();
        List<TheatreProjectLayer.TextActionPlacement> textActionPlacements = new ArrayList<>();
        List<TheatreProjectLayer.IntervencionVisual> intervencionesVisuales = new ArrayList<>();
        List<TheatreProjectLayer.CameraCue> cameraCues = new ArrayList<>();
        List<TheatreProjectLayer.StageBackdrop> stageBackdrops = new ArrayList<>();
        List<TheatreProjectLayer.StageBackdropAssignment> stageBackdropAssignments = new ArrayList<>();
        List<TheatreProjectLayer.ChoralVoiceAssignment> choralVoiceAssignments = new ArrayList<>();
        List<TheatreInterventionState> interventionStates = new ArrayList<>();
        List<NarrativeLayerAssignment> emotionAssignments = new ArrayList<>();
        List<NarrativeLayerAssignment> imageAssignments = new ArrayList<>();
        Map<String, String> backdropIdsByPath = new LinkedHashMap<>();

        int actSortOrder = 0;
        for (ActPlan actPlan : plan.acts()) {
            String actId = stableEntityId(actPlan.id(), "ACT", actPlan.name());
            acts.add(new TheatreProjectLayer.TheatreAct(actId, actPlan.name(), actPlan.notes()));
            for (ScenePlan scenePlan : actPlan.scenes()) {
                String sceneId = stableEntityId(scenePlan.id(), "SCN", scenePlan.name());
                String spatialMapAssetId = assetIdFor(assetIds, scenePlan.spatialMap());
                scenes.add(new TheatreProjectLayer.Scene(
                        sceneId, scenePlan.name(), scenePlan.notes(), actId, spatialMapAssetId));
                if (!scenePlan.stageBackdrop().isBlank()) {
                    String backdropId = registerStageBackdrop(
                            stageBackdrops, backdropIdsByPath, assetIds, scenePlan.stageBackdrop(), sceneId);
                    if (!backdropId.isBlank()) {
                        stageBackdropAssignments.add(new TheatreProjectLayer.StageBackdropAssignment(
                                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE, sceneId, backdropId,
                                "Fondo de escena importado desde teatro.md."));
                    }
                }
            }
        }
        if (acts.isEmpty()) {
            String actId = "ACT-001";
            acts.add(new TheatreProjectLayer.TheatreAct(actId, "Acto 1", ""));
        }

        for (ProfilePlan cp : plan.characters()) {
            String charId = stableEntityId(cp.id(), "CHR", cp.name());
            List<String> aliases = cp.aliases().isEmpty()
                    ? List.of(cp.name().toUpperCase(Locale.ROOT).replace(" ", "_")) : cp.aliases();
            characters.add(new TheatreProjectLayer.CharacterProfile(charId, cp.name(), aliases, cp.notes()));

            String voiceId = resolveVoiceId(cp.voz(), voiceLibrary);
            voiceRoleAliases.add(new TheatreProjectLayer.VoiceRoleAlias(
                    "VOICE-ROLE-" + charId, cp.name(), voiceId, charId,
                    !cp.voz().isBlank() ? "Voz asignada desde el plan: " + cp.voz() : "Voz por defecto"));

            for (ImageRef imgRef : cp.imagenes()) {
                String assetId = assetIdFor(assetIds, imgRef.path());
                if (assetId != null) {
                    characterImages.add(new TheatreProjectLayer.CharacterImage(
                            "CHARIMG-" + (characterImages.size() + 1),
                            charId, resolveSceneId(imgRef.sceneId(), scenes), viewOrDefault(imgRef.angle()), assetId,
                            "Referencia importada desde teatro.md."));
                }
            }
        }

        for (ProfilePlan op : plan.objects()) {
            String objId = stableEntityId(op.id(), "OBJ", op.name());
            objects.add(new TheatreProjectLayer.TheatreObject(objId, op.name(), op.notes()));
            for (ImageRef imgRef : op.imagenes()) {
                String assetId = assetIdFor(assetIds, imgRef.path());
                if (assetId != null) {
                    objectImages.add(new TheatreProjectLayer.ObjectImage(
                            "OBJIMG-" + (objectImages.size() + 1),
                            objId, resolveSceneId(imgRef.sceneId(), scenes), viewOrDefault(imgRef.angle()), assetId,
                            "Referencia importada desde teatro.md."));
                }
            }
        }

        List<NarrationSegment> cueSegments = interventionSegmentsFromManifestRanges(plan, script);
        Map<String, NarrationSegment> segmentByInterventionId = new LinkedHashMap<>();
        List<TheatreProjectLayer.Intervencion> intervenciones = new ArrayList<>();
        int seq = 1;
        for (NarrationSegment segment : cueSegments) {
            String blockId = segment.sourceBlockIds().isEmpty() ? segment.id() : segment.sourceBlockIds().get(0);
            InterventionPlan planned = seq <= plan.interventions().size() ? plan.interventions().get(seq - 1) : null;
            String interId = segment.metadata().getOrDefault("theatreGrammarInterventionId",
                    planned == null ? "INTERVENCION-" + seq : interventionId(planned));
            intervenciones.add(new TheatreProjectLayer.Intervencion(interId, blockId, seq));
            segmentByInterventionId.put(interId, segment);
            seq++;
        }
        if (intervenciones.isEmpty()) {
            for (InterventionPlan ip : plan.interventions()) {
                String interId = interventionId(ip);
                intervenciones.add(new TheatreProjectLayer.Intervencion(interId, "B" + String.format(Locale.ROOT, "%04d", ip.sequenceIndex()), ip.sequenceIndex()));
            }
        }

        Map<String, String> sceneNameToId = new LinkedHashMap<>();
        for (TheatreProjectLayer.Scene s : scenes) {
            sceneNameToId.put(normalizeSceneName(s.displayName()), s.id());
        }

        Map<String, String> charNameToId = new LinkedHashMap<>();
        Map<String, String> charNameToDisplay = new LinkedHashMap<>();
        for (TheatreProjectLayer.CharacterProfile c : characters) {
            String key = c.displayName().toUpperCase(Locale.ROOT);
            charNameToId.put(key, c.id());
            charNameToDisplay.put(key, c.displayName());
            charNameToId.put(c.id().toUpperCase(Locale.ROOT), c.id());
            for (String alias : c.aliases()) charNameToId.put(alias.toUpperCase(Locale.ROOT), c.id());
        }
        Map<String, String> objectNameToId = new LinkedHashMap<>();
        for (TheatreProjectLayer.TheatreObject object : objects) {
            objectNameToId.put(object.id().toUpperCase(Locale.ROOT), object.id());
            objectNameToId.put(object.displayName().toUpperCase(Locale.ROOT), object.id());
        }

        Map<String, String> aliasByBlockId = new LinkedHashMap<>();
        for (TheatreProjectLayer.Intervencion intervention : intervenciones) {
            aliasByBlockId.putIfAbsent(intervention.blockId(), intervention.id());
        }

        Map<String, List<String>> sceneBoundaryMap = new LinkedHashMap<>();
        for (InterventionPlan ip : plan.interventions()) {
            String sceneName = normalizeSceneName(ip.sceneName());
            sceneBoundaryMap.computeIfAbsent(sceneName, k -> new ArrayList<>())
                    .add(interventionId(ip));
        }

        Map<String, String> sceneBoundariesStart = new LinkedHashMap<>();
        Map<String, String> sceneBoundariesEnd = new LinkedHashMap<>();
        for (ActPlan actPlan : plan.acts()) {
            for (ScenePlan scenePlan : actPlan.scenes()) {
                if (scenePlan.textStartIndex() <= 0 || scenePlan.textEndIndex() <= 0) {
                    continue;
                }
                String sid = sceneNameToId.getOrDefault(normalizeSceneName(scenePlan.name()), "");
                if (sid.isBlank()) {
                    continue;
                }
                String startAlias = aliasByBlockId.get(blockIdForTextIndex(scenePlan.textStartIndex()));
                String endAlias = aliasByBlockId.get(blockIdForTextIndex(scenePlan.textEndIndex()));
                if (startAlias != null && !startAlias.isBlank() && endAlias != null && !endAlias.isBlank()) {
                    sceneBoundariesStart.put(sid, startAlias);
                    sceneBoundariesEnd.put(sid, endAlias);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : sceneBoundaryMap.entrySet()) {
            List<String> ids = entry.getValue();
            if (!ids.isEmpty()) {
                String sid = sceneNameToId.getOrDefault(entry.getKey(), "");
                if (!sid.isBlank() && !sceneBoundariesStart.containsKey(sid)) {
                    sceneBoundariesStart.put(sid, ids.get(0));
                    sceneBoundariesEnd.put(sid, ids.get(ids.size() - 1));
                }
            }
        }

        for (InterventionPlan ip : plan.interventions()) {
            String sceneName = normalizeSceneName(ip.sceneName());
            String sceneId = sceneNameToId.getOrDefault(sceneName, scenes.isEmpty() ? "" : scenes.get(0).id());
            String interId = interventionId(ip);
            String charId = charNameToId.getOrDefault(ip.characterName().toUpperCase(Locale.ROOT), "");

            List<TheatreInterventionState.CharacterState> resolvedCharacterStates = ip.characterStates().stream()
                    .map(value -> new TheatreInterventionState.CharacterState(
                            requiredReference(value.characterId(), charNameToId, "character"), value.presence(),
                            value.position(), value.orientation(), resolveOptionalReference(value.gazeTarget(), charNameToId),
                            value.visualVariantId(), value.costume()))
                    .toList();
            List<TheatreInterventionState.ObjectState> resolvedObjectStates = ip.objectStates().stream()
                    .map(value -> new TheatreInterventionState.ObjectState(
                            requiredReference(value.objectId(), objectNameToId, "object"), value.presence(), value.position(),
                            resolveOptionalReference(value.holderCharacterId(), charNameToId), value.manipulation()))
                    .toList();
            List<TheatreInterventionState.StageEvent> resolvedEvents = ip.stageEvents().stream()
                    .map(value -> new TheatreInterventionState.StageEvent(value.type(),
                            resolveOptionalReference(value.characterId(), charNameToId),
                            resolveOptionalReference(value.objectId(), objectNameToId),
                            resolveOptionalReference(value.targetCharacterId(), charNameToId),
                            value.fromPosition(), value.toPosition()))
                    .toList();
            String inherited = ip.inheritsFromInterventionId().isBlank() ? "" : normalizeInterventionId(ip.inheritsFromInterventionId());
            interventionStates.add(new TheatreInterventionState(interId, ip.inheritanceMode(), inherited,
                    resolvedCharacterStates, resolvedObjectStates, resolvedEvents, ip.microexpression(), ip.emoji(), ip.tono()));

            if (!ip.simultaneousVoiceNames().isEmpty()) {
                List<String> participants = ip.simultaneousVoiceNames().stream()
                        .map(name -> resolveOtherCharacterId(name, charNameToId))
                        .filter(id -> id != null && !id.isBlank())
                        .distinct()
                        .toList();
                if (participants.size() >= 2) {
                    choralVoiceAssignments.add(new TheatreProjectLayer.ChoralVoiceAssignment(
                            interId, participants, "",
                            "grammar:" + interId + ":" + String.join(",", participants),
                            "Voces simultaneas importadas desde teatro.md."));
                }
            }

            if (!ip.cameraCue().isBlank()) {
                cameraCues.add(new TheatreProjectLayer.CameraCue(
                        interId, normalizeCameraId(ip.cameraCue()), "Plano importado desde teatro.md."));
            }

            if (ip.clearStageBackdrop()) {
                stageBackdropAssignments.add(new TheatreProjectLayer.StageBackdropAssignment(
                        TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION, interId,
                        TheatreProjectLayer.STAGE_BACKDROP_NONE, "Sin fondo desde teatro.md."));
            } else if (!ip.stageBackdrop().isBlank()) {
                String backdropId = registerStageBackdrop(
                        stageBackdrops, backdropIdsByPath, assetIds, ip.stageBackdrop(), interId);
                if (!backdropId.isBlank()) {
                    stageBackdropAssignments.add(new TheatreProjectLayer.StageBackdropAssignment(
                            TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION, interId, backdropId,
                            "Fondo heredado importado desde teatro.md."));
                }
            }

            if (!sceneId.isBlank() || !charId.isBlank() || !ip.origin().isBlank() || !ip.destination().isBlank() || !ip.interactionTarget().isBlank()) {
                Map<String, String> characterLocs = new LinkedHashMap<>();
                if (!charId.isBlank() && !ip.origin().isBlank()) {
                    characterLocs.put(displayNameFor(ip.characterName(), charNameToDisplay), ip.origin());
                }
                for (String target : interactionTargets(ip.interactionTarget())) {
                    String otherTarget = resolveOtherCharacterId(target, charNameToId);
                    if (!otherTarget.isBlank() && !otherTarget.equals(charId)) {
                        String targetLocation = ip.destination().isBlank() ? ip.origin() : ip.destination();
                        characterLocs.put(displayNameFor(target, charNameToDisplay), targetLocation);
                    }
                }
                for (TheatreInterventionState.CharacterState state : resolvedCharacterStates) {
                    if (!state.position().isBlank()) characterLocs.put(state.characterId(), state.position());
                }
                textActionPlacements.add(new TheatreProjectLayer.TextActionPlacement(
                        interId, sceneId, charId, ip.origin(), ip.destination(),
                        TheatreInteractionTargetPolicy.canonicalize(ip.interactionTarget()), Map.copyOf(characterLocs)));
            }

            if (!ip.interactionTarget().isBlank()) {
                String fromChar = charNameToId.getOrDefault(ip.characterName().toUpperCase(Locale.ROOT), "");
                for (String target : interactionTargets(ip.interactionTarget())) {
                    String toChar = resolveOtherCharacterId(target, charNameToId);
                    if (fromChar.isBlank() || toChar.isBlank()) {
                        continue;
                    }
                    actions.add(new TheatreProjectLayer.TheatreAction(
                            sceneId, interId, interId, fromChar,
                            ip.characterName() + " interacts with " + target, true));
                }
            }

            if (!charId.isBlank() && !ip.origin().isBlank()) {
                double x = 0.5;
                double y = 0.5;
                try {
                    TheatreStageZone zone = TheatreStageZone.parse(ip.origin());
                    x = zone.x(); y = zone.y();
                } catch (RuntimeException ignored) { }
                positions.add(new TheatreProjectLayer.SpatialPosition(
                        sceneId, interId, charId, x, y, ip.origin()));
            }

            if (script != null && !charId.isBlank() && !ip.tono().isBlank()) {
                NarrationSegment segment = segmentByInterventionId.get(interId);
                if (segment != null) {
                    VoiceReferenceTone tone = safeParseTone(ip.tono());
                    emotionAssignments.add(new NarrativeLayerAssignment(
                            "NLA-EMOTION-" + segment.id(),
                            NarrativeLayerKind.EMOTION,
                            new ScriptTextRange(segment.id(), 0, segment.narrationText().length()),
                            tone.layerTargetId(),
                            "Tono " + tone.name(),
                            "Emoción desde el plan markdown."));
                }
            }

            for (String imgPath : ip.images()) {
                String assetId = assetIdFor(assetIds, imgPath);
                if (assetId != null) {
                    intervencionesVisuales.add(new TheatreProjectLayer.IntervencionVisual(interId, assetId, "Del plan: " + imgPath));
                    NarrationSegment segment = segmentByInterventionId.get(interId);
                    if (segment != null) {
                        imageAssignments.add(new NarrativeLayerAssignment(
                                "NLA-IMAGE-MD-" + interId + "-" + (imageAssignments.size() + 1),
                                NarrativeLayerKind.IMAGE,
                                new ScriptTextRange(segment.id(), 0, segment.narrationText().length()),
                                assetId,
                                "Visual " + interId,
                                "Visual importado desde teatro.md."));
                    }
                }
            }
        }

        return new ImportResult(
                new TheatreProjectLayer(
                        intervenciones, characters, voiceRoleAliases, characterImages,
                        intervencionesVisuales, List.of(), acts, scenes, positions, actions,
                        textActionPlacements, objectImages, objects, List.of(),
                        TheatreBuiltInCameraCatalog.references(), cameraCues, stageBackdrops,
                        stageBackdropAssignments, choralVoiceAssignments, interventionStates),
                sceneBoundariesStart, sceneBoundariesEnd,
                emotionAssignments,
                imageAssignments);
    }

    public record ImportResult(
            TheatreProjectLayer layer,
            Map<String, String> sceneBoundariesStart,
            Map<String, String> sceneBoundariesEnd,
            List<NarrativeLayerAssignment> emotionAssignments,
            List<NarrativeLayerAssignment> imageAssignments) {
    }

    private static String normalizeId(String prefix, String name) {
        if (name == null || name.isBlank()) return prefix + "-UNKNOWN";
        String normalized = name.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return prefix + "-" + (normalized.isBlank() ? "UNKNOWN" : normalized);
    }

    public static String stableEntityId(String explicitId, String prefix, String name) {
        if (explicitId == null || explicitId.isBlank()) return normalizeId(prefix, name);
        String value = explicitId.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!value.matches("[A-Z][A-Z0-9_-]{0,63}")) throw new IllegalArgumentException("Invalid stable theatre id: " + explicitId);
        return value;
    }

    private static String interventionId(InterventionPlan plan) {
        return plan.interventionId().isBlank() ? "INTERVENCION-" + plan.sequenceIndex()
                : normalizeInterventionId(plan.interventionId());
    }

    private static String normalizeInterventionId(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (normalized.matches("INTERVENCION-\\d+")) return normalized;
        if (normalized.matches("\\d+")) return "INTERVENCION-" + Integer.parseInt(normalized);
        throw new IllegalArgumentException("Invalid intervention id: " + value);
    }

    private static String requiredReference(String value, Map<String, String> ids, String kind) {
        String resolved = resolveOptionalReference(value, ids);
        if (resolved.isBlank()) throw new IllegalArgumentException("Unknown theatre " + kind + " reference: " + value);
        return resolved;
    }

    private static String resolveOptionalReference(String value, Map<String, String> ids) {
        if (value == null || value.isBlank()) return "";
        return ids.getOrDefault(value.strip().toUpperCase(Locale.ROOT), "");
    }

    private static String normalizeCameraId(String cameraId) {
        return cameraId == null ? "" : cameraId.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String registerStageBackdrop(
            List<TheatreProjectLayer.StageBackdrop> stageBackdrops,
            Map<String, String> backdropIdsByPath,
            Map<String, String> assetIds,
            String requestedPath,
            String scopeId) {
        String key = normalizeAssetKey(requestedPath);
        if (key.isBlank()) {
            return "";
        }
        String existing = backdropIdsByPath.get(key);
        if (existing != null) {
            return existing;
        }
        String assetId = assetIdFor(assetIds, requestedPath);
        if (assetId == null || assetId.isBlank()) {
            return "";
        }
        String displayName = displayNameForBackdrop(requestedPath);
        String id = normalizeId("BDR", scopeId + " " + displayName + " " + (stageBackdrops.size() + 1));
        stageBackdrops.add(new TheatreProjectLayer.StageBackdrop(
                id, displayName, assetId, "Fondo importado desde teatro.md: " + requestedPath.strip()));
        backdropIdsByPath.put(key, id);
        return id;
    }

    private static String normalizeAssetKey(String value) {
        return value == null ? "" : value.strip().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static String displayNameForBackdrop(String requestedPath) {
        String normalized = requestedPath == null ? "" : requestedPath.strip().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String file = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = file.lastIndexOf('.');
        String name = dot > 0 ? file.substring(0, dot) : file;
        return name.isBlank() ? "Fondo de escenario" : name.replace('_', ' ').replace('-', ' ');
    }

    private static String normalizeSceneName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String resolveSceneId(String candidate, List<TheatreProjectLayer.Scene> scenes) {
        if (candidate == null || candidate.isBlank()) return "";
        String normalized = candidate.trim();
        for (TheatreProjectLayer.Scene s : scenes) {
            if (s.id().equalsIgnoreCase(normalized) || s.displayName().equalsIgnoreCase(normalized)) {
                return s.id();
            }
            if (normalizeSceneName(s.displayName()).equals(normalizeSceneName(normalized))) {
                return s.id();
            }
        }
        if (normalized.startsWith("SCN-") || normalized.startsWith("ESC-")) return normalized;
        return "";
    }

    public static String resolveVoiceId(String vozId, VoiceLibrary voiceLibrary) {
        if (vozId == null || vozId.isBlank()) {
            if (voiceLibrary != null && !voiceLibrary.voices().isEmpty()) return voiceLibrary.voices().get(0).id();
            return "VOICE-DEFAULT";
        }
        if (voiceLibrary != null) {
            Optional<com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile> profile = voiceLibrary.voiceById(vozId);
            if (profile.isPresent()) return profile.get().id();
            var matches = voiceLibrary.voices().stream().filter(v ->
                    v.id().equalsIgnoreCase(vozId) || v.displayName().equalsIgnoreCase(vozId)
                    || vozId.equalsIgnoreCase(v.metadata().getOrDefault("sourceFolder", ""))).toList();
            if (matches.size() == 1) return matches.getFirst().id();
            var preset = com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog.profiles()
                    .stream().filter(v -> vozId.equalsIgnoreCase(v.metadata().get("sourceFolder")))
                    .filter(v -> voiceLibrary.voiceById(v.id()).isPresent()).findFirst();
            if (preset.isPresent()) return preset.get().id();
        }
        return vozId;
    }

    private static String resolveOtherCharacterId(String target, Map<String, String> charNameToId) {
        if (target == null || target.isBlank()) return "";
        String upper = target.toUpperCase(Locale.ROOT);
        if (charNameToId.containsKey(upper)) return charNameToId.get(upper);
        for (Map.Entry<String, String> e : charNameToId.entrySet()) {
            if (e.getKey().contains(upper) || upper.contains(e.getKey())) return e.getValue();
        }
        return "";
    }

    private static List<String> interactionTargets(String target) {
        String normalized = target == null ? "" : target.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] parts = normalized.split("\\s*(?:,|;|/|\\s+y\\s+)\\s*");
        ArrayList<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.strip());
            }
        }
        return result.isEmpty() ? List.of(normalized) : List.copyOf(result);
    }

    private static String viewOrDefault(String view) {
        return view == null || view.isBlank() ? "Referencia" : view.strip();
    }

    private static String displayNameFor(String name, Map<String, String> charNameToDisplay) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (charNameToDisplay.containsKey(upper)) {
            return charNameToDisplay.get(upper);
        }
        for (Map.Entry<String, String> entry : charNameToDisplay.entrySet()) {
            if (entry.getKey().contains(upper) || upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return name.strip();
    }

    private static String assetIdFor(Map<String, String> importedAssetIds, String requestedPath) {
        if (importedAssetIds == null || importedAssetIds.isEmpty() || requestedPath == null || requestedPath.isBlank()) {
            return null;
        }
        String normalized = requestedPath.strip().replace('\\', '/');
        ArrayList<String> candidates = new ArrayList<>();
        candidates.add(requestedPath.strip());
        candidates.add(normalized);
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
            candidates.add(normalized);
        }
        if (normalized.startsWith("assets/")) {
            candidates.add(normalized.substring("assets/".length()));
        }
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            candidates.add(normalized.substring(slash + 1));
        }
        for (String candidate : candidates) {
            String id = importedAssetIds.get(candidate);
            if (id != null) {
                return id;
            }
            id = importedAssetIds.get(candidate.toLowerCase(Locale.ROOT));
            if (id != null) {
                return id;
            }
        }
        for (Map.Entry<String, String> entry : importedAssetIds.entrySet()) {
            for (String candidate : candidates) {
                if (entry.getKey().equalsIgnoreCase(candidate)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static List<NarrationSegment> theatricalCueSegments(NarrationScriptDocument script) {
        if (script == null || script.empty()) {
            return List.of();
        }
        ArrayList<NarrationSegment> result = new ArrayList<>();
        Set<String> seenBlocks = new LinkedHashSet<>();
        for (NarrationSegment segment : script.segments()) {
            if (segment == null || !segment.narratable() || !isTheatricalCue(segment.narrationText())) {
                continue;
            }
            String blockId = segment.sourceBlockIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(segment.id());
            if (seenBlocks.add(blockId)) {
                result.add(segment);
            }
        }
        return List.copyOf(result);
    }

    private static List<NarrationSegment> interventionSegmentsFromManifestRanges(ImportPlan plan, NarrationScriptDocument script) {
        if (script != null && script.segments().stream().anyMatch(s -> s.metadata().containsKey("theatreGrammarInterventionId"))) {
            Map<String, NarrationSegment> byId = new LinkedHashMap<>();
            script.segments().forEach(s -> byId.put(s.metadata().get("theatreGrammarInterventionId"), s));
            return plan.interventions().stream().map(i -> {
                NarrationSegment segment = byId.get(i.stableInterventionId());
                if (segment == null) throw new IllegalArgumentException("Falta el parlamento: " + i.stableInterventionId());
                return segment;
            }).toList();
        }
        if (script == null || script.empty()) {
            return List.of();
        }
        List<NarrationSegment> orderedSegments = orderedDocumentSegments(script);
        ArrayList<NarrationSegment> result = new ArrayList<>();
        Set<String> seenBlocks = new LinkedHashSet<>();
        if (plan != null) {
            for (ActPlan actPlan : plan.acts()) {
                for (ScenePlan scenePlan : actPlan.scenes()) {
                    if (scenePlan.textStartIndex() <= 0 || scenePlan.textEndIndex() < scenePlan.textStartIndex()) {
                        continue;
                    }
                    int start = Math.max(1, scenePlan.textStartIndex());
                    int end = Math.min(scenePlan.textEndIndex(), orderedSegments.size());
                    for (int textIndex = start; textIndex <= end; textIndex++) {
                        NarrationSegment segment = orderedSegments.get(textIndex - 1);
                        String blockId = firstBlockId(segment);
                        if (segment.narratable() && !blockId.isBlank() && seenBlocks.add(blockId)) {
                            result.add(segment);
                        }
                    }
                }
            }
        }
        if (!result.isEmpty()) {
            return List.copyOf(result);
        }
        return theatricalCueSegments(script);
    }

    private static List<NarrationSegment> orderedDocumentSegments(NarrationScriptDocument script) {
        ArrayList<NarrationSegment> ordered = new ArrayList<>();
        Set<String> seenBlocks = new LinkedHashSet<>();
        for (NarrationSegment segment : script.segments()) {
            if (segment == null) {
                continue;
            }
            String blockId = firstBlockId(segment);
            if (blockId.isBlank()) {
                blockId = segment.id();
            }
            if (seenBlocks.add(blockId)) {
                ordered.add(segment);
            }
        }
        return List.copyOf(ordered);
    }

    private static String firstBlockId(NarrationSegment segment) {
        if (segment == null) {
            return "";
        }
        return segment.sourceBlockIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse("");
    }

    private static String blockIdForTextIndex(int textIndex) {
        return "B" + String.format(Locale.ROOT, "%04d", Math.max(0, textIndex));
    }

    private static NarrationSegment findMatchingSegment(int interIndex, List<NarrationSegment> cueSegments) {
        if (cueSegments == null || interIndex < 0 || interIndex >= cueSegments.size()) {
            return null;
        }
        return cueSegments.get(interIndex);
    }

    private static boolean isTheatricalCue(String text) {
        String normalized = normalizeCue(text);
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.startsWith("ACOTACION:")
                || normalized.startsWith("ACOTACION ")
                || normalized.startsWith("(")
                || normalized.startsWith("[")) {
            return true;
        }
        int colon = normalized.indexOf(':');
        if (colon < 2 || colon > 42) {
            return false;
        }
        String cue = normalized.substring(0, colon).strip();
        if (cue.equals("ESCENA") || cue.startsWith("ESCENA ")
                || cue.equals("ACTO") || cue.startsWith("ACTO ")) {
            return false;
        }
        boolean hasLetter = cue.chars().anyMatch(Character::isLetter);
        boolean legalCue = cue.chars().allMatch(ch ->
                Character.isLetterOrDigit(ch) || ch == ' ' || ch == '.' || ch == '-' || ch == '_' || ch == '\'');
        return hasLetter && legalCue;
    }

    private static String normalizeCue(String text) {
        String value = text == null ? "" : text.replace('\u00A0', ' ').strip();
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replaceAll("[\\s\\u00A0]+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static VoiceReferenceTone safeParseTone(String tono) {
        if (tono == null || tono.isBlank()) return VoiceReferenceTone.NEUTRAL;
        return VoiceReferenceTone.fromLayerTargetId(tono).orElseThrow(() ->
                new IllegalArgumentException("Tono teatral desconocido: " + tono
                        + ". Usa un tono de la biblioteca, por ejemplo ANGRY o enojado; no se sustituirá por neutral."));
    }
}
