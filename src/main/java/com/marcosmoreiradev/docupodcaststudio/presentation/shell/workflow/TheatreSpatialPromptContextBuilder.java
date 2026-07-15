package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds compact spatial context for theatre frame prompts without changing persistent models. */
final class TheatreSpatialPromptContextBuilder {
    private static final String ABSENT_LOCATION = "no presente";
    private static final String STAGE_ORIENTATION = "del escenario, desde la perspectiva del personaje que mira hacia el publico";

    private TheatreSpatialPromptContextBuilder() {
    }

    static String build(TheatreProjectLayer.TextActionPlacement placement, String fallbackSpeaker) {
        if (placement == null || "_".equals(placement.intervencionId())) {
            return "";
        }
        String technicalSpeaker = normalize(placement.characterId());
        String lookupSpeaker = firstNonBlank(technicalSpeaker, fallbackSpeaker);
        String speaker = readableSpeaker(technicalSpeaker, fallbackSpeaker);
        String origin = locationFor(placement, lookupSpeaker, placement.origin());
        String destination = normalize(placement.destination());
        StringBuilder text = new StringBuilder("Disposicion espacial configurada:");
        if (!speaker.isBlank() && !origin.isBlank()) {
            text.append("\n- ").append(speaker).append(" esta ubicado en ").append(stageLocation(origin)).append('.');
        }
        appendInteraction(text, placement, speaker, destination);
        appendDirection(text, origin, destination);
        appendOtherCharacters(text, placement.characterLocations(), speaker, lookupSpeaker, placement.interactionTarget());
        appendStageOrientation(text);
        return text.toString();
    }

    private static void appendInteraction(StringBuilder text,
                                          TheatreProjectLayer.TextActionPlacement placement,
                                          String speaker,
                                          String destination) {
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (target.isBlank()) {
                continue;
            }
            if (isSelfTarget(target)) {
                text.append("\n- ").append(speaker).append(" dirige la accion hacia si mismo.");
                continue;
            }
            if (isSpecialTarget(target)) {
                text.append("\n- ").append(speaker).append(" interactua con ").append(target).append('.');
                continue;
            }
            String location = locationFor(placement, target, destination);
            text.append("\n- ").append(speaker).append(" interactua con ").append(displayCharacter(target));
            if (!location.isBlank()) {
                text.append(", ubicado en ").append(stageLocation(location));
            }
            text.append('.');
        }
    }

    private static void appendDirection(StringBuilder text, String origin, String destination) {
        if (!origin.isBlank() && !destination.isBlank() && !origin.equalsIgnoreCase(destination)) {
            text.append("\n- Direccion escenica: desde ").append(stageLocation(origin))
                    .append(" hacia ").append(stageLocation(destination)).append('.');
        }
    }

    private static void appendOtherCharacters(StringBuilder text,
                                              Map<String, String> locations,
                                              String speaker,
                                              String technicalSpeaker,
                                              String interactionTarget) {
        Set<String> targets = new LinkedHashSet<>(interactionTargets(interactionTarget));
        if (locations == null || locations.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : locations.entrySet()) {
            String character = normalize(entry.getKey());
            String location = normalize(entry.getValue());
            if (character.isBlank() || location.isBlank() || isAbsentLocation(location)
                    || sameCharacter(character, speaker) || sameCharacter(character, technicalSpeaker)
                    || containsCharacter(targets, character)) {
                continue;
            }
            text.append("\n- ").append(displayCharacter(character)).append(" esta ubicado en ").append(stageLocation(location)).append('.');
        }
    }

    private static void appendStageOrientation(StringBuilder text) {
        text.append("\n- Convencion espacial: izquierda y derecha se refieren a la perspectiva del personaje que mira hacia el publico.");
        text.append("\n- Guia de encuadre: la convencion espacial no implica mostrar al publico; no lo renderices salvo que el guion lo solicite de forma explicita.");
    }

    private static String locationFor(TheatreProjectLayer.TextActionPlacement placement, String character, String fallback) {
        String normalizedCharacter = normalize(character);
        if (!normalizedCharacter.isBlank()) {
            for (Map.Entry<String, String> entry : placement.characterLocations().entrySet()) {
                String location = normalize(entry.getValue());
                if (normalize(entry.getKey()).equalsIgnoreCase(normalizedCharacter)
                        && !location.isBlank()
                        && !isAbsentLocation(location)) {
                    return location;
                }
            }
        }
        return normalize(fallback);
    }

    private static String stageLocation(String location) {
        String normalized = normalize(location);
        if (normalized.isBlank() || isOffStageLocation(normalized)) {
            return normalized;
        }
        return normalized + " " + STAGE_ORIENTATION;
    }

    private static boolean isOffStageLocation(String location) {
        return normalize(location).equalsIgnoreCase("fuera de escena");
    }

    private static String displayCharacter(String character) {
        String normalized = normalize(character);
        if (!normalized.regionMatches(true, 0, "CHR-", 0, 4)) {
            return normalized;
        }
        return normalized.substring(4).replace('-', ' ').strip();
    }

    private static String readableSpeaker(String technicalSpeaker, String fallbackSpeaker) {
        String technical = displayCharacter(technicalSpeaker);
        String fallback = displayCharacter(fallbackSpeaker);
        if (technical.isBlank() || sameCharacter(technical, fallback)) {
            return fallback.isBlank() ? technical : fallback;
        }
        return technical;
    }

    private static ArrayList<String> interactionTargets(String target) {
        String normalized = normalize(target);
        ArrayList<String> result = new ArrayList<>();
        if (normalized.isBlank()) {
            return result;
        }
        if (isSpecialTarget(normalized)) {
            result.add(normalized);
            return result;
        }
        for (String part : normalized.split("\\s*(?:,|;|/|\\s+y\\s+)\\s*")) {
            String item = normalize(part);
            if (!item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean containsCharacter(Set<String> values, String candidate) {
        return values.stream().anyMatch(value -> sameCharacter(value, candidate));
    }

    private static boolean sameCharacter(String first, String second) {
        String left = displayCharacter(first).replaceAll("\\s+", " ").strip();
        String right = displayCharacter(second).replaceAll("\\s+", " ").strip();
        return !left.isBlank() && left.equalsIgnoreCase(right);
    }

    private static boolean isSelfTarget(String target) {
        String normalized = normalize(target).toLowerCase(Locale.ROOT);
        return normalized.equals("para si mismo") || normalized.equals("para si misma")
                || normalized.equals("para si");
    }

    private static boolean isSpecialTarget(String target) {
        String normalized = normalize(target).toLowerCase(Locale.ROOT);
        return normalized.equals("publico")
                || normalized.equals("para si mismo")
                || normalized.equals("para si misma")
                || normalized.equals("entidad no presente en escenario");
    }

    private static boolean isAbsentLocation(String location) {
        String normalized = normalize(location).toLowerCase(Locale.ROOT);
        return normalized.equals(ABSENT_LOCATION) || normalized.equals("no presente en esta intervencion");
    }

    private static String firstNonBlank(String first, String second) {
        String normalized = normalize(first);
        return normalized.isBlank() ? normalize(second) : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
