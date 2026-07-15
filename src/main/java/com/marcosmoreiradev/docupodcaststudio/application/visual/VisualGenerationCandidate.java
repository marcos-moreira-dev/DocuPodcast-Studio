package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;

import java.nio.file.Path;

/** Generated or imported visual candidate awaiting explicit approval. */
public record VisualGenerationCandidate(
        String id,
        String fragmentId,
        FragmentAssetRole role,
        String assetId,
        Path outputPath,
        boolean approved,
        String notes
) {
    public VisualGenerationCandidate {
        id = normalize(id).isBlank() ? "VISUAL-CANDIDATE" : normalize(id);
        fragmentId = normalize(fragmentId);
        role = role == null ? FragmentAssetRole.MAIN_IMAGE : role;
        assetId = normalize(assetId);
        notes = normalize(notes);
    }

    public VisualGenerationCandidate approve(String approvedAssetId) {
        return new VisualGenerationCandidate(id, fragmentId, role, approvedAssetId, outputPath, true, notes);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
