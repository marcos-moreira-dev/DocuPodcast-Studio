package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** User decision applied after PDF semantic components have been classified. */
public enum SecondarySemanticReadingPolicy {
    OMIT_ALL(
            "Omitir todo tipo de componentes",
            "Lee la prosa principal y omite tablas, ecuaciones, imágenes y extras."),
    INTERPRET_ALL_BRIEF(
            "Interpretar todo (sin descripciones largas)",
            "Interpreta cada componente secundario con una narración breve."),
    TABLES_AND_EQUATIONS(
            "Solo interpretar cuadros y ecuaciones",
            "Interpreta tablas y ecuaciones; omite imágenes y extras."),
    IMAGES_AND_EXTRAS(
            "Solo interpretar imágenes y extras",
            "Interpreta imágenes y contenido anómalo; omite tablas y ecuaciones.");

    private final String displayName;
    private final String description;

    SecondarySemanticReadingPolicy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public boolean includes(SecondarySemanticComponentKind kind) {
        if (kind == null || !kind.secondary()) return false;
        return switch (this) {
            case OMIT_ALL -> false;
            case INTERPRET_ALL_BRIEF -> true;
            case TABLES_AND_EQUATIONS -> kind == SecondarySemanticComponentKind.TABLE
                    || kind == SecondarySemanticComponentKind.EQUATION;
            case IMAGES_AND_EXTRAS -> kind == SecondarySemanticComponentKind.IMAGE
                    || kind == SecondarySemanticComponentKind.EXTRA;
        };
    }

    public static SecondarySemanticReadingPolicy fromLegacy(
            ImageNarrationPolicy imagePolicy,
            TableNarrationPolicy tablePolicy) {
        boolean images = imagePolicy != ImageNarrationPolicy.IGNORE_IMAGES;
        boolean tables = tablePolicy != null && !tablePolicy.skips();
        if (images && tables) return INTERPRET_ALL_BRIEF;
        if (tables) return TABLES_AND_EQUATIONS;
        if (images) return IMAGES_AND_EXTRAS;
        return OMIT_ALL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
