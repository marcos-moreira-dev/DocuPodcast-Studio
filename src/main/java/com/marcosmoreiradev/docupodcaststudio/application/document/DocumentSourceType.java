package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Canonical source document types accepted by the V1 document intake pipeline. */
public enum DocumentSourceType {
    DOCX(SourceDocumentFormat.DOCX, true),
    MARKDOWN(SourceDocumentFormat.MARKDOWN, true),
    TXT(SourceDocumentFormat.TXT, true),
    PDF_TEXT(SourceDocumentFormat.PDF, true),
    UNKNOWN(SourceDocumentFormat.UNKNOWN, false);

    private final SourceDocumentFormat format;
    private final boolean readOnlySource;

    DocumentSourceType(SourceDocumentFormat format, boolean readOnlySource) {
        this.format = format;
        this.readOnlySource = readOnlySource;
    }

    public SourceDocumentFormat format() {
        return format;
    }

    public boolean readOnlySource() {
        return readOnlySource;
    }

    public boolean requiresNativeText() {
        return false;
    }

    public static DocumentSourceType fromPath(Path sourceFile) {
        String name = sourceFile == null || sourceFile.getFileName() == null
                ? ""
                : sourceFile.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".docx")) {
            return DOCX;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return MARKDOWN;
        }
        if (name.endsWith(".txt")) {
            return TXT;
        }
        if (name.endsWith(".pdf")) {
            return PDF_TEXT;
        }
        return UNKNOWN;
    }

    public static Optional<DocumentSourceType> supported(Path sourceFile) {
        DocumentSourceType type = fromPath(sourceFile);
        return type == UNKNOWN ? Optional.empty() : Optional.of(type);
    }
}
