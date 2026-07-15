package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds the operational Produccion teatral view without changing persisted theatre schema. */
public final class BuildTheatreProductionProjectionUseCase {
    private final TheatreFragmentLinkPolicy linkPolicy;

    public BuildTheatreProductionProjectionUseCase() {
        this(new TheatreFragmentLinkPolicy());
    }

    public BuildTheatreProductionProjectionUseCase(TheatreFragmentLinkPolicy linkPolicy) {
        this.linkPolicy = linkPolicy == null ? new TheatreFragmentLinkPolicy() : linkPolicy;
    }

    public TheatreProductionProjection build(
            DocuPodcastProject project,
            ReadableDocument document,
            NarrationScriptDocument script,
            FragmentWorkspaceProjection fragmentProjection,
            List<AudioJobSnapshot> audioJobs,
            PlaybackManifest manifest
    ) {
        TheatreProjectLayer theatre = project == null ? TheatreProjectLayer.empty() : project.theatre();
        String title = project == null ? (document == null ? "Produccion teatral" : document.title()) : project.metadata().title();
        Map<String, NarrationSegment> segmentsById = segmentsById(script);
        Map<String, NarrationSegment> segmentsByBlock = segmentsByBlock(script);
        Set<String> completedAudioSegments = completedAudioSegments(audioJobs, manifest);
        Set<String> assetIds = assetIds(project);
        Map<String, TheatreProjectLayer.CharacterProfile> characters = charactersById(theatre);
        Map<String, TheatreProjectLayer.TheatreObject> objects = objectsById(theatre);
        Map<String, TheatreProjectLayer.TheatreAct> acts = actsById(theatre);
        Map<String, TheatreProjectLayer.Scene> scenes = scenesById(theatre);
        Map<String, TheatreProjectLayer.TextActionPlacement> placements = placementsByIntervention(theatre);
        Set<String> visualReadyInterventions = visualReadyInterventions(theatre, assetIds);
        Set<String> fragmentAudioReady = fragmentAudioReady(fragmentProjection);
        Set<String> fragmentVisualReady = fragmentVisualReady(fragmentProjection);

        ArrayList<String> diagnostics = new ArrayList<>();
        diagnostics.addAll(referenceDiagnostics(theatre, assetIds, characters, objects, acts, scenes));

        ArrayList<TheatreProductionIntervention> interventions = new ArrayList<>();
        int linked = 0;
        int audioReady = 0;
        int visualReady = 0;
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            TheatreFragmentLink link = linkPolicy.linkForIntervention(theatre, intervention, fragmentProjection);
            Optional<DocumentFragment> fragment = fragmentProjection == null
                    ? Optional.empty()
                    : fragmentProjection.fragmentById(link.fragmentId());
            String segmentId = !link.segmentId().isBlank()
                    ? link.segmentId()
                    : segmentsByBlock.getOrDefault(intervention.blockId(), emptySegment()).id();
            if (segmentId.equals("_")) {
                segmentId = "";
            }
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            String characterId = placement == null ? "" : placement.characterId();
            String sceneId = !link.sceneId().isBlank() ? link.sceneId() : (placement == null ? "" : placement.sceneId());
            boolean linkedToFragment = link.linked();
            boolean interventionAudio = audioFor(link.fragmentId(), segmentId, fragmentAudioReady, completedAudioSegments);
            boolean interventionVisual = visualReadyInterventions.contains(intervention.id())
                    || fragmentVisualReady.contains(link.fragmentId().value());
            if (linkedToFragment) {
                linked++;
            }
            if (interventionAudio) {
                audioReady++;
            }
            if (interventionVisual) {
                visualReady++;
            }
            ArrayList<String> interventionDiagnostics = new ArrayList<>();
            if (!linkedToFragment) {
                interventionDiagnostics.add("Intervencion " + intervention.id() + " no resuelve un FragmentId existente.");
            }
            if (sceneId.isBlank()) {
                interventionDiagnostics.add("Intervencion " + intervention.id() + " no esta asignada a una escena.");
            } else if (!scenes.containsKey(sceneId)) {
                interventionDiagnostics.add("Intervencion " + intervention.id() + " referencia escena inexistente " + sceneId + ".");
            }
            if (!characterId.isBlank() && !characters.containsKey(characterId)) {
                interventionDiagnostics.add("Intervencion " + intervention.id() + " referencia personaje inexistente " + characterId + ".");
            }
            diagnostics.addAll(interventionDiagnostics);
            interventions.add(new TheatreProductionIntervention(
                    intervention.id(),
                    intervention.sequenceIndex(),
                    intervention.blockId(),
                    link.fragmentId(),
                    segmentId,
                    sceneId,
                    characterId,
                    characterName(characters, characterId),
                    previewText(fragment, segmentsById.get(segmentId), document, intervention.blockId()),
                    linkedToFragment,
                    interventionAudio,
                    interventionVisual,
                    placement != null,
                    hasPosition(theatre, intervention.id()),
                    hasAction(theatre, intervention.id()),
                    interventionDiagnostics));
        }
        interventions.sort(Comparator.comparingInt(TheatreProductionIntervention::sequenceIndex));

        ArrayList<TheatreProductionScene> sceneViews = new ArrayList<>();
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            List<TheatreProductionIntervention> sceneInterventions = interventions.stream()
                    .filter(intervention -> intervention.sceneId().equals(scene.id()))
                    .toList();
            ArrayList<String> sceneDiagnostics = new ArrayList<>();
            if (!scene.actId().isBlank() && !acts.containsKey(scene.actId())) {
                sceneDiagnostics.add("Escena " + scene.id() + " referencia acto inexistente " + scene.actId() + ".");
            }
            if (!scene.spatialMapAssetId().isBlank() && !assetIds.contains(scene.spatialMapAssetId())) {
                sceneDiagnostics.add("Escena " + scene.id() + " referencia mapa inexistente " + scene.spatialMapAssetId() + ".");
            }
            if (sceneInterventions.isEmpty()) {
                sceneDiagnostics.add("Escena " + scene.id() + " no tiene intervenciones vinculadas.");
            }
            sceneViews.add(new TheatreProductionScene(
                    scene.id(),
                    scene.displayName(),
                    scene.actId(),
                    sceneInterventions.size(),
                    (int) sceneInterventions.stream().filter(TheatreProductionIntervention::audioReady).count(),
                    (int) sceneInterventions.stream().filter(TheatreProductionIntervention::visualReady).count(),
                    !scene.spatialMapAssetId().isBlank() && assetIds.contains(scene.spatialMapAssetId()),
                    sceneInterventions.stream().anyMatch(TheatreProductionIntervention::hasTextPlacement),
                    theatre.positions().stream().anyMatch(position -> position.sceneId().equals(scene.id())),
                    theatre.actions().stream().anyMatch(action -> action.sceneId().equals(scene.id())),
                    sceneDiagnostics));
        }

        int interventionCount = interventions.size();
        int audioMissing = Math.max(0, interventionCount - audioReady);
        int visualMissing = Math.max(0, interventionCount - visualReady);
        int brokenReferences = diagnostics.size();
        TheatreProductionReadiness readiness = readiness(
                theatre,
                script,
                interventionCount,
                linked,
                audioReady,
                audioMissing,
                visualReady,
                visualMissing,
                brokenReferences,
                sceneViews);
        return new TheatreProductionProjection(title, sceneViews, interventions, readiness, diagnostics);
    }

    private static TheatreProductionReadiness readiness(
            TheatreProjectLayer theatre,
            NarrationScriptDocument script,
            int interventionCount,
            int linked,
            int audioReady,
            int audioMissing,
            int visualReady,
            int visualMissing,
            int brokenReferences,
            List<TheatreProductionScene> scenes
    ) {
        ArrayList<String> baseMissing = new ArrayList<>();
        if (script == null || script.empty()) {
            baseMissing.add("Prepara la lectura teatral antes de exportar.");
        }
        if (interventionCount == 0) {
            baseMissing.add("Importa o detecta intervenciones teatrales.");
        }
        if (linked == 0 && interventionCount > 0) {
            baseMissing.add("Vincula las intervenciones a fragmentos del documento.");
        }
        if (audioMissing > 0) {
            baseMissing.add("Falta audio listo en " + audioMissing + " intervencion(es).");
        }
        if (brokenReferences > 0) {
            baseMissing.add("Corrige " + brokenReferences + " referencia(s) teatral(es) invalida(s).");
        }

        ArrayList<String> workMissing = new ArrayList<>(baseMissing);
        if (theatre.scenes().isEmpty()) {
            workMissing.add("Define al menos una escena teatral.");
        }

        ArrayList<String> mapMissing = new ArrayList<>(workMissing);
        if (theatre.textActionPlacements().isEmpty()) {
            mapMissing.add("Configura placements del mapa textual para las intervenciones.");
        }
        if (theatre.positions().isEmpty() && theatre.actions().isEmpty()) {
            mapMissing.add("Configura posiciones o acciones teatrales para el mapa espacial.");
        }

        ArrayList<String> portionMissing = new ArrayList<>(baseMissing);
        if (theatre.acts().isEmpty() && theatre.scenes().isEmpty()) {
            portionMissing.add("Define al menos un acto o una escena exportable.");
        }
        boolean anySceneWithInterventions = scenes.stream().anyMatch(scene -> scene.interventionCount() > 0);
        if (!theatre.scenes().isEmpty() && !anySceneWithInterventions) {
            portionMissing.add("Asigna intervenciones a una escena antes de exportar una porcion.");
        }

        ArrayList<String> warnings = new ArrayList<>();
        if (visualMissing > 0) {
            warnings.add("Faltan visuales teatrales en " + visualMissing + " intervencion(es); no se generan automaticamente al exportar.");
        }
        if (theatre.characters().isEmpty()) {
            warnings.add("No hay personajes declarados en la capa teatral.");
        }
        if (theatre.objects().isEmpty()) {
            warnings.add("No hay objetos teatrales declarados.");
        }

        return new TheatreProductionReadiness(
                theatre.acts().size(),
                theatre.scenes().size(),
                interventionCount,
                linked,
                Math.max(0, interventionCount - linked),
                audioReady,
                audioMissing,
                visualReady,
                visualMissing,
                theatre.characters().size(),
                theatre.objects().size(),
                theatre.textActionPlacements().size(),
                theatre.positions().size(),
                theatre.actions().size(),
                brokenReferences,
                workMissing,
                mapMissing,
                portionMissing,
                warnings);
    }

    private static List<String> referenceDiagnostics(
            TheatreProjectLayer theatre,
            Set<String> assetIds,
            Map<String, TheatreProjectLayer.CharacterProfile> characters,
            Map<String, TheatreProjectLayer.TheatreObject> objects,
            Map<String, TheatreProjectLayer.TheatreAct> acts,
            Map<String, TheatreProjectLayer.Scene> scenes
    ) {
        ArrayList<String> diagnostics = new ArrayList<>();
        Set<String> interventions = new LinkedHashSet<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            interventions.add(intervention.id());
        }
        for (TheatreProjectLayer.IntervencionVisual visual : theatre.intervencionesVisuales()) {
            if (!interventions.contains(visual.intervencionId())) {
                diagnostics.add("Visual teatral " + visual.assetId() + " referencia intervencion inexistente " + visual.intervencionId() + ".");
            }
            if (!assetIds.contains(visual.assetId())) {
                diagnostics.add("Visual teatral " + visual.intervencionId() + " referencia asset inexistente " + visual.assetId() + ".");
            }
        }
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            if (!characters.containsKey(image.characterId())) {
                diagnostics.add("Imagen de personaje referencia personaje inexistente " + image.characterId() + ".");
            }
            if (!image.sceneId().isBlank() && !scenes.containsKey(image.sceneId())) {
                diagnostics.add("Imagen de personaje " + image.id() + " referencia escena inexistente " + image.sceneId() + ".");
            }
            if (!assetIds.contains(image.assetId())) {
                diagnostics.add("Imagen de personaje referencia asset inexistente " + image.assetId() + ".");
            }
        }
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            if (!objects.containsKey(image.objectId())) {
                diagnostics.add("Imagen de objeto referencia objeto inexistente " + image.objectId() + ".");
            }
            if (!image.sceneId().isBlank() && !scenes.containsKey(image.sceneId())) {
                diagnostics.add("Imagen de objeto " + image.id() + " referencia escena inexistente " + image.sceneId() + ".");
            }
            if (!assetIds.contains(image.assetId())) {
                diagnostics.add("Imagen de objeto referencia asset inexistente " + image.assetId() + ".");
            }
        }
        for (TheatreProjectLayer.VoiceRoleAlias alias : theatre.voiceRoleAliases()) {
            if (!alias.characterId().isBlank() && !characters.containsKey(alias.characterId())) {
                diagnostics.add("Alias de voz " + alias.id() + " referencia personaje inexistente " + alias.characterId() + ".");
            }
        }
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            if (!scene.actId().isBlank() && !acts.containsKey(scene.actId())) {
                diagnostics.add("Escena " + scene.id() + " referencia acto inexistente " + scene.actId() + ".");
            }
            if (!scene.spatialMapAssetId().isBlank() && !assetIds.contains(scene.spatialMapAssetId())) {
                diagnostics.add("Escena " + scene.id() + " referencia mapa inexistente " + scene.spatialMapAssetId() + ".");
            }
        }
        for (TheatreProjectLayer.TextActionPlacement placement : theatre.textActionPlacements()) {
            if (!interventions.contains(placement.intervencionId())) {
                diagnostics.add("Placement teatral referencia intervencion inexistente " + placement.intervencionId() + ".");
            }
            if (!scenes.containsKey(placement.sceneId())) {
                diagnostics.add("Placement teatral " + placement.intervencionId() + " referencia escena inexistente " + placement.sceneId() + ".");
            }
            if (!placement.characterId().isBlank() && !characters.containsKey(placement.characterId())) {
                diagnostics.add("Placement teatral " + placement.intervencionId() + " referencia personaje inexistente " + placement.characterId() + ".");
            }
        }
        for (TheatreProjectLayer.SpatialPosition position : theatre.positions()) {
            if (!scenes.containsKey(position.sceneId())) {
                diagnostics.add("Posicion teatral " + position.alias() + " referencia escena inexistente " + position.sceneId() + ".");
            }
            if (!position.characterId().isBlank() && !characters.containsKey(position.characterId())) {
                diagnostics.add("Posicion teatral " + position.alias() + " referencia personaje inexistente " + position.characterId() + ".");
            }
        }
        for (TheatreProjectLayer.TheatreAction action : theatre.actions()) {
            if (!scenes.containsKey(action.sceneId())) {
                diagnostics.add("Accion teatral referencia escena inexistente " + action.sceneId() + ".");
            }
            if (!action.characterId().isBlank() && !characters.containsKey(action.characterId())) {
                diagnostics.add("Accion teatral referencia personaje inexistente " + action.characterId() + ".");
            }
        }
        return List.copyOf(diagnostics);
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

    private static Map<String, NarrationSegment> segmentsByBlock(NarrationScriptDocument script) {
        LinkedHashMap<String, NarrationSegment> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            for (String blockId : segment.sourceBlockIds()) {
                if (blockId != null && !blockId.isBlank()) {
                    result.putIfAbsent(blockId, segment);
                }
            }
        }
        return result;
    }

    private static NarrationSegment emptySegment() {
        return NarrationSegment.of("_", com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType.PARAGRAPH, "", "", List.of());
    }

    private static Set<String> completedAudioSegments(List<AudioJobSnapshot> audioJobs, PlaybackManifest manifest) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (audioJobs != null) {
            for (AudioJobSnapshot job : audioJobs) {
                if (job == null) {
                    continue;
                }
                for (AudioSegmentSnapshot segment : job.segments()) {
                    if (segment.completed() && !segment.audioRelativePath().isBlank()) {
                        result.add(segment.segmentId());
                    }
                }
            }
        }
        if (manifest != null) {
            for (var cue : manifest.cues()) {
                if (cue.hasAudio()) {
                    result.add(cue.segmentId());
                }
            }
        }
        return result;
    }

    private static Set<String> assetIds(DocuPodcastProject project) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (project == null || project.assets() == null) {
            return result;
        }
        for (ProjectAssetReference reference : project.assets().references()) {
            result.add(reference.id());
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.CharacterProfile> charactersById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.CharacterProfile> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.CharacterProfile character : theatre.characters()) {
            result.put(character.id(), character);
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.TheatreObject> objectsById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.TheatreObject> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.TheatreObject object : theatre.objects()) {
            result.put(object.id(), object);
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.TheatreAct> actsById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.TheatreAct> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.TheatreAct act : theatre.acts()) {
            result.put(act.id(), act);
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.Scene> scenesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.Scene> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            result.put(scene.id(), scene);
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.TextActionPlacement> placementsByIntervention(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.TextActionPlacement> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.TextActionPlacement placement : theatre.textActionPlacements()) {
            result.putIfAbsent(placement.intervencionId(), placement);
        }
        return result;
    }

    private static Set<String> visualReadyInterventions(TheatreProjectLayer theatre, Set<String> assetIds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (TheatreProjectLayer.IntervencionVisual visual : theatre.intervencionesVisuales()) {
            if (assetIds.contains(visual.assetId())) {
                result.add(visual.intervencionId());
            }
        }
        return result;
    }

    private static Set<String> fragmentAudioReady(FragmentWorkspaceProjection projection) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (projection == null) {
            return result;
        }
        for (FragmentAssetBinding binding : projection.bindings()) {
            boolean audioRole = binding.role() == FragmentAssetRole.AUDIO_TTS
                    || binding.role() == FragmentAssetRole.AUDIO_RECORDED
                    || binding.role() == FragmentAssetRole.AUDIO_IMPORTED
                    || binding.role() == FragmentAssetRole.PLAYBACK_CUE;
            if (audioRole && !binding.assetPath().isBlank()
                    && ("COMPLETED".equals(binding.status()) || "READY".equals(binding.status()))) {
                result.add(binding.fragmentId().value());
            }
        }
        return result;
    }

    private static Set<String> fragmentVisualReady(FragmentWorkspaceProjection projection) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (projection == null) {
            return result;
        }
        for (FragmentAssetBinding binding : projection.bindings()) {
            if (binding.role() == FragmentAssetRole.THEATRE_VISUAL && !binding.assetPath().isBlank()) {
                result.add(binding.fragmentId().value());
            }
        }
        return result;
    }

    private static boolean audioFor(
            FragmentId fragmentId,
            String segmentId,
            Set<String> fragmentAudioReady,
            Set<String> completedAudioSegments
    ) {
        if (fragmentId != null && fragmentAudioReady.contains(fragmentId.value())) {
            return true;
        }
        if (segmentId == null || segmentId.isBlank()) {
            return false;
        }
        if (completedAudioSegments.contains(segmentId)) {
            return true;
        }
        String unitPrefix = segmentId + "-";
        return completedAudioSegments.stream().anyMatch(id -> id.startsWith(unitPrefix));
    }

    private static boolean hasPosition(TheatreProjectLayer theatre, String interventionId) {
        return theatre.positions().stream().anyMatch(position -> position.alias().equals(interventionId));
    }

    private static boolean hasAction(TheatreProjectLayer theatre, String interventionId) {
        return theatre.actions().stream()
                .anyMatch(action -> action.fromAlias().equals(interventionId) || action.toAlias().equals(interventionId));
    }

    private static String characterName(Map<String, TheatreProjectLayer.CharacterProfile> characters, String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return "";
        }
        TheatreProjectLayer.CharacterProfile character = characters.get(characterId);
        return character == null ? "" : character.displayName();
    }

    private static String previewText(
            Optional<DocumentFragment> fragment,
            NarrationSegment segment,
            ReadableDocument document,
            String blockId
    ) {
        String text = fragment.map(DocumentFragment::text).orElse("");
        if (text.isBlank() && segment != null) {
            text = segment.narrationText();
        }
        if (text.isBlank() && document != null) {
            text = document.blockById(blockId).map(block -> block.text()).orElse("");
        }
        text = text.strip();
        return text.length() <= 160 ? text : text.substring(0, 160).strip() + "...";
    }
}
