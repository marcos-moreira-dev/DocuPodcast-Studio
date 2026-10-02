package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.List;

/** Identity, wardrobe and blocking data for one character in a generated intervention. */
public record TheatreCharacterGenerationContext(
        String characterId,
        String displayName,
        String notes,
        String location,
        boolean speaker,
        List<TheatreImageContextAsset> references
) {
    public TheatreCharacterGenerationContext {
        characterId = characterId == null ? "" : characterId.strip();
        displayName = displayName == null || displayName.isBlank() ? characterId : displayName.strip();
        notes = notes == null ? "" : notes.strip();
        location = location == null ? "" : location.strip();
        references = references == null ? List.of() : List.copyOf(references);
    }

    public boolean identityReady() {
        return references.stream().anyMatch(reference -> {
            if (reference.imageUri().isBlank()) return false;
            try {
                return java.nio.file.Files.isRegularFile(
                        java.nio.file.Path.of(java.net.URI.create(reference.imageUri())));
            } catch (RuntimeException ignored) {
                return false;
            }
        });
    }
}
