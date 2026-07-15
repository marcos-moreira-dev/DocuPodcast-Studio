package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import java.util.List;
import java.util.Objects;

/**
 * Inventory entry for a reusable GUI component.
 *
 * <p>T97 freezes this catalog before the strong GUI redesign so menu/ribbon/sidebar/workspace work
 * is built on shared components instead of ad-hoc JavaFX nodes or duplicated buttons.</p>
 */
public record GuiComponentContract(
        String componentName,
        String className,
        GuiComponentStatus status,
        GuiComponentSurface primarySurface,
        List<GuiComponentSurface> allowedSurfaces,
        String approvedUse,
        String forbiddenUse
) {
    public GuiComponentContract {
        componentName = requireText(componentName, "componentName");
        className = requireText(className, "className");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(primarySurface, "primarySurface");
        allowedSurfaces = List.copyOf(Objects.requireNonNull(allowedSurfaces, "allowedSurfaces"));
        if (!allowedSurfaces.contains(primarySurface)) {
            throw new IllegalArgumentException("La superficie principal debe estar en allowedSurfaces: " + componentName);
        }
        approvedUse = requireText(approvedUse, "approvedUse");
        forbiddenUse = requireText(forbiddenUse, "forbiddenUse");
    }

    public boolean canBeUsedOn(GuiComponentSurface surface) {
        return allowedSurfaces.contains(surface);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return value.trim();
    }
}
