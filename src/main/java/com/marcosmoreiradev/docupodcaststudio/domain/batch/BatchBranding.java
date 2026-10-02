package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Optional overlay copied into the batch; it never becomes narrated document content. */
public record BatchBranding(
        boolean enabled,
        String sourcePath,
        String projectRelativePath,
        BrandingPlacement placement,
        int sizePercent,
        double opacity
) {
    public BatchBranding {
        sourcePath = sourcePath == null ? "" : sourcePath.strip();
        projectRelativePath = projectRelativePath == null ? "" : projectRelativePath.strip();
        placement = placement == null ? BrandingPlacement.BOTTOM_RIGHT : placement;
        sizePercent = Math.max(5, Math.min(40, sizePercent));
        opacity = Math.max(0.1, Math.min(1.0, opacity));
        if (sourcePath.isBlank()) enabled = false;
    }

    public static BatchBranding none() {
        return new BatchBranding(false, "", "", BrandingPlacement.BOTTOM_RIGHT, 15, 0.9);
    }
}
