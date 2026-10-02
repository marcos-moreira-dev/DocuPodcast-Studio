package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDelta;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreRefreshPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Computes a deterministic, non-destructive refresh plan. */
public final class ComputeTheatreRefreshPlanUseCase {
    public TheatreRefreshPlan compute(TheatreImportState previous, TheatrePackageInventory current) {
        Objects.requireNonNull(current, "current");
        if (previous != null && !previous.packageId().equals(current.packageId())) {
            return new TheatreRefreshPlan(current.packageId(), previous.inventoryFingerprint(), current.fingerprint(),
                    List.of(new TheatrePackageDelta(TheatrePackageDeltaStatus.CONFLICT, null,
                            current.entries().isEmpty() ? synthetic(current) : current.entries().getFirst(),
                            "La carpeta pertenece a otro packageId.")));
        }

        Map<String, TheatrePackageEntry> beforeById = byId(previous == null ? List.of() : previous.entries());
        Map<String, TheatrePackageEntry> afterById = byId(current.entries());
        ArrayList<TheatrePackageDelta> deltas = new ArrayList<>();
        ArrayList<TheatrePackageEntry> removed = new ArrayList<>();
        ArrayList<TheatrePackageEntry> added = new ArrayList<>();

        for (TheatrePackageEntry after : current.entries()) {
            TheatrePackageEntry before = beforeById.get(after.logicalId());
            if (before == null) {
                added.add(after);
            } else if (sameContent(before, after) && before.relativePath().equals(after.relativePath())) {
                deltas.add(new TheatrePackageDelta(TheatrePackageDeltaStatus.UNCHANGED, before, after, "Sin cambios."));
            } else {
                deltas.add(new TheatrePackageDelta(TheatrePackageDeltaStatus.MODIFIED, before, after,
                        "Cambió el contenido, la ruta o los metadatos del asset."));
            }
        }
        for (TheatrePackageEntry before : beforeById.values()) {
            if (!afterById.containsKey(before.logicalId())) removed.add(before);
        }

        matchUnambiguousRenames(removed, added, deltas);
        added.forEach(entry -> deltas.add(new TheatrePackageDelta(TheatrePackageDeltaStatus.NEW, null, entry, "Asset nuevo.")));
        removed.forEach(entry -> deltas.add(new TheatrePackageDelta(TheatrePackageDeltaStatus.MISSING_SOURCE_RETAINED,
                entry, null, "Falta en el origen; se conserva la copia materializada.")));
        deltas.sort(java.util.Comparator.comparing(TheatrePackageDelta::logicalId));
        return new TheatreRefreshPlan(current.packageId(), previous == null ? "" : previous.inventoryFingerprint(),
                current.fingerprint(), deltas);
    }

    private static void matchUnambiguousRenames(List<TheatrePackageEntry> removed, List<TheatrePackageEntry> added,
                                                List<TheatrePackageDelta> deltas) {
        ArrayList<TheatrePackageEntry> matchedRemoved = new ArrayList<>();
        ArrayList<TheatrePackageEntry> matchedAdded = new ArrayList<>();
        for (TheatrePackageEntry oldEntry : removed) {
            List<TheatrePackageEntry> candidates = added.stream()
                    .filter(candidate -> candidate.kind() == oldEntry.kind())
                    .filter(candidate -> candidate.sha256().equals(oldEntry.sha256()))
                    .toList();
            if (candidates.size() == 1) {
                TheatrePackageEntry candidate = candidates.getFirst();
                long reverseMatches = removed.stream()
                        .filter(other -> other.kind() == candidate.kind())
                        .filter(other -> other.sha256().equals(candidate.sha256())).count();
                if (reverseMatches == 1) {
                    deltas.add(new TheatrePackageDelta(TheatrePackageDeltaStatus.RENAMED, oldEntry, candidate,
                            "Mismo contenido y tipo con una única ruta candidata."));
                    matchedRemoved.add(oldEntry);
                    matchedAdded.add(candidate);
                }
            }
        }
        removed.removeAll(matchedRemoved);
        added.removeAll(matchedAdded);
    }

    private static boolean sameContent(TheatrePackageEntry left, TheatrePackageEntry right) {
        return left.kind() == right.kind() && left.sha256().equals(right.sha256())
                && left.size() == right.size() && left.metadata().equals(right.metadata());
    }

    private static Map<String, TheatrePackageEntry> byId(List<TheatrePackageEntry> entries) {
        LinkedHashMap<String, TheatrePackageEntry> map = new LinkedHashMap<>();
        entries.forEach(entry -> map.put(entry.logicalId(), entry));
        return map;
    }

    private static TheatrePackageEntry synthetic(TheatrePackageInventory inventory) {
        return new TheatrePackageEntry("package:" + inventory.packageId(),
                com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind.OTHER,
                "docupodcast-theatre.json", "0".repeat(64), 0, Map.of());
    }
}
