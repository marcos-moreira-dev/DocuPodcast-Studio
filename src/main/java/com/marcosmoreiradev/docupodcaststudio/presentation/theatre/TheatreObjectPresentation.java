package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Presentation row for a stage object, prop or scenery reference. */
public record TheatreObjectPresentation(
        String id,
        String displayName,
        String description
) {
    public TheatreObjectPresentation {
        id = normalize(id);
        displayName = normalize(displayName);
        description = normalize(description);
    }

    public boolean hasDescription() {
        return !description.isBlank();
    }

    public String cardPreview() {
        if (!description.isBlank()) {
            return excerpt(description, 92);
        }
        return "Ficha t\u00e9cnica pendiente. Pulsa Ver ficha para escribirla.";
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

    static TheatreObjectPresentation from(com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer.TheatreObject object) {
        return new TheatreObjectPresentation(object.id(), object.displayName(), object.notes());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
