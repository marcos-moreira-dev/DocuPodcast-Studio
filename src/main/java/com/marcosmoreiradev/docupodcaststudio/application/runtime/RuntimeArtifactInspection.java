package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.List;

/** Human-readable inspection result for local model/tool artifacts. */
public record RuntimeArtifactInspection(
        String artifactId,
        String displayName,
        Path rootDirectory,
        boolean complete,
        List<String> present,
        List<String> missingRequired,
        List<String> missingOptional,
        String userMessage
) {
    public RuntimeArtifactInspection {
        artifactId = normalize(artifactId);
        displayName = normalize(displayName);
        present = List.copyOf(present == null ? List.of() : present);
        missingRequired = List.copyOf(missingRequired == null ? List.of() : missingRequired);
        missingOptional = List.copyOf(missingOptional == null ? List.of() : missingOptional);
        complete = missingRequired.isEmpty();
        userMessage = normalize(userMessage).isBlank()
                ? (complete ? displayName + " está completo." : displayName + " requiere archivos locales.")
                : normalize(userMessage);
    }

    public boolean blocked() {
        return !complete;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
