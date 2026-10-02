package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeTheatreRefreshPlanUseCaseTest {
    private final ComputeTheatreRefreshPlanUseCase useCase = new ComputeTheatreRefreshPlanUseCase();

    @Test
    void identicalInventoryIsANoOp() {
        TheatrePackageInventory inventory = inventory(List.of(entry("character:narrador:view:frontal", "a.png", "1")));
        TheatreImportState state = TheatreImportState.fromInventory(inventory, Instant.EPOCH, null);

        var plan = useCase.compute(state, inventory);

        assertTrue(plan.isNoOp());
        assertEquals(1, plan.count(TheatrePackageDeltaStatus.UNCHANGED));
        assertEquals(0, plan.count(TheatrePackageDeltaStatus.MODIFIED));
    }

    @Test
    void classifiesNewModifiedAndMissingWithoutDeleting() {
        TheatrePackageInventory before = inventory(List.of(
                entry("character:narrador:view:frontal", "narrador.png", "1"),
                entry("backdrop:plaza", "plaza.png", "2")));
        TheatreImportState state = TheatreImportState.fromInventory(before, Instant.EPOCH, null);
        TheatrePackageInventory after = inventory(List.of(
                entry("character:narrador:view:frontal", "narrador.png", "3"),
                entry("character:concha:view:frontal", "concha.png", "4")));

        var plan = useCase.compute(state, after);

        assertEquals(1, plan.count(TheatrePackageDeltaStatus.NEW));
        assertEquals(1, plan.count(TheatrePackageDeltaStatus.MODIFIED));
        assertEquals(1, plan.count(TheatrePackageDeltaStatus.MISSING_SOURCE_RETAINED));
        assertTrue(plan.hasEffectiveChanges());
    }

    @Test
    void recognizesOnlyUnambiguousRename() {
        TheatrePackageInventory before = inventory(List.of(entry("backdrop:old", "old.png", "9")));
        TheatrePackageInventory after = inventory(List.of(entry("backdrop:new", "new.png", "9")));

        var plan = useCase.compute(TheatreImportState.fromInventory(before, Instant.EPOCH, null), after);

        assertEquals(1, plan.count(TheatrePackageDeltaStatus.RENAMED));
        assertEquals(0, plan.count(TheatrePackageDeltaStatus.NEW));
    }

    private static TheatrePackageInventory inventory(List<TheatrePackageEntry> entries) {
        return TheatrePackageInventory.create(1, "obra", "1", "D:/obra", entries);
    }

    private static TheatrePackageEntry entry(String id, String path, String hashSeed) {
        return new TheatrePackageEntry(id, TheatrePackageAssetKind.BACKDROP, path,
                hashSeed.repeat(64), 10, Map.of());
    }
}
