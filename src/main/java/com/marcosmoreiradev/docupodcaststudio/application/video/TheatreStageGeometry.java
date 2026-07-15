package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.text.Normalizer;
import java.util.Locale;

/** Normalized theatre stage geometry shared by preview and video export. */
public final class TheatreStageGeometry {
    public static final double REFERENCE_WIDTH = 760.0;
    public static final double REFERENCE_HEIGHT = 476.0;

    private TheatreStageGeometry() {
    }

    public static StagePoint pointFor(String location) {
        return switch (normalize(location)) {
            case "fondo derecha" -> new StagePoint(240, 155);
            case "fondo centro" -> new StagePoint(380, 155);
            case "fondo izquierda" -> new StagePoint(520, 155);
            case "centro derecha" -> new StagePoint(240, 238);
            case "centro izquierda" -> new StagePoint(520, 238);
            case "frente derecha" -> new StagePoint(240, 318);
            case "frente centro" -> new StagePoint(380, 318);
            case "frente izquierda" -> new StagePoint(520, 318);
            case "hacia el publico" -> new StagePoint(380, 402);
            case "extra diegetico" -> new StagePoint(665, 105);
            case "diegetico" -> new StagePoint(690, 238);
            default -> new StagePoint(380, 238);
        };
    }

    public static boolean knownPosition(String location) {
        return switch (normalize(location)) {
            case "fondo derecha", "fondo centro", "fondo izquierda",
                    "centro derecha", "centro", "centro izquierda",
                    "frente derecha", "frente centro", "frente izquierda",
                    "hacia el publico", "extra diegetico", "diegetico" -> true;
            default -> false;
        };
    }

    public static String normalizeFrameMode(String mode) {
        String normalized = normalize(mode);
        return switch (normalized) {
            case "characters", "personajes" -> "characters";
            case "none", "sin acompanante", "sin acompañante" -> "none";
            default -> "fragments";
        };
    }

    public static boolean specialInteractionTarget(String target) {
        String normalized = normalize(target);
        return normalized.equals("publico")
                || normalized.equals("para si mismo")
                || normalized.equals("entidad no presente en escenario");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    public record StagePoint(double x, double y) {
        public double scaledX(double targetWidth) {
            return x * targetWidth / REFERENCE_WIDTH;
        }

        public double scaledY(double targetHeight) {
            return y * targetHeight / REFERENCE_HEIGHT;
        }
    }
}
