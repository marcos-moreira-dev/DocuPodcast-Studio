package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.Objects;

/** Descriptor used by the document intake brain before invoking source-specific importers. */
public record DocumentSourceDescriptor(
        Path path,
        DocumentSourceType type,
        boolean readOnly,
        boolean nativeTextRequired
) {
    public DocumentSourceDescriptor {
        path = Objects.requireNonNull(path, "path");
        type = Objects.requireNonNullElse(type, DocumentSourceType.UNKNOWN);
        readOnly = true;
        nativeTextRequired = type.requiresNativeText();
    }

    public static DocumentSourceDescriptor from(Path path) {
        DocumentSourceType type = DocumentSourceType.fromPath(path);
        return new DocumentSourceDescriptor(path, type, true, type.requiresNativeText());
    }

    public String displayName() {
        return type.format().displayName();
    }
}
