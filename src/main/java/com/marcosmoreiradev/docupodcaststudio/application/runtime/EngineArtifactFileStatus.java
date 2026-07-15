package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.List;

/** Runtime audit row for one expected engine artifact in the local/app-image layout. */
public record EngineArtifactFileStatus(
        EngineArtifactDescriptor descriptor,
        boolean present,
        List<Path> matchedFiles,
        List<String> sha256Values,
        String action
) {
    public EngineArtifactFileStatus {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor is required");
        }
        matchedFiles = matchedFiles == null ? List.of() : List.copyOf(matchedFiles);
        sha256Values = sha256Values == null ? List.of() : List.copyOf(sha256Values);
        action = action == null ? "" : action.trim();
    }

    public boolean missingRequiredForFinalRc() {
        return descriptor.requiredForFinalRc() && !present;
    }

    public String humanState() {
        if (present) {
            return "OK";
        }
        return descriptor.requiredForFinalRc() ? "FALTA" : "OPCIONAL_NO_CONFIGURADO";
    }

    public String matchedFilesLabel() {
        if (matchedFiles.isEmpty()) {
            return "-";
        }
        return String.join(", ", matchedFiles.stream().map(Path::toString).toList());
    }

    public String checksumLabel() {
        if (sha256Values.isEmpty()) {
            return present ? "no calculado" : "-";
        }
        return String.join(", ", sha256Values);
    }

    public String toMarkdownRow() {
        return "| " + descriptor.displayName()
                + " | `" + descriptor.expectedPath() + "`"
                + " | " + humanState()
                + " | " + (descriptor.requiredForFinalRc() ? "sí" : "no")
                + " | " + checksumLabel()
                + " | " + action.replace('|', '/')
                + " |";
    }
}
