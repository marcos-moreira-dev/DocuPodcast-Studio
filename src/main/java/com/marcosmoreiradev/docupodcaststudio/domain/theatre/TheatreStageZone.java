package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import java.text.Normalizer;
import java.util.Locale;

/** Canonical nine-zone stage catalogue shared by grammar, GUI and rendering. */
public enum TheatreStageZone {
    FONDO_IZQUIERDA(1.0 / 6.0, 1.0 / 6.0),
    FONDO_CENTRO(0.5, 1.0 / 6.0),
    FONDO_DERECHA(5.0 / 6.0, 1.0 / 6.0),
    CENTRO_IZQUIERDA(1.0 / 6.0, 0.5),
    CENTRO(0.5, 0.5),
    CENTRO_DERECHA(5.0 / 6.0, 0.5),
    FRENTE_IZQUIERDA(1.0 / 6.0, 5.0 / 6.0),
    FRENTE_CENTRO(0.5, 5.0 / 6.0),
    FRENTE_DERECHA(5.0 / 6.0, 5.0 / 6.0);

    private final double x;
    private final double y;

    TheatreStageZone(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() { return x; }
    public double y() { return y; }
    public String grammarValue() { return name().toLowerCase(Locale.ROOT); }

    public static TheatreStageZone parse(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .strip().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ATRAS_IZQUIERDA", "FONDO_IZQ" -> FONDO_IZQUIERDA;
            case "ATRAS_CENTRO" -> FONDO_CENTRO;
            case "ATRAS_DERECHA", "FONDO_DER" -> FONDO_DERECHA;
            case "MEDIO_IZQUIERDA", "CENTRO_IZQ" -> CENTRO_IZQUIERDA;
            case "MEDIO", "MEDIO_CENTRO" -> CENTRO;
            case "MEDIO_DERECHA", "CENTRO_DER" -> CENTRO_DERECHA;
            case "ADELANTE_IZQUIERDA", "FRENTE_IZQ" -> FRENTE_IZQUIERDA;
            case "ADELANTE_CENTRO" -> FRENTE_CENTRO;
            case "ADELANTE_DERECHA", "FRENTE_DER" -> FRENTE_DERECHA;
            default -> valueOf(normalized);
        };
    }
}
