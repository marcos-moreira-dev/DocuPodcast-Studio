package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.Map;

/** Image asset selected as visual context for a theatre AI generation unit. */
public record TheatreImageContextAsset(
        String role,
        String label,
        String assetId,
        String relativePath,
        String imageUri,
        Map<String, String> metadata
) {
    public TheatreImageContextAsset(String role, String label, String assetId,
                                    String relativePath, String imageUri) {
        this(role, label, assetId, relativePath, imageUri, Map.of());
    }

    public TheatreImageContextAsset {
        role = role == null ? "" : role.strip();
        label = label == null ? "" : label.strip();
        assetId = assetId == null ? "" : assetId.strip();
        relativePath = relativePath == null ? "" : relativePath.strip();
        imageUri = imageUri == null ? "" : imageUri.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
