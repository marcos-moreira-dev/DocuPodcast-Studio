package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** One-time memory cost of a shareable resident model. */
public record ModelResidencyDemand(
        ModelResidencyKey key,
        long hostMemoryBytes,
        long vramBytes) {
    public ModelResidencyDemand {
        key = Objects.requireNonNull(key, "model residency key");
        hostMemoryBytes = Math.max(0L, hostMemoryBytes);
        vramBytes = Math.max(0L, vramBytes);
    }
}
