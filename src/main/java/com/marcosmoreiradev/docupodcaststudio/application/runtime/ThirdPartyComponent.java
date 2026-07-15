package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.Objects;

/** One legal/operational third-party entry in the productization manifest. */
public record ThirdPartyComponent(
        String id,
        String displayName,
        ThirdPartyComponentKind kind,
        String expectedLocation,
        String licenseName,
        String licenseUrl,
        boolean redistributedByDefault,
        boolean requiresUserProvidedFiles,
        String notes
) {
    public ThirdPartyComponent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        Objects.requireNonNull(kind, "kind");
        expectedLocation = expectedLocation == null ? "" : expectedLocation.trim();
        licenseName = licenseName == null ? "Pendiente de confirmar" : licenseName.trim();
        licenseUrl = licenseUrl == null ? "" : licenseUrl.trim();
        notes = notes == null ? "" : notes.trim();
    }
}
