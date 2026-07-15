package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.util.List;
import java.util.Optional;

/** Catalog of examples available inside the application package. */
public interface ExampleProjectCatalog {
    List<ExampleProjectDescriptor> listExamples();

    default Optional<ExampleProjectDescriptor> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listExamples().stream()
                .filter(example -> example.id().equals(id.strip()))
                .findFirst();
    }
}
