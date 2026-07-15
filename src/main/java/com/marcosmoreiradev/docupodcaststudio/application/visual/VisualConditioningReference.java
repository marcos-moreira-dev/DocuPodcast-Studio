package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;

/** A real project image that must be consumed by the selected ComfyUI workflow. */
public record VisualConditioningReference(
        String assetId,
        String label,
        Path imagePath,
        VisualConditioningRole role,
        double strength,
        String engineImageName
) {
    public VisualConditioningReference {
        assetId = clean(assetId);
        label = clean(label);
        imagePath = imagePath == null ? null : imagePath.toAbsolutePath().normalize();
        role = role == null ? VisualConditioningRole.OBJECT : role;
        strength = Math.max(0.0, Math.min(1.0, strength));
        engineImageName = clean(engineImageName);
    }

    public VisualConditioningReference(String assetId,
                                       String label,
                                       Path imagePath,
                                       VisualConditioningRole role,
                                       double strength) {
        this(assetId, label, imagePath, role, strength, "");
    }

    public VisualConditioningReference withEngineImageName(String value) {
        return new VisualConditioningReference(assetId, label, imagePath, role, strength, value);
    }

    public boolean usable() {
        return imagePath != null && java.nio.file.Files.isRegularFile(imagePath);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
