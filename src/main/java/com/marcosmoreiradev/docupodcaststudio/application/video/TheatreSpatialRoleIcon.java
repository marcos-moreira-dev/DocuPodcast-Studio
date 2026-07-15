package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.text.Normalizer;
import java.util.Locale;

/** Role-based marker icons shared by theatre spatial preview and exported frames. */
public enum TheatreSpatialRoleIcon {
    ACTOR("/images/theatre/spatial/actor-mapa-espacial.png"),
    NARRATOR("/images/theatre/spatial/narrador-mapa-espacial.png"),
    AUDIENCE("/images/theatre/spatial/publico-mapa-espacial.png");

    private final String resourcePath;

    TheatreSpatialRoleIcon(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public static TheatreSpatialRoleIcon forSpeaker(String speaker) {
        if (isNarrator(speaker)) {
            return NARRATOR;
        }
        if (isAudience(speaker)) {
            return AUDIENCE;
        }
        return ACTOR;
    }

    public static TheatreSpatialRoleIcon forTarget(String target, String destinationLocation, TheatreSpatialRoleIcon fallback) {
        if (isAudience(target) || isAudience(destinationLocation)) {
            return AUDIENCE;
        }
        if (isNarrator(target)) {
            return NARRATOR;
        }
        return fallback == null ? ACTOR : fallback;
    }

    public static boolean isAudience(String value) {
        String normalized = normalize(value);
        return normalized.equals("publico")
                || normalized.equals("audiencia")
                || normalized.equals("audience")
                || normalized.equals("public")
                || normalized.equals("hacia el publico");
    }

    public static boolean isNarrator(String value) {
        String normalized = normalize(value);
        return normalized.equals("narrador")
                || normalized.equals("narradora")
                || normalized.equals("narrator")
                || normalized.startsWith("narrador ")
                || normalized.startsWith("narradora ");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
