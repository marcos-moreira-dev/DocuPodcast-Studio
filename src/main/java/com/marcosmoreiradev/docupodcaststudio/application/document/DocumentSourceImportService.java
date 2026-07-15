package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Unified intake facade for DOCX, Markdown, TXT and native-text PDF sources. */
public final class DocumentSourceImportService {
    private final ImportDocumentUseCase importDocumentUseCase;

    public DocumentSourceImportService(ImportDocumentUseCase importDocumentUseCase) {
        this.importDocumentUseCase = Objects.requireNonNull(importDocumentUseCase, "importDocumentUseCase");
    }

    public ReadableDocument importSource(Path sourceFile) throws IOException {
        DocumentSourceDescriptor descriptor = DocumentSourceDescriptor.from(sourceFile);
        if (descriptor.type() == DocumentSourceType.UNKNOWN) {
            throw new IOException("Formato no compatible para Documento narrable V1: " + sourceFile.getFileName());
        }
        return importDocumentUseCase.importDocument(descriptor.path());
    }
}
