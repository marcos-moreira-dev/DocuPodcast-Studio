package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.List;

/** Aggregate visual readiness for narrative and theatre visual production. */
public record VisualReadiness(
        int totalFragments,
        int narratableFragments,
        int mainImageReadyCount,
        int bridgeImageReadyCount,
        int documentSupportCount,
        int theatreVisualCount,
        int globalVisualCount,
        int missingMainImageCount,
        int brokenAssetCount,
        List<String> blockers,
        List<String> warnings
) {
    public VisualReadiness {
        totalFragments = Math.max(0, totalFragments);
        narratableFragments = Math.max(0, narratableFragments);
        mainImageReadyCount = Math.max(0, mainImageReadyCount);
        bridgeImageReadyCount = Math.max(0, bridgeImageReadyCount);
        documentSupportCount = Math.max(0, documentSupportCount);
        theatreVisualCount = Math.max(0, theatreVisualCount);
        globalVisualCount = Math.max(0, globalVisualCount);
        missingMainImageCount = Math.max(0, missingMainImageCount);
        brokenAssetCount = Math.max(0, brokenAssetCount);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
