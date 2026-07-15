package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** One human-readable requirement that a local model folder must satisfy. */
public record ModelRequirement(String label, List<String> acceptedSuffixesOrNames) {
    public ModelRequirement {
        label = normalize(label);
        if (label.isBlank()) {
            throw new IllegalArgumentException("requirement label is required");
        }
        acceptedSuffixesOrNames = List.copyOf(Objects.requireNonNullElse(acceptedSuffixesOrNames, List.of()));
        if (acceptedSuffixesOrNames.isEmpty()) {
            throw new IllegalArgumentException("at least one accepted file marker is required");
        }
    }

    public boolean matches(Path file) {
        if (file == null || file.getFileName() == null) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase();
        return acceptedSuffixesOrNames.stream()
                .map(ModelRequirement::normalize)
                .map(String::toLowerCase)
                .anyMatch(marker -> name.equals(marker) || name.endsWith(marker));
    }

    public String displayMarkers() {
        return String.join(" / ", acceptedSuffixesOrNames);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
