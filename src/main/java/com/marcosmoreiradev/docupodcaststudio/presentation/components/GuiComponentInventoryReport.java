package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import java.util.List;
import java.util.Map;

/** Derived inventory summary for transversal and specialized GUI components. */
public record GuiComponentInventoryReport(
        List<GuiComponentContract> components,
        Map<String, String> classifications,
        int transversalCount,
        int narrativeSpecializedCount,
        int theatreSpecializedCount,
        int documentSupportCount,
        int legacyInternalCount
) {
    public GuiComponentInventoryReport {
        components = components == null ? List.of() : List.copyOf(components);
        classifications = classifications == null ? Map.of() : Map.copyOf(classifications);
        transversalCount = Math.max(0, transversalCount);
        narrativeSpecializedCount = Math.max(0, narrativeSpecializedCount);
        theatreSpecializedCount = Math.max(0, theatreSpecializedCount);
        documentSupportCount = Math.max(0, documentSupportCount);
        legacyInternalCount = Math.max(0, legacyInternalCount);
    }

    public int total() {
        return components.size();
    }
}
