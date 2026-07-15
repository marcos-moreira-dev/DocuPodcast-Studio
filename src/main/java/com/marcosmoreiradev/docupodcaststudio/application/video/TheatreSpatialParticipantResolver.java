package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Resolves the people and destinations represented on a theatre spatial map. */
final class TheatreSpatialParticipantResolver {

    List<Participant> resolve(DocuPodcastProject project,
                              TheatreProjectLayer.TextActionPlacement placement,
                              String sceneId,
                              Map<String, String> characterNames) {
        if (placement == null) {
            return List.of();
        }
        LinkedHashMap<String, Participant> result = new LinkedHashMap<>();
        String speakerName = speakerName(placement, characterNames);
        if (!speakerName.isBlank()) {
            result.put(speakerName, new Participant(
                    placement.characterId(),
                    speakerName,
                    locationFor(placement, speakerName, placement.origin()),
                    TheatreSpatialRoleIcon.forSpeaker(speakerName),
                    true));
        }
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                continue;
            }
            if (TheatreSpatialRoleIcon.isAudience(target)) {
                result.putIfAbsent("PUBLICO", new Participant(
                        "", "PUBLICO", audienceLocation(), TheatreSpatialRoleIcon.AUDIENCE, false));
                continue;
            }
            if (isOffstageTarget(target)) {
                continue;
            }
            String targetId = characterIdForName(project, target);
            String targetName = displayNameForName(project, target);
            if (isCharacterAbsent(placement, targetName.isBlank() ? target : targetName)) {
                continue;
            }
            if (!targetName.isBlank() && !targetName.equalsIgnoreCase(speakerName)) {
                result.put(targetName, new Participant(
                        targetId,
                        targetName,
                        locationFor(placement, targetName, placement.destination()),
                        TheatreSpatialRoleIcon.forSpeaker(targetName),
                        false));
            }
        }
        if (TheatreSpatialRoleIcon.isAudience(placement.destination())) {
            result.putIfAbsent("PUBLICO", new Participant(
                    "", "PUBLICO", audienceLocation(), TheatreSpatialRoleIcon.AUDIENCE, false));
        }
        placement.characterLocations().forEach((name, location) -> {
            if (name == null || name.isBlank()
                    || TheatreStageGeometry.specialInteractionTarget(name)
                    || isAbsentLocation(location)) {
                return;
            }
            String displayName = displayNameForName(project, name);
            if (displayName.isBlank()) {
                displayName = name.strip();
            }
            result.putIfAbsent(displayName, new Participant(
                    characterIdForName(project, displayName),
                    displayName,
                    location,
                    TheatreSpatialRoleIcon.forSpeaker(displayName),
                    false));
        });
        return List.copyOf(result.values());
    }

    List<String> destinationLocations(DocuPodcastProject project,
                                      TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return List.of("centro");
        }
        ArrayList<String> result = new ArrayList<>();
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                addUnique(result, placement.origin());
            } else if (TheatreSpatialRoleIcon.isAudience(target)) {
                addUnique(result, audienceLocation());
            } else if (isOffstageTarget(target)) {
                addUnique(result, placement.destination());
            } else {
                String targetName = displayNameForName(project, target);
                if (isCharacterAbsent(placement, targetName.isBlank() ? target : targetName)) {
                    continue;
                }
                addUnique(result, locationFor(
                        placement,
                        targetName.isBlank() ? target : targetName,
                        placement.destination()));
            }
        }
        if (result.isEmpty()) {
            addUnique(result, placement.destination());
        }
        return List.copyOf(result);
    }

    String speakerName(TheatreProjectLayer.TextActionPlacement placement,
                       Map<String, String> characterNames) {
        if (placement == null) {
            return "";
        }
        String byId = characterNames == null ? "" : characterNames.getOrDefault(placement.characterId(), "");
        if (!byId.isBlank()) {
            return byId;
        }
        return placement.characterLocations().keySet().stream()
                .filter(name -> !TheatreStageGeometry.specialInteractionTarget(name))
                .findFirst()
                .orElse("");
    }

    Optional<ProjectAssetReference> characterImage(DocuPodcastProject project,
                                                    String characterId,
                                                    String sceneId) {
        if (project == null || characterId == null || characterId.isBlank()) {
            return Optional.empty();
        }
        TheatreProjectLayer theatre = project.theatre();
        ProjectAssetCatalog assets = project.assets();
        return theatre.characterImages().stream()
                .filter(image -> characterId.equals(image.characterId()))
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .findFirst()
                .flatMap(image -> assets.byId(image.assetId()))
                .filter(ProjectAssetReference::isImage);
    }

    private static List<String> interactionTargets(String target) {
        String normalized = target == null ? "" : target.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (TheatreStageGeometry.specialInteractionTarget(normalized)) {
            return List.of(normalized);
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

    private static boolean isSelfTarget(String target) {
        return normalizeToken(target).equals("para si mismo");
    }

    private static boolean isOffstageTarget(String target) {
        return normalizeToken(target).equals("entidad no presente en escenario");
    }

    private static String audienceLocation() {
        return "hacia el publico";
    }

    private static void addUnique(List<String> values, String value) {
        if (isAbsentLocation(value)) {
            return;
        }
        String safe = value == null || value.isBlank() ? "centro" : value.strip();
        if (!values.contains(safe)) {
            values.add(safe);
        }
    }

    private static boolean isCharacterAbsent(TheatreProjectLayer.TextActionPlacement placement, String character) {
        if (placement == null || character == null || character.isBlank()) {
            return false;
        }
        return placement.characterLocations().entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase(character)
                        && isAbsentLocation(entry.getValue()));
    }

    private static boolean isAbsentLocation(String location) {
        String normalized = location == null ? "" : normalizeToken(location);
        return normalized.equals("no presente") || normalized.equals("no presente en esta intervencion");
    }

    private static String locationFor(TheatreProjectLayer.TextActionPlacement placement,
                                      String characterName,
                                      String fallback) {
        for (Map.Entry<String, String> entry : placement.characterLocations().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(characterName)
                    && entry.getValue() != null
                    && !entry.getValue().isBlank()
                    && !isAbsentLocation(entry.getValue())) {
                return entry.getValue();
            }
        }
        return fallback == null || fallback.isBlank() ? "centro" : fallback;
    }

    private static String characterIdForName(DocuPodcastProject project, String name) {
        if (project == null || name == null || name.isBlank()) {
            return "";
        }
        String normalized = normalizeName(name);
        return project.theatre().characters().stream()
                .filter(character -> normalizeName(character.displayName()).equals(normalized))
                .map(TheatreProjectLayer.CharacterProfile::id)
                .findFirst()
                .orElse("");
    }

    private static String displayNameForName(DocuPodcastProject project, String name) {
        if (project == null || name == null || name.isBlank()) {
            return "";
        }
        String normalized = normalizeName(name);
        return project.theatre().characters().stream()
                .filter(character -> normalizeName(character.displayName()).equals(normalized))
                .map(TheatreProjectLayer.CharacterProfile::displayName)
                .findFirst()
                .orElseGet(name::strip);
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    record Participant(String characterId,
                       String name,
                       String location,
                       TheatreSpatialRoleIcon role,
                       boolean speaking) {
        Participant {
            name = name == null ? "" : name.strip();
            location = location == null || location.isBlank() ? "centro" : location.strip();
            role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
        }
    }
}
