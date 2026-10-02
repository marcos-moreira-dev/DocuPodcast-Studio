package com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Replaceable state written only after a successful theatre-package commit. */
public record TheatreImportState(
        int schemaVersion,
        String packageId,
        String packageVersion,
        String sourceRoot,
        String inventoryFingerprint,
        Instant appliedAt,
        List<TheatrePackageEntry> entries
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public TheatreImportState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported theatre import state schema: " + schemaVersion);
        }
        packageId = required(packageId, "packageId");
        packageVersion = Objects.requireNonNullElse(packageVersion, "").strip();
        sourceRoot = required(sourceRoot, "sourceRoot");
        inventoryFingerprint = required(inventoryFingerprint, "inventoryFingerprint");
        appliedAt = Objects.requireNonNull(appliedAt, "appliedAt");
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static TheatreImportState fromInventory(TheatrePackageInventory inventory, Instant appliedAt,
                                                   List<TheatrePackageEntry> materializedEntries) {
        Objects.requireNonNull(inventory, "inventory");
        return new TheatreImportState(CURRENT_SCHEMA_VERSION, inventory.packageId(), inventory.packageVersion(),
                inventory.sourceRoot(), inventory.fingerprint(), appliedAt,
                materializedEntries == null ? inventory.entries() : materializedEntries);
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
