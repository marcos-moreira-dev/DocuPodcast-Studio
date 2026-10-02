package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.*;

/** Canonical human-readable serialization of the operational theatre layer. */
public final class TheatreGrammarV2Writer {
    public String write(DocuPodcastProject project, Map<String, String> packagedAssetPaths) {
        return write(project, packagedAssetPaths, null);
    }

    public String write(DocuPodcastProject project, Map<String, String> packagedAssetPaths,
                        NarrationScriptDocument script) {
        TheatreProjectLayer layer = project.theatre();
        StringBuilder out = new StringBuilder("# ").append(project.metadata().title()).append("\n\n")
                .append("> DocuPodcast Teatro Grammar v2\n")
                .append("> grammarVersion: theatre-v2\n\n## Personajes\n");
        for (var character : layer.characters()) {
            String voice = layer.voiceRoleAliases().stream().filter(v -> v.characterId().equals(character.id()))
                    .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId).findFirst().orElse("");
            out.append("- personaje: ").append(character.displayName()).append(" | id=").append(character.id());
            if (!character.aliases().isEmpty()) out.append(" | aliases=").append(String.join(",", character.aliases()));
            if (!voice.isBlank()) out.append(" | voz=").append(voice);
            if (!character.notes().isBlank()) out.append(" | nota=").append(oneLine(character.notes()));
            out.append('\n');
            layer.characterImages().stream().filter(v -> v.characterId().equals(character.id())).forEach(image -> {
                String path = packagedAssetPaths.get(image.assetId());
                if (path != null) out.append("- ").append(path).append(" | escena=").append(image.sceneId())
                        .append(" | angulo=").append(image.view()).append('\n');
            });
        }
        out.append("\n## Objetos\n");
        for (var object : layer.objects()) {
            out.append("- objeto: ").append(object.displayName()).append(" | id=").append(object.id());
            if (!object.notes().isBlank()) out.append(" | nota=").append(oneLine(object.notes()));
            out.append('\n');
            layer.objectImages().stream().filter(v -> v.objectId().equals(object.id())).forEach(image -> {
                String path = packagedAssetPaths.get(image.assetId());
                if (path != null) out.append("- ").append(path).append(" | escena=").append(image.sceneId())
                        .append(" | angulo=").append(image.view()).append('\n');
            });
        }
        List<TheatreProjectLayer.TheatreAct> acts = layer.acts().isEmpty()
                ? List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")) : layer.acts();
        for (var act : acts) {
            out.append("\n## Acto: ").append(act.displayName()).append(" | id=").append(act.id()).append('\n');
            if (!act.notes().isBlank()) out.append("notas: ").append(oneLine(act.notes())).append('\n');
            List<TheatreProjectLayer.Scene> scenes = layer.scenes().stream().filter(s -> s.actId().equals(act.id())
                    || (s.actId().isBlank() && act.equals(acts.get(0)))).toList();
            if (scenes.isEmpty()) scenes = List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", act.id()));
            for (var scene : scenes) writeScene(out, layer, scene, packagedAssetPaths, script);
        }
        return out.toString();
    }

    private static void writeScene(StringBuilder out, TheatreProjectLayer layer, TheatreProjectLayer.Scene scene,
                                   Map<String, String> paths, NarrationScriptDocument script) {
        out.append("\n### Escena: ").append(scene.displayName()).append(" | id=").append(scene.id()).append('\n');
        ArrayList<String> sceneMetadata = new ArrayList<>();
        if (!scene.spatialMapAssetId().isBlank() && paths.containsKey(scene.spatialMapAssetId()))
            sceneMetadata.add("mapa_espacial=" + paths.get(scene.spatialMapAssetId()));
        layer.stageBackdropAssignments().stream().filter(a -> a.scope().equals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE)
                && a.scopeId().equals(scene.id())).findFirst().ifPresent(a -> layer.stageBackdrops().stream()
                .filter(b -> b.id().equals(a.backdropId())).findFirst().ifPresent(b -> {
                    if (paths.containsKey(b.assetId())) sceneMetadata.add("fondo_escenario=" + paths.get(b.assetId()));
                }));
        if (!sceneMetadata.isEmpty()) out.append("> ").append(String.join(" | ", sceneMetadata)).append('\n');
        if (!scene.notes().isBlank()) out.append("notas: ").append(oneLine(scene.notes())).append('\n');
        List<TheatreProjectLayer.TextActionPlacement> placements = layer.textActionPlacements().stream()
                .filter(p -> p.sceneId().equals(scene.id())).toList();
        for (var intervention : layer.intervenciones().stream().sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex)).toList()) {
            var placement = placements.stream().filter(p -> p.intervencionId().equals(intervention.id())).findFirst().orElse(null);
            if (placement == null) continue;
            String speaker = layer.characters().stream().filter(c -> c.id().equals(placement.characterId()))
                    .map(TheatreProjectLayer.CharacterProfile::displayName).findFirst().orElse("NARRADOR");
            String text = narrationText(script, intervention.blockId());
            out.append(speaker).append(": ").append(text.isBlank() ? "Intervención " + intervention.sequenceIndex() + "." : text.replace('\n', ' ').replace('\r', ' ').strip()).append("\n");
            ArrayList<String> meta = new ArrayList<>(); meta.add("id=" + intervention.id());
            if (script != null) script.segments().stream()
                    .filter(s -> s.sourceBlockIds().contains(intervention.blockId()) || s.id().equals(intervention.blockId()))
                    .findFirst().ifPresent(s -> {
                        if ("false".equals(s.metadata().get("theatreApplyCamera"))) meta.add("aplicar_plano=false");
                        String context = s.metadata().getOrDefault("theatreAiContext", "");
                        if (!context.isBlank()) meta.add("contexto_ia=" + oneLine(context));
                    });
            layer.stageBackdropAssignments().stream()
                    .filter(a -> a.scope().equals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION) && a.scopeId().equals(intervention.id()))
                    .findFirst().ifPresent(a -> {
                        if (a.backdropId().equals(TheatreProjectLayer.STAGE_BACKDROP_NONE)) meta.add("quitar_fondo=true");
                        else layer.stageBackdrops().stream().filter(b -> b.id().equals(a.backdropId())).findFirst()
                                .ifPresent(b -> { if (paths.containsKey(b.assetId())) meta.add("fondo=" + paths.get(b.assetId())); });
                    });
            if (!placement.origin().isBlank()) meta.add("origen=" + placement.origin());
            if (!placement.destination().isBlank()) meta.add("destino=" + placement.destination());
            if (!placement.interactionTarget().isBlank()) meta.add("interaccion=" + placement.interactionTarget());
            layer.cameraCues().stream().filter(c -> c.intervencionId().equals(intervention.id())).findFirst()
                    .ifPresent(c -> meta.add("plano=" + c.cameraId()));
            layer.choralVoiceAssignments().stream().filter(c -> c.intervencionId().equals(intervention.id())).findFirst()
                    .ifPresent(c -> meta.add("voces=" + String.join(",", c.participantCharacterIds())));
            layer.intervencionesVisuales().stream().filter(v -> v.intervencionId().equals(intervention.id())).findFirst()
                    .ifPresent(v -> { if (paths.containsKey(v.assetId())) meta.add("imagen=" + paths.get(v.assetId())); });
            layer.interventionStates().stream().filter(s -> s.interventionId().equals(intervention.id())).findFirst()
                    .ifPresent(s -> appendState(meta, s));
            out.append("> ").append(String.join(" | ", meta)).append('\n');
        }
    }

    private static void appendState(List<String> meta, TheatreInterventionState state) {
        meta.add("hereda=" + switch (state.inheritanceMode()) { case PREVIOUS -> "anterior"; case RESET -> "ninguna"; case EXPLICIT -> state.inheritsFromInterventionId(); });
        var present = state.characters().stream().filter(c -> c.presence() == TheatreInterventionState.Presence.PRESENT)
                .map(c -> c.characterId() + (c.position().isBlank() ? "" : "@" + c.position())).toList();
        var absent = state.characters().stream().filter(c -> c.presence() == TheatreInterventionState.Presence.ABSENT)
                .map(TheatreInterventionState.CharacterState::characterId).toList();
        if (!present.isEmpty()) meta.add("presentes=" + String.join(",", present));
        if (!absent.isEmpty()) meta.add("ausentes=" + String.join(",", absent));
        addCharacterProperty(meta, "orientaciones", state, TheatreInterventionState.CharacterState::orientation);
        addCharacterProperty(meta, "miradas", state, TheatreInterventionState.CharacterState::gazeTarget);
        addCharacterProperty(meta, "variantes", state, TheatreInterventionState.CharacterState::visualVariantId);
        addCharacterProperty(meta, "vestuarios", state, TheatreInterventionState.CharacterState::costume);
        var objects = state.objects().stream().map(o -> o.objectId() + "@" + (o.presence() == TheatreInterventionState.Presence.ABSENT
                ? "ausente" : !o.holderCharacterId().isBlank() ? "portado:" + o.holderCharacterId() : o.position())).toList();
        if (!objects.isEmpty()) meta.add("objetos=" + String.join(",", objects));
        var events = state.events().stream().map(TheatreGrammarV2Writer::event).toList();
        if (!events.isEmpty()) meta.add("eventos=" + String.join(";", events));
        if (!state.microexpression().isBlank()) meta.add("microexpresion=" + state.microexpression());
        if (!state.emoji().isBlank()) meta.add("emoji=" + state.emoji());
        if (!state.tone().isBlank()) meta.add("tono=" + state.tone());
    }
    private static void addCharacterProperty(List<String> meta, String key, TheatreInterventionState state,
                                             java.util.function.Function<TheatreInterventionState.CharacterState,String> getter) {
        var values = state.characters().stream().filter(c -> !getter.apply(c).isBlank())
                .map(c -> c.characterId() + "@" + getter.apply(c)).toList(); if (!values.isEmpty()) meta.add(key + "=" + String.join(",", values));
    }
    private static String event(TheatreInterventionState.StageEvent e) {
        String value = e.type() + (e.characterId().isBlank() ? "" : ":" + e.characterId())
                + (e.objectId().isBlank() ? "" : ":" + e.objectId()) + (e.targetCharacterId().isBlank() ? "" : ":" + e.targetCharacterId());
        return value + (e.toPosition().isBlank() ? "" : "@" + e.toPosition());
    }
    private static String oneLine(String value) { return value.replace('\n', ' ').replace('|', '/').strip(); }
    private static String narrationText(NarrationScriptDocument script, String blockId) {
        if (script == null || blockId == null || blockId.isBlank()) return "";
        return script.segments().stream().filter(segment -> segment.id().equals(blockId)
                        || segment.sourceBlockIds().contains(blockId))
                .map(segment -> segment.narrationText()).findFirst().orElse("");
    }
}
