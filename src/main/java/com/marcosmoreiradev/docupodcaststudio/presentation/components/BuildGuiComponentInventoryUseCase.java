package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Builds the real GUI component inventory used by final migration guardrails. */
public final class BuildGuiComponentInventoryUseCase {
    public GuiComponentInventoryReport build(GuiComponentCatalog catalog) {
        GuiComponentCatalog current = catalog == null ? GuiComponentCatalog.official() : catalog;
        LinkedHashMap<String, String> classifications = new LinkedHashMap<>();
        for (GuiComponentContract component : current.components()) {
            classifications.put(component.componentName(), classify(component));
        }
        return new GuiComponentInventoryReport(
                current.components(),
                classifications,
                count(classifications, "transversal"),
                count(classifications, "especializado-narrativo"),
                count(classifications, "especializado-teatral"),
                count(classifications, "soporte-documental"),
                count(classifications, "legacy-interno"));
    }

    private static String classify(GuiComponentContract component) {
        String name = (component.componentName() + " " + component.className()).toLowerCase(Locale.ROOT);
        if (component.status() == GuiComponentStatus.LEGACY_BRIDGE) {
            return "legacy-interno";
        }
        if (name.contains("theatre") || name.contains("teatro") || name.contains("intervencion")
                || name.contains("spatial")) {
            return "especializado-teatral";
        }
        if (name.contains("narrative")) {
            return "especializado-narrativo";
        }
        if (name.contains("document") || name.contains("source")) {
            return "soporte-documental";
        }
        return "transversal";
    }

    private static int count(Map<String, String> classifications, String expected) {
        return (int) classifications.values().stream().filter(expected::equals).count();
    }
}
