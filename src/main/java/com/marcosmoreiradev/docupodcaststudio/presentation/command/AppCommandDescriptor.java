package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable catalog entry for one application command. */
public record AppCommandDescriptor(
        AppCommandId id,
        String label,
        String shortLabel,
        String description,
        AppCommandOwner owner,
        AppCommandSurface primarySurface,
        Set<AppCommandSurface> allowedSurfaces,
        boolean implemented,
        boolean visibleByDefault
) {
    public AppCommandDescriptor {
        Objects.requireNonNull(id, "id");
        label = requireText(label, "label");
        shortLabel = requireText(shortLabel, "shortLabel");
        description = requireText(description, "description");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(primarySurface, "primarySurface");
        Objects.requireNonNull(allowedSurfaces, "allowedSurfaces");
        if (allowedSurfaces.isEmpty()) {
            throw new IllegalArgumentException("allowedSurfaces no puede estar vacio.");
        }
        if (!allowedSurfaces.contains(primarySurface)) {
            EnumSet<AppCommandSurface> copy = EnumSet.copyOf(allowedSurfaces);
            copy.add(primarySurface);
            allowedSurfaces = copy;
        }
        allowedSurfaces = Set.copyOf(allowedSurfaces);
        if (visibleByDefault && !implemented) {
            throw new IllegalArgumentException("Un comando visible debe estar implementado: " + id);
        }
    }

    public boolean allowedOn(AppCommandSurface surface) {
        return allowedSurfaces.contains(Objects.requireNonNull(surface, "surface"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacio.");
        }
        return value.strip();
    }
}
