package com.marcosmoreiradev.docupodcaststudio.application.theatre;

/** Image asset selected as visual context for a theatre AI generation unit. */
public record TheatreImageContextAsset(
        String role,
        String label,
        String assetId,
        String relativePath,
        String imageUri
) {
}
