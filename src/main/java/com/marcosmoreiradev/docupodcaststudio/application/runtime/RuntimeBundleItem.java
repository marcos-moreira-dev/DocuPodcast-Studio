package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** One expected folder or file in the portable/installer runtime layout. */
public record RuntimeBundleItem(
        String id,
        RuntimePathRole role,
        RuntimeBundleItemKind kind,
        String relativePath,
        boolean requiredForPortableProduct,
        boolean includedAsPlaceholder,
        String purpose,
        String preparationHint
) {
    public RuntimeBundleItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(kind, "kind");
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        purpose = purpose == null ? "" : purpose.trim();
        preparationHint = preparationHint == null ? "" : preparationHint.trim();
    }

    public Path resolve(ApplicationRuntimeLayout layout) {
        Objects.requireNonNull(layout, "layout");
        return layout.resolveBundled(relativePath);
    }

    public boolean existsIn(ApplicationRuntimeLayout layout) {
        Path path = resolve(layout);
        return kind == RuntimeBundleItemKind.DIRECTORY ? Files.isDirectory(path) : Files.isRegularFile(path);
    }
}
