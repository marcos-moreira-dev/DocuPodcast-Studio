package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.util.Map;

/** Intended interpretation style. Actual output depends on engine capabilities. */
public record PerformanceStyle(
        String id,
        String displayName,
        String description,
        boolean requiresEngineSupport,
        Map<String, String> metadata
) {
    public PerformanceStyle {
        id = token(id, "id");
        displayName = requiredText(displayName, "displayName");
        description = normalize(description);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static PerformanceStyle neutral() {
        return new PerformanceStyle(
                "STY-NEUTRAL",
                "Neutro",
                "Lectura fluida sin intención emocional especial.",
                false,
                Map.of("builtIn", "true")
        );
    }

    public static PerformanceStyle serious() {
        return new PerformanceStyle(
                "STY-SERIOUS",
                "Serio",
                "Intención de lectura sobria. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true")
        );
    }

    public static PerformanceStyle dramatic() {
        return new PerformanceStyle(
                "STY-DRAMATIC",
                "Dramático",
                "Intención de interpretación teatral. Depende de muestras/modelo compatibles.",
                true,
                Map.of("builtIn", "true")
        );
    }


    public static PerformanceStyle warm() {
        return new PerformanceStyle(
                "STY-WARM",
                "Cálido",
                "Lectura cercana y amable. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true", "tone", "warm")
        );
    }

    public static PerformanceStyle happy() {
        return new PerformanceStyle(
                "STY-HAPPY",
                "Feliz",
                "Lectura positiva y expresiva. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true", "tone", "happy")
        );
    }

    public static PerformanceStyle cheerful() {
        return new PerformanceStyle(
                "STY-CHEERFUL",
                "Alegre",
                "Lectura animada y enérgica. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true", "tone", "cheerful")
        );
    }

    public static PerformanceStyle sad() {
        return new PerformanceStyle(
                "STY-SAD",
                "Triste",
                "Lectura contenida o melancólica. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true", "tone", "sad")
        );
    }

    public static PerformanceStyle calm() {
        return new PerformanceStyle(
                "STY-CALM",
                "Calmado",
                "Lectura pausada y tranquila. Depende de capacidades del motor.",
                true,
                Map.of("builtIn", "true", "tone", "calm")
        );
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String requiredText(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
