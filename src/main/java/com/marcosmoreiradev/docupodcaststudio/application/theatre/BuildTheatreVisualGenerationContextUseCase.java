package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds the canonical theatre context consumed by every visual-generation surface. */
public final class BuildTheatreVisualGenerationContextUseCase {
    public TheatreVisualGenerationContext execute(DocuPodcastProject project,
                                                   StoryboardDocument storyboard,
                                                   Path projectRoot,
                                                   TheatreImageGenerationUnit unit) {
        return execute(project, storyboard, null, projectRoot, unit);
    }

    public TheatreVisualGenerationContext execute(DocuPodcastProject project,
                                                   StoryboardDocument storyboard,
                                                   NarrationScriptDocument script,
                                                   Path projectRoot,
                                                   TheatreImageGenerationUnit unit) {
        if (project == null || unit == null) {
            return new TheatreVisualGenerationContext(unit, List.of(), List.of(), List.of(), null, null, null);
        }
        Path root = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
        ProjectAssetCatalog assets = project.assets();
        TheatreProjectLayer theatre = project.theatre();
        LinkedHashMap<String, TheatreImageContextAsset> identities = new LinkedHashMap<>();
        LinkedHashMap<String, TheatreImageContextAsset> objects = new LinkedHashMap<>();
        LinkedHashMap<String, TheatreImageContextAsset> environments = new LinkedHashMap<>();

        TheatreProjectLayer.TextActionPlacement placement = theatre.textActionPlacements().stream()
                .filter(candidate -> candidate.sceneId().equals(unit.sceneId())
                        && candidate.intervencionId().equals(unit.interventionId()))
                .findFirst().orElse(null);
        LinkedHashMap<String, String> participants = new LinkedHashMap<>();
        addParticipant(participants, theatre, unit.speaker());
        if (placement != null) {
            addParticipant(participants, theatre, placement.characterId());
            if (!specialTarget(placement.interactionTarget())) {
                addParticipant(participants, theatre, placement.interactionTarget());
            }
            placement.characterLocations().keySet()
                    .forEach(value -> addParticipant(participants, theatre, value));
        }
        ArrayList<TheatreCharacterGenerationContext> characterContexts = new ArrayList<>();
        for (Map.Entry<String, String> participant : participants.entrySet()) {
            String id = participant.getKey();
            String label = participant.getValue();
            List<TheatreImageContextAsset> references =
                    characterReferences(assets, theatre, id, label, unit.sceneId(), root);
            references.forEach(reference -> identities.putIfAbsent(
                    "personaje:" + id + ':' + reference.assetId(), reference));
            String notes = theatre.characters().stream().filter(character -> character.id().equals(id))
                    .map(TheatreProjectLayer.CharacterProfile::notes).findFirst().orElse("");
            String location = placement == null ? "" : locationFor(placement, id, label, theatre);
            boolean speaker = normalizeCharacterId(theatre, unit.speaker()).equals(id);
            characterContexts.add(new TheatreCharacterGenerationContext(
                    id, label, notes, location, speaker, references));
        }

        addObjects(objects, assets, theatre, unit.sceneId(), root);
        boolean assignedBackdrop = new TheatreStageBackdropResolver()
                .resolve(project, unit.sceneId(), unit.interventionId(), root)
                .map(backdrop -> {
                    addAsset(environments, backdrop.asset(), root, "entorno",
                            "Fondo de escenario - " + backdrop.backdrop().displayName());
                    return true;
                })
                .orElse(false);
        if (!assignedBackdrop) {
            assets.references().stream()
                    .filter(ProjectAssetReference::isImage)
                    .filter(asset -> normalize(asset.relativePath()).contains("teatro-vacio")
                            || normalize(asset.relativePath()).contains("teatro vacio")
                            || normalize(asset.displayName()).contains("teatro vacio"))
                    .findFirst()
                    .ifPresent(asset -> addAsset(environments, asset, root, "entorno", "Teatro vacío"));
        }

        ActiveStoryboard activeStoryboard = activeStoryboard(project, storyboard, script, assets, root, unit);
        TheatreImageContextAsset drawn = "drawn".equalsIgnoreCase(activeStoryboard.variant())
                ? activeStoryboard.asset() : null;
        TheatreImageContextAsset cameraGuide = new TheatreCameraApplicationPolicy()
                .applies(storyboard, unit.segmentId())
                ? new TheatreCameraReferenceResolver()
                .resolve(project, unit.interventionId(), root)
                .map(camera -> contextAsset(camera.absolutePath(), "plano",
                        "Tipo de plano - " + camera.reference().displayName(), camera.reference().id()))
                .orElse(null)
                : null;
        TheatreImageContextAsset previous =
                adjacentFrame(project, storyboard, script, root, unit, -1).orElse(null);
        TheatreImageContextAsset next =
                adjacentFrame(project, storyboard, script, root, unit, 1).orElse(null);
        String sceneNotes = theatre.scenes().stream().filter(scene -> scene.id().equals(unit.sceneId()))
                .map(TheatreProjectLayer.Scene::notes).findFirst().orElse("");
        String actNotes = theatre.acts().stream().filter(act -> act.id().equals(unit.actId()))
                .map(TheatreProjectLayer.TheatreAct::notes).findFirst().orElse("");

        return new TheatreVisualGenerationContext(unit, List.copyOf(identities.values()),
                List.copyOf(objects.values()), List.copyOf(environments.values()), previous, next, drawn, cameraGuide,
                activeStoryboard.asset(), activeStoryboard.variant(), characterContexts, actNotes, sceneNotes);
    }

    private static List<TheatreImageContextAsset> characterReferences(
            ProjectAssetCatalog assets,
            TheatreProjectLayer theatre,
            String characterId,
            String label,
            String sceneId,
            Path root) {
        return theatre.characterImages().stream()
                .filter(image -> image.characterId().equals(characterId))
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .sorted(Comparator
                        .comparing((TheatreProjectLayer.CharacterImage image) -> image.sceneId().isBlank())
                        .thenComparingInt(image -> viewPriority(image.view()))
                        .thenComparing(TheatreProjectLayer.CharacterImage::view))
                .map(image -> assets.byId(image.assetId()).filter(ProjectAssetReference::isImage)
                        .map(asset -> contextAsset(asset, root, "personaje",
                                label + (image.view().isBlank() ? "" : " - " + image.view()),
                                Map.of("subjectId", characterId, "view", image.view(),
                                        "referenceNotes", image.notes())))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .filter(reference -> !reference.imageUri().isBlank())
                .limit(2)
                .toList();
    }

    private static void addParticipant(Map<String, String> participants,
                                       TheatreProjectLayer theatre,
                                       String value) {
        String id = normalizeCharacterId(theatre, value);
        if (id.isBlank()) return;
        String label = theatre.characters().stream().filter(character -> character.id().equals(id))
                .map(TheatreProjectLayer.CharacterProfile::displayName).findFirst().orElse(id);
        participants.put(id, label);
    }

    private static int viewPriority(String view) {
        String normalized = normalize(view);
        if (normalized.contains("frontal") || normalized.contains("frente")) return 0;
        if (normalized.contains("lateral") || normalized.contains("perfil")) return 1;
        return 2;
    }

    private static String locationFor(TheatreProjectLayer.TextActionPlacement placement,
                                      String characterId,
                                      String label,
                                      TheatreProjectLayer theatre) {
        for (Map.Entry<String, String> entry : placement.characterLocations().entrySet()) {
            if (normalizeCharacterId(theatre, entry.getKey()).equals(characterId)
                    || normalize(entry.getKey()).equals(normalize(label))) {
                return entry.getValue();
            }
        }
        if (normalizeCharacterId(theatre, placement.characterId()).equals(characterId)) {
            return !placement.destination().isBlank() ? placement.destination() : placement.origin();
        }
        return "";
    }

    private static void addObjects(Map<String, TheatreImageContextAsset> selected,
                                   ProjectAssetCatalog assets,
                                   TheatreProjectLayer theatre,
                                   String sceneId,
                                   Path root) {
        Set<String> ids = new LinkedHashSet<>();
        theatre.objectImages().stream()
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .forEach(image -> ids.add(image.objectId()));
        for (String id : ids) {
            String name = theatre.objects().stream().filter(object -> object.id().equals(id))
                    .map(TheatreProjectLayer.TheatreObject::displayName).findFirst().orElse(id);
            theatre.objectImages().stream()
                    .filter(image -> image.objectId().equals(id))
                    .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                    .sorted(Comparator.comparing((TheatreProjectLayer.ObjectImage image) -> image.sceneId().isBlank())
                            .thenComparing(TheatreProjectLayer.ObjectImage::view))
                    .findFirst()
                    .ifPresent(image -> addAsset(selected, assets, image.assetId(), root, "objeto",
                            name + (image.view().isBlank() ? "" : " - " + image.view())));
        }
    }

    private static ActiveStoryboard activeStoryboard(DocuPodcastProject project,
                                                      StoryboardDocument storyboard,
                                                      NarrationScriptDocument script,
                                                      ProjectAssetCatalog assets,
                                                      Path root,
                                                      TheatreImageGenerationUnit unit) {
        StoryboardBinding binding = storyboard == null ? null
                : storyboard.bindingForSegment(unit.segmentId()).orElse(null);
        if (binding != null) {
            Map<String, String> metadata = binding.metadata();
            String declared = metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT, "").strip();
            if (!declared.isBlank()) {
                String assetId = switch (declared.toLowerCase(Locale.ROOT)) {
                    case UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_GENERATED ->
                            metadata.getOrDefault(
                                    UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID,
                                    binding.imageAssetId());
                    case UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_DRAWN ->
                            metadata.getOrDefault(
                                    UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID,
                                    binding.imageAssetId());
                    default -> metadata.getOrDefault(
                            UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID,
                            binding.imageAssetId());
                };
                ProjectAssetReference asset = assets.byId(assetId).orElse(null);
                TheatreImageContextAsset context = asset == null
                        ? new TheatreImageContextAsset("storyboard", "Variante activa " + declared,
                        assetId, "", "", Map.of("activeVariant", declared, "invalid", "true"))
                        : contextAsset(asset, root, "storyboard", "Frame activo " + declared,
                        Map.of("activeVariant", declared));
                return new ActiveStoryboard(context, declared);
            }
        }
        if (script != null && root != null) {
            Optional<TheatrePrimaryVisualReference> resolved = new TheatrePrimaryVisualResolver()
                    .resolve(project, storyboard, script, unit.interventionId(), root);
            if (resolved.isPresent()) {
                TheatrePrimaryVisualReference reference = resolved.get();
                String variant = switch (reference.source()) {
                    case GENERATED_IMAGE -> "generated";
                    case STORYBOARD_FRAME -> "drawn";
                    case SCENERY_COMPOSITION -> "scenery";
                    case OFFICIAL_IMAGE -> "official";
                    case LEGACY_THEATRE_VISUAL -> "legacy";
                };
                return new ActiveStoryboard(contextAsset(reference.absolutePath(), "storyboard",
                        "Frame activo " + variant, reference.assetId(),
                        Map.of("activeVariant", variant)), variant);
            }
        }
        return new ActiveStoryboard(null, "");
    }

    private static Optional<TheatreImageContextAsset> adjacentFrame(
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            NarrationScriptDocument script,
            Path root,
            TheatreImageGenerationUnit unit,
            int offset) {
        if (script == null || root == null) return Optional.empty();
        List<TheatreProjectLayer.Intervencion> ordered = project.theatre().intervenciones().stream()
                .sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex)).toList();
        int index = -1;
        for (int current = 0; current < ordered.size(); current++) {
            if (ordered.get(current).id().equals(unit.interventionId())) {
                index = current;
                break;
            }
        }
        int target = index + offset;
        if (index < 0 || target < 0 || target >= ordered.size()) return Optional.empty();
        TheatreProjectLayer.Intervencion adjacent = ordered.get(target);
        Optional<NarrationSegment> segment = script.segments().stream()
                .filter(item -> item.sourceBlockIds().contains(adjacent.blockId())).findFirst();
        if (segment.isEmpty()) return Optional.empty();
        return new TheatrePrimaryVisualResolver()
                .resolve(project, storyboard, script, adjacent.id(), root)
                .map(reference -> contextAsset(reference.absolutePath(),
                        offset < 0 ? "anterior" : "siguiente",
                        offset < 0 ? "Frame anterior aprobado" : "Frame siguiente aprobado",
                        reference.assetId(), Map.of("segmentId", segment.get().id())));
    }

    private static void addAsset(Map<String, TheatreImageContextAsset> selected,
                                 ProjectAssetCatalog assets,
                                 String assetId,
                                 Path root,
                                 String role,
                                 String label) {
        if (assetId == null || assetId.isBlank()) return;
        assets.byId(assetId).filter(ProjectAssetReference::isImage)
                .ifPresent(asset -> addAsset(selected, asset, root, role, label));
    }

    private static void addAsset(Map<String, TheatreImageContextAsset> selected,
                                 ProjectAssetReference asset,
                                 Path root,
                                 String role,
                                 String label) {
        TheatreImageContextAsset value = contextAsset(asset, root, role, label);
        if (value != null) selected.putIfAbsent(role + ':' + asset.id(), value);
    }

    private static TheatreImageContextAsset contextAsset(ProjectAssetReference asset,
                                                         Path root,
                                                         String role,
                                                         String label) {
        return contextAsset(asset, root, role, label, Map.of());
    }

    private static TheatreImageContextAsset contextAsset(ProjectAssetReference asset,
                                                         Path root,
                                                         String role,
                                                         String label,
                                                         Map<String, String> metadata) {
        if (asset == null || !asset.isImage()) return null;
        String uri = "";
        if (root != null) {
            Path source = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
            if (source.startsWith(root) && Files.isRegularFile(source)) uri = source.toUri().toString();
        }
        return new TheatreImageContextAsset(
                role, label, asset.id(), asset.relativePath(), uri, metadata);
    }

    private static TheatreImageContextAsset contextAsset(Path source,
                                                         String role,
                                                         String label,
                                                         String assetId) {
        return contextAsset(source, role, label, assetId, Map.of());
    }

    private static TheatreImageContextAsset contextAsset(Path source,
                                                         String role,
                                                         String label,
                                                         String assetId,
                                                         Map<String, String> metadata) {
        if (source == null) return null;
        Path normalized = source.toAbsolutePath().normalize();
        String uri = Files.isRegularFile(normalized) ? normalized.toUri().toString() : "";
        return new TheatreImageContextAsset(
                role, label, assetId, normalized.toString(), uri, metadata);
    }

    private static String normalizeCharacterId(TheatreProjectLayer theatre, String value) {
        if (value == null || value.isBlank() || specialTarget(value)) return "";
        String normalized = normalize(value);
        return theatre.characters().stream()
                .filter(character -> normalize(character.id()).equals(normalized)
                        || normalize(character.displayName()).equals(normalized)
                        || character.aliases().stream().anyMatch(alias -> normalize(alias).equals(normalized)))
                .map(TheatreProjectLayer.CharacterProfile::id)
                .findFirst().orElse("");
    }

    private static boolean specialTarget(String value) {
        String normalized = normalize(value);
        return normalized.equals("publico") || normalized.equals("para si mismo")
                || normalized.equals("entidad no presente en escenario");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip();
    }

    private record ActiveStoryboard(TheatreImageContextAsset asset, String variant) {
        private ActiveStoryboard {
            variant = variant == null ? "" : variant.strip();
        }
    }
}
