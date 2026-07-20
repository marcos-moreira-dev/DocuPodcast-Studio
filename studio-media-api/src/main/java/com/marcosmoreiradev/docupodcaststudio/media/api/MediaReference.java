package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Provider-neutral local media reference. */
public record MediaReference(
        String id,
        Path file,
        MediaReferenceRole role,
        double strength,
        Map<String, String> metadata) {
    public MediaReference {
        id = id == null ? "" : id.strip();
        if (file == null) throw new IllegalArgumentException("reference file is required");
        file = file.toAbsolutePath().normalize();
        role = role == null ? MediaReferenceRole.OBJECT : role;
        strength = Double.isFinite(strength) ? Math.max(0.0, Math.min(1.0, strength)) : 1.0;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
