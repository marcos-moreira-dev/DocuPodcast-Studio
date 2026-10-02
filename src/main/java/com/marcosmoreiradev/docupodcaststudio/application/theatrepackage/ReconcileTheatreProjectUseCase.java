package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreRefreshPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Applies only materialized package bindings while preserving unrelated theatre work. */
public final class ReconcileTheatreProjectUseCase {
    public DocuPodcastProject reconcile(DocuPodcastProject project, List<StagedTheatreAsset> stagedAssets) {
        return reconcile(project, stagedAssets, null);
    }

    public DocuPodcastProject reconcile(DocuPodcastProject project, List<StagedTheatreAsset> stagedAssets,
                                        TheatreRefreshPlan plan) {
        return reconcile(project, stagedAssets, plan, null);
    }

    public DocuPodcastProject reconcile(DocuPodcastProject project, List<StagedTheatreAsset> stagedAssets,
                                        TheatreRefreshPlan plan,
                                        com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script) {
        Objects.requireNonNull(project, "project");
        DocuPodcastProject updated = project;
        TheatreProjectLayer theatre = project.theatre();
        ArrayList<NarrativeLayerAssignment> layers = new ArrayList<>(project.narrativeLayerAssignments());

        if (plan != null) {
            for (var delta : plan.deltas()) {
                if (delta.status() != TheatrePackageDeltaStatus.RENAMED || delta.before() == null) continue;
                TheatrePackageEntry old = delta.before();
                theatre = removeBinding(theatre, old);
                layers.removeIf(layer -> layer.id().equals(humanAudioLayerId(old.logicalId())));
                updated = updated.withoutAsset(projectAssetId(old.logicalId()));
            }
        }

        for (StagedTheatreAsset staged : stagedAssets == null ? List.<StagedTheatreAsset>of() : stagedAssets) {
            updated = updated.withAsset(staged.reference());
            TheatrePackageEntry entry = staged.entry();
            String assetId = staged.reference().id();
            switch (entry.kind()) {
                case CHARACTER_IMAGE -> theatre = upsertCharacterImage(theatre, entry, assetId);
                case OBJECT_IMAGE -> theatre = upsertObjectImage(theatre, entry, assetId);
                case BACKDROP -> theatre = upsertBackdrop(theatre, entry, assetId);
                case SPATIAL_MAP -> theatre = assignSpatialMap(theatre, entry, assetId);
                case INTERVENTION_IMAGE -> theatre = upsertInterventionVisual(theatre, entry, assetId);
                case INTERMEDIATE_FRAME -> theatre = upsertIntermediateFrame(theatre, entry, assetId);
                case HUMAN_AUDIO -> {
                    if (entry.metadata("trackId").isBlank()) layers = upsertHumanAudio(theatre, layers, entry, assetId, script);
                    else theatre = upsertAudioTrack(theatre, entry, assetId, script);
                }
                case VOICE_SAMPLE -> {
                    String voiceId = entry.metadata("voiceProfileId");
                    if (voiceId.isBlank() || updated.voiceLibrary().voiceById(voiceId).isEmpty())
                        throw new IllegalArgumentException("La muestra requiere un voiceProfileId existente: " + entry.logicalId());
                    var sample = new com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample(
                            fallback(entry.metadata("sampleId"), "SAMPLE-" + safe(entry.logicalId())), voiceId,
                            com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone.valueOf(fallback(entry.metadata("tone"), "NEUTRAL")),
                            staged.reference().relativePath(), com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin.IMPORTED_FILE,
                            com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership.PROJECT_ASSET,
                            (long) number(entry, "durationMillis", 0), java.time.Instant.EPOCH,
                            entry.metadata("notes"), entry.metadata("referenceTranscript"));
                    updated = updated.withVoiceLibrary(updated.voiceLibrary().withReferenceSample(sample));
                }
                case VIDEO, OTHER -> { /* Catalog-only: no implicit playback assignment. */ }
            }
        }
        return updated.withTheatre(theatre).withNarrativeLayerAssignments(layers);
    }

    private static TheatreProjectLayer removeBinding(TheatreProjectLayer theatre, TheatrePackageEntry entry) {
        return switch (entry.kind()) {
            case CHARACTER_IMAGE -> theatre.withCharacterImages(theatre.characterImages().stream()
                    .filter(item -> !item.id().equals(entry.logicalId())).toList());
            case OBJECT_IMAGE -> theatre.withObjectImages(theatre.objectImages().stream()
                    .filter(item -> !item.id().equals(entry.logicalId())).toList());
            case BACKDROP -> {
                String id = entry.metadata("backdropId");
                TheatreProjectLayer withoutBackdrop = theatre.withStageBackdrops(theatre.stageBackdrops().stream()
                        .filter(item -> !item.id().equals(id)).toList());
                yield withoutBackdrop.withStageBackdropAssignments(withoutBackdrop.stageBackdropAssignments().stream()
                        .filter(item -> !item.backdropId().equals(id)).toList());
            }
            case SPATIAL_MAP -> {
                String sceneId = entry.metadata("sceneId");
                yield theatre.withScenes(theatre.scenes().stream().map(scene -> scene.id().equals(sceneId)
                        ? new TheatreProjectLayer.Scene(scene.id(), scene.displayName(), scene.notes(), scene.actId(), "")
                        : scene).toList());
            }
            case INTERVENTION_IMAGE -> theatre.withIntervencionesVisuales(theatre.intervencionesVisuales().stream()
                    .filter(item -> !item.intervencionId().equals(entry.metadata("interventionId"))).toList());
            case INTERMEDIATE_FRAME -> theatre.withIntermediateFrames(theatre.intermediateFrames().stream()
                    .filter(item -> !(item.fromIntervencionId().equals(entry.metadata("fromInterventionId"))
                            && item.toIntervencionId().equals(entry.metadata("toInterventionId")))).toList());
            case HUMAN_AUDIO, VOICE_SAMPLE, VIDEO, OTHER -> theatre;
        };
    }

    private static TheatreProjectLayer upsertCharacterImage(TheatreProjectLayer theatre, TheatrePackageEntry entry,
                                                            String assetId) {
        ArrayList<TheatreProjectLayer.CharacterImage> images = new ArrayList<>(theatre.characterImages());
        images.removeIf(item -> item.id().equals(entry.logicalId()));
        images.add(new TheatreProjectLayer.CharacterImage(entry.logicalId(), entry.metadata("characterId"),
                entry.metadata("sceneId"), fallback(entry.metadata("view"), "frontal"), assetId,
                "Sincronizada desde paquete teatral."));
        return theatre.withCharacterImages(images);
    }

    private static TheatreProjectLayer upsertObjectImage(TheatreProjectLayer theatre, TheatrePackageEntry entry,
                                                         String assetId) {
        ArrayList<TheatreProjectLayer.ObjectImage> images = new ArrayList<>(theatre.objectImages());
        images.removeIf(item -> item.id().equals(entry.logicalId()));
        images.add(new TheatreProjectLayer.ObjectImage(entry.logicalId(), entry.metadata("objectId"),
                entry.metadata("sceneId"), fallback(entry.metadata("view"), "frontal"), assetId,
                "Sincronizada desde paquete teatral."));
        return theatre.withObjectImages(images);
    }

    private static TheatreProjectLayer upsertBackdrop(TheatreProjectLayer theatre, TheatrePackageEntry entry,
                                                      String assetId) {
        String backdropId = entry.metadata("backdropId");
        ArrayList<TheatreProjectLayer.StageBackdrop> backdrops = new ArrayList<>(theatre.stageBackdrops());
        backdrops.removeIf(item -> item.id().equals(backdropId));
        backdrops.add(new TheatreProjectLayer.StageBackdrop(backdropId,
                fallback(entry.metadata("displayName"), backdropId), assetId, "Sincronizado desde paquete teatral."));
        TheatreProjectLayer result = theatre.withStageBackdrops(backdrops);
        String scope = entry.metadata("scope");
        String scopeId = entry.metadata("scopeId");
        if (!scope.isBlank() && !scopeId.isBlank()) {
            ArrayList<TheatreProjectLayer.StageBackdropAssignment> assignments =
                    new ArrayList<>(result.stageBackdropAssignments());
            assignments.removeIf(item -> item.scope().equalsIgnoreCase(scope) && item.scopeId().equals(scopeId));
            assignments.add(new TheatreProjectLayer.StageBackdropAssignment(scope, scopeId, backdropId,
                    "Sincronizado desde paquete teatral."));
            result = result.withStageBackdropAssignments(assignments);
        }
        return result;
    }

    private static TheatreProjectLayer assignSpatialMap(TheatreProjectLayer theatre, TheatrePackageEntry entry,
                                                        String assetId) {
        String sceneId = entry.metadata("sceneId");
        List<TheatreProjectLayer.Scene> scenes = theatre.scenes().stream().map(scene -> scene.id().equals(sceneId)
                ? new TheatreProjectLayer.Scene(scene.id(), scene.displayName(), scene.notes(), scene.actId(), assetId)
                : scene).toList();
        return theatre.withScenes(scenes);
    }

    private static TheatreProjectLayer upsertInterventionVisual(TheatreProjectLayer theatre,
                                                                TheatrePackageEntry entry, String assetId) {
        String interventionId = entry.metadata("interventionId");
        ArrayList<TheatreProjectLayer.IntervencionVisual> visuals = new ArrayList<>(theatre.intervencionesVisuales());
        visuals.removeIf(item -> item.intervencionId().equals(interventionId));
        visuals.add(new TheatreProjectLayer.IntervencionVisual(interventionId, assetId,
                "Visual explícito sincronizado desde paquete teatral."));
        return theatre.withIntervencionesVisuales(visuals);
    }

    private static TheatreProjectLayer upsertIntermediateFrame(TheatreProjectLayer theatre,
                                                               TheatrePackageEntry entry, String assetId) {
        String from = entry.metadata("fromInterventionId");
        String to = entry.metadata("toInterventionId");
        ArrayList<TheatreProjectLayer.IntermediateFrame> frames = new ArrayList<>(theatre.intermediateFrames());
        frames.removeIf(item -> item.fromIntervencionId().equals(from) && item.toIntervencionId().equals(to));
        frames.add(new TheatreProjectLayer.IntermediateFrame(from, to, assetId,
                "Frame puente sincronizado desde paquete teatral."));
        return theatre.withIntermediateFrames(frames);
    }

    private static ArrayList<NarrativeLayerAssignment> upsertHumanAudio(TheatreProjectLayer theatre,
                                                                        ArrayList<NarrativeLayerAssignment> layers,
                                                                        TheatrePackageEntry entry, String assetId,
                                                                        com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script) {
        String interventionId = entry.metadata("interventionId");
        TheatreProjectLayer.Intervencion intervention = theatre.intervenciones().stream()
                .filter(item -> item.id().equals(interventionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Intervención no resuelta: " + interventionId));
        String layerId = humanAudioLayerId(entry.logicalId());
        String segmentId = script == null ? intervention.blockId() : script.segments().stream()
                .filter(s -> s.sourceBlockIds().contains(intervention.blockId()) || s.id().equals(intervention.blockId()))
                .map(s -> s.id()).findFirst().orElseThrow(() -> new IllegalArgumentException("Parlamento no resuelto: " + interventionId));
        int textLength = script == null ? Integer.MAX_VALUE : script.segmentById(segmentId).orElseThrow().narrationText().length();
        int start = Integer.parseInt(fallback(entry.metadata("textStart"), "0"));
        int end = Integer.parseInt(fallback(entry.metadata("textEnd"), Integer.toString(textLength)));
        if (start < 0 || end > textLength || end <= start) throw new IllegalArgumentException("Rango de audio inválido: " + entry.logicalId());
        layers.removeIf(layer -> layer.id().equals(layerId)
                || (layer.primaryNarrationLayer() && layer.textRange().segmentId().equals(segmentId)
                && layer.textRange().startOffset() < end && layer.textRange().endOffset() > start));
        layers.add(new NarrativeLayerAssignment(layerId, NarrativeLayerKind.HUMAN_AUDIO,
                new ScriptTextRange(segmentId, start, end), assetId,
                fallback(entry.metadata("displayName"), "Audio humano de " + interventionId),
                "Sincronizado desde paquete teatral."));
        return layers;
    }

    private static String fallback(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static TheatreProjectLayer upsertAudioTrack(TheatreProjectLayer theatre, TheatrePackageEntry entry, String assetId,
            com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script) {
        String interventionId = entry.metadata("interventionId");
        var intervention = theatre.intervenciones().stream().filter(i -> i.id().equals(interventionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ancla de pista inexistente: " + interventionId));
        String segmentId = script == null ? "" : script.segments().stream().filter(s -> s.sourceBlockIds().contains(intervention.blockId()))
                .map(s -> s.id()).findFirst().orElseThrow();
        var track = new TheatreProjectLayer.TheatreAudioTrack(entry.metadata("trackId"), assetId, interventionId, segmentId,
                number(entry,"sourceStartSeconds",0), number(entry,"sourceEndSeconds",0),
                TheatreProjectLayer.AudioTrackEndMode.valueOf(fallback(entry.metadata("endMode"), "FILE_END")),
                number(entry,"volume",0.3), number(entry,"sourceDurationSeconds",0), Boolean.parseBoolean(entry.metadata("gentleFade")));
        var tracks = new ArrayList<>(theatre.audioTracks());
        tracks.removeIf(t -> t.id().equals(track.id())); tracks.add(track);
        return theatre.withAudioTracks(tracks);
    }
    private static double number(TheatrePackageEntry entry, String key, double defaultValue) {
        String value = entry.metadata(key);
        return value.isBlank() ? defaultValue : Double.parseDouble(value);
    }
    private static String safe(String value) { return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9._-]", "-"); }
    private static String humanAudioLayerId(String logicalId) { return "NLA-THEATRE-PACKAGE-" + safe(logicalId); }
    private static String projectAssetId(String logicalId) { return "THEATRE-" + safe(logicalId); }
}
