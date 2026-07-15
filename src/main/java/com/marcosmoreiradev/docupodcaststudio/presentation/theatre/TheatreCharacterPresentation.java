package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Presentation row for a character detected from theatre dialogue lines. */
public record TheatreCharacterPresentation(
        String id,
        String displayName,
        String firstLinePreview,
        String description,
        int mentionCount
) {
    public TheatreCharacterPresentation {
        id = normalize(id);
        displayName = normalize(displayName);
        firstLinePreview = normalize(firstLinePreview);
        description = normalize(description);
        mentionCount = Math.max(0, mentionCount);
    }

    public boolean hasDescription() {
        return !description.isBlank();
    }

    public String cardPreview() {
        if (!description.isBlank()) {
            return excerpt(description, 92);
        }
        return "Ficha t\u00e9cnica pendiente. Pulsa Ver descripci\u00f3n para escribirla.";
    }

    public String sheetStatusLabel() {
        return hasDescription() ? "Ficha t\u00e9cnica disponible" : "Ficha t\u00e9cnica pendiente";
    }

    static String excerpt(String value, int maxCharacters) {
        String normalized = normalize(value);
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxCharacters)).strip() + "...";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
