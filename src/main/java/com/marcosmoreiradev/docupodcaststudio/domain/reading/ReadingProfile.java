package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import java.util.Objects;

/** User-visible profile that defines how an imported document becomes a narratable structure. */
public record ReadingProfile(
        String id,
        String name,
        String description,
        HeadingDetectionRules headingRules,
        ImageNarrationPolicy imagePolicy,
        TableNarrationPolicy tablePolicy
) {
    public ReadingProfile {
        id = required(id, "id");
        name = required(name, "name");
        description = description == null ? "" : description.strip();
        headingRules = Objects.requireNonNullElseGet(headingRules, HeadingDetectionRules::academicDefaults);
        imagePolicy = Objects.requireNonNullElse(imagePolicy, ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT);
        tablePolicy = Objects.requireNonNullElse(tablePolicy, TableNarrationPolicy.ANNOUNCE_SUMMARY);
    }

    public static ReadingProfile academicDefaults() {
        return new ReadingProfile(
                "reading-profile-academic-default",
                "Documento académico Word",
                "Perfil inicial para notas en DOCX: prioriza estilos de Word, listas, imágenes con descripción y fallback suave por negrita breve.",
                HeadingDetectionRules.academicDefaults(),
                ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT,
                TableNarrationPolicy.ANNOUNCE_SUMMARY
        );
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}
