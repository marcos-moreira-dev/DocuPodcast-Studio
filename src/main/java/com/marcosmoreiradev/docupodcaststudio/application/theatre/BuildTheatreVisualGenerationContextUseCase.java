package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds the canonical theatre context consumed by every visual-generation surface. */
public final class BuildTheatreVisualGenerationContextUseCase {
    public TheatreVisualGenerationContext execute(DocuPodcastProject project,
                                                   StoryboardDocument storyboard,
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

        addParticipantImage(identities, assets, theatre, unit.speaker(), unit.sceneId(), root);
        theatre.textActionPlacements().stream()
                .filter(candidate -> candidate.sceneId().equals(unit.sceneId())
                        && candidate.intervencionId().equals(unit.interventionId()))
                .findFirst()
                .ifPresent(placement -> addParticipants(identities, assets, theatre, placement, unit.sceneId(), root));
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
                    .ifPresent(asset -> addAsset(environments, asset, root, "entorno", "Teatro vacio"));
        }

        TheatreImageContextAsset drawn = storyboard == null ? null : storyboard.bindingForSegment(unit.segmentId())
                .map(StoryboardBinding::metadata)
                .map(metadata -> metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, ""))
                .flatMap(assets::byId)
                .map(asset -> contextAsset(asset, root, "boceto", "Frame dibujado"))
                .orElse(null);
        TheatreImageContextAsset cameraGuide = new TheatreCameraApplicationPolicy().applies(storyboard, unit.segmentId())
                ? new TheatreCameraReferenceResolver()
                        .resolve(project, unit.interventionId(), root)
                        .map(camera -> contextAsset(camera.absolutePath(), "plano",
                                "Tipo de plano - " + camera.reference().displayName(), camera.reference().id()))
                        .orElse(null)
                : null;
        return new TheatreVisualGenerationContext(unit, List.copyOf(identities.values()),
                List.copyOf(objects.values()), List.copyOf(environments.values()), null, null, drawn, cameraGuide);
    }

    private static void addParticipants(Map<String, TheatreImageContextAsset> selected,
                                        ProjectAssetCatalog assets,
                                        TheatreProjectLayer theatre,
                                        TheatreProjectLayer.TextActionPlacement placement,
                                        String sceneId,
                                        Path root) {
        LinkedHashMap<String, String> participants = new LinkedHashMap<>();
        addParticipant(participants, theatre, placement.characterId());
        if (!specialTarget(placement.interactionTarget())) addParticipant(participants, theatre, placement.interactionTarget());
        placement.characterLocations().keySet().forEach(value -> addParticipant(participants, theatre, value));
        participants.forEach((id, label) -> theatre.characterImages().stream()
                .filter(image -> image.characterId().equals(id))
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .sorted(Comparator.comparing((TheatreProjectLayer.CharacterImage image) -> image.sceneId().isBlank())
                        .thenComparing(TheatreProjectLayer.CharacterImage::view))
                .findFirst()
                .ifPresent(image -> addAsset(selected, assets, image.assetId(), root, "personaje",
                        label + (image.view().isBlank() ? "" : " - " + image.view()))));
    }

    private static void addParticipantImage(Map<String, TheatreImageContextAsset> selected,
                                            ProjectAssetCatalog assets,
                                            TheatreProjectLayer theatre,
                                            String participant,
                                            String sceneId,
                                            Path root) {
        LinkedHashMap<String, String> resolved = new LinkedHashMap<>();
        addParticipant(resolved, theatre, participant);
        resolved.forEach((id, label) -> theatre.characterImages().stream()
                .filter(image -> image.characterId().equals(id))
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .sorted(Comparator.comparing((TheatreProjectLayer.CharacterImage image) -> image.sceneId().isBlank())
                        .thenComparing(TheatreProjectLayer.CharacterImage::view))
                .findFirst()
                .ifPresent(image -> addAsset(selected, assets, image.assetId(), root, "personaje",
                        label + (image.view().isBlank() ? "" : " - " + image.view()))));
    }

    private static void addParticipant(Map<String, String> participants, TheatreProjectLayer theatre, String value) {
        String id = normalizeCharacterId(theatre, value);
        if (id.isBlank()) return;
        String label = theatre.characters().stream().filter(character -> character.id().equals(id))
                .map(TheatreProjectLayer.CharacterProfile::displayName).findFirst().orElse(id);
        participants.put(id, label);
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
        if (asset == null || !asset.isImage()) return null;
        String uri = "";
        if (root != null) {
            Path source = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
            if (source.startsWith(root) && Files.isRegularFile(source)) uri = source.toUri().toString();
        }
        return new TheatreImageContextAsset(role, label, asset.id(), asset.relativePath(), uri);
    }

    private static TheatreImageContextAsset contextAsset(Path source,
                                                         String role,
                                                         String label,
                                                         String assetId) {
        if (source == null || !Files.isRegularFile(source)) return null;
        return new TheatreImageContextAsset(role, label, assetId, source.toString(), source.toUri().toString());
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
}
