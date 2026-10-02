package com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Deterministic snapshot of a theatre package. */
public record TheatrePackageInventory(
        int schemaVersion,
        String packageId,
        String packageVersion,
        String sourceRoot,
        List<TheatrePackageEntry> entries,
        String fingerprint
) {
    public TheatrePackageInventory {
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be positive");
        packageId = required(packageId, "packageId");
        packageVersion = Objects.requireNonNullElse(packageVersion, "").strip();
        sourceRoot = required(sourceRoot, "sourceRoot");
        entries = entries == null ? List.of() : entries.stream()
                .sorted(Comparator.comparing(TheatrePackageEntry::logicalId))
                .toList();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (TheatrePackageEntry entry : entries) {
            if (!ids.add(entry.logicalId())) {
                throw new IllegalArgumentException("Duplicated theatre package logicalId: " + entry.logicalId());
            }
        }
        String calculated = fingerprint(schemaVersion, packageId, packageVersion, entries);
        fingerprint = Objects.requireNonNullElse(fingerprint, "").isBlank() ? calculated : fingerprint.strip();
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("Theatre package fingerprint does not match its entries");
        }
    }

    public static TheatrePackageInventory create(int schemaVersion, String packageId, String packageVersion,
                                                 String sourceRoot, List<TheatrePackageEntry> entries) {
        return new TheatrePackageInventory(schemaVersion, packageId, packageVersion, sourceRoot, entries, "");
    }

    public static String fingerprint(int schemaVersion, String packageId, String packageVersion,
                                     List<TheatrePackageEntry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, Integer.toString(schemaVersion));
            update(digest, required(packageId, "packageId"));
            update(digest, Objects.requireNonNullElse(packageVersion, "").strip());
            List<TheatrePackageEntry> ordered = entries == null ? List.of() : entries.stream()
                    .sorted(Comparator.comparing(TheatrePackageEntry::logicalId)).toList();
            for (TheatrePackageEntry entry : ordered) {
                update(digest, entry.logicalId());
                update(digest, entry.kind().name());
                update(digest, entry.relativePath());
                update(digest, entry.sha256());
                update(digest, Long.toString(entry.size()));
                entry.metadata().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(value -> {
                            update(digest, value.getKey());
                            update(digest, value.getValue());
                        });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
