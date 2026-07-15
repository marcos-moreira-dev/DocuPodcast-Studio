package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Checks what the example will operationally provide before the user creates it. */
public final class InspectExampleProjectReadinessUseCase {
    public ExampleProjectReadinessReport inspect(ExampleProjectDescriptor example) {
        Objects.requireNonNull(example, "example");
        ArrayList<String> warnings = new ArrayList<>();
        boolean sourceAvailable = resourceExists(example.sourceDocumentResource());
        if (!sourceAvailable) {
            warnings.add("No se encontró el Word demo incluido.");
        }
        Set<String> assetNames = new HashSet<>();
        for (ExampleAssetDescriptor asset : example.assets()) {
            assetNames.add(asset.fileName());
            if (!resourceExists(asset.resourcePath())) {
                warnings.add("Falta asset demo: " + asset.fileName() + ".");
            }
        }
        int unresolved = 0;
        for (ExampleVisualBindingDescriptor binding : example.visualBindings()) {
            if (!assetNames.contains(binding.assetFileName())) {
                unresolved++;
            }
        }
        if (unresolved > 0) {
            warnings.add(unresolved + " asociaciones visuales apuntan a imágenes no incluidas.");
        }
        if (!example.hasTheatreMarkdown() && !example.assets().isEmpty() && example.visualBindings().isEmpty()) {
            warnings.add("El ejemplo copia imágenes, pero todavía no trae asociaciones automáticas.");
        }
        return new ExampleProjectReadinessReport(
                example.id(),
                sourceAvailable,
                example.assets().size(),
                example.visualBindings().size(),
                unresolved,
                warnings);
    }

    private static boolean resourceExists(String resourcePath) {
        String normalized = resourcePath == null ? "" : resourcePath.strip();
        if (normalized.isBlank()) {
            return false;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return InspectExampleProjectReadinessUseCase.class.getResource(normalized) != null;
    }
}
