package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;
import java.util.Objects;

/** Productization manifest for tools/models/scripts that travel with the app or portable folder. */
public record RuntimeBundleManifest(ApplicationRuntimeLayout layout, List<RuntimeBundleItem> items) {
    public RuntimeBundleManifest {
        Objects.requireNonNull(layout, "layout");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public List<RuntimeBundleItem> requiredForPortableProduct() {
        return items.stream().filter(RuntimeBundleItem::requiredForPortableProduct).toList();
    }

    public List<RuntimeBundleItem> missingRequiredItems() {
        return requiredForPortableProduct().stream()
                .filter(item -> !item.existsIn(layout))
                .toList();
    }

    public boolean portableProductReady() {
        return missingRequiredItems().isEmpty();
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# DocuPodcast Studio - Runtime bundle manifest\n\n");
        builder.append("Application root: `").append(layout.applicationRoot()).append("`\n\n");
        builder.append("| ID | Ruta | Tipo | Requerido portable | Placeholder | Proposito | Preparacion |\n");
        builder.append("|---|---|---|---:|---:|---|---|\n");
        for (RuntimeBundleItem item : items) {
            builder.append("| ").append(item.id())
                    .append(" | `").append(item.relativePath()).append("` | ").append(item.kind())
                    .append(" | ").append(item.requiredForPortableProduct() ? "si" : "no")
                    .append(" | ").append(item.includedAsPlaceholder() ? "si" : "no")
                    .append(" | ").append(escape(item.purpose()))
                    .append(" | ").append(escape(item.preparationHint()))
                    .append(" |\n");
        }
        return builder.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "/");
    }
}
