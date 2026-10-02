package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Unified intake facade returning the canonical source kind for each document format. */
public final class DocumentSourceImportService {
    private final ImportDocumentUseCase importDocumentUseCase;
    private final CreatePreparedPdfSessionWorkspaceUseCase createPreparedPdfWorkspace;

    public DocumentSourceImportService(ImportDocumentUseCase importDocumentUseCase,
                                       CreatePreparedPdfSessionWorkspaceUseCase createPreparedPdfWorkspace) {
        this.importDocumentUseCase = Objects.requireNonNull(importDocumentUseCase, "importDocumentUseCase");
        this.createPreparedPdfWorkspace =
                Objects.requireNonNull(createPreparedPdfWorkspace, "createPreparedPdfWorkspace");
    }

    public ProjectDocumentSource importSource(Path sourceFile) throws IOException {
        DocumentSourceDescriptor descriptor = DocumentSourceDescriptor.from(sourceFile);
        if (descriptor.type() == DocumentSourceType.UNKNOWN) {
            throw new IOException("Formato documental no compatible: " + sourceFile.getFileName());
        }
        if (descriptor.type() == DocumentSourceType.PDF_TEXT) {
            return createPreparedPdfWorkspace.create(descriptor.path(), titleFrom(descriptor.path()));
        }
        return new BlockDocumentSource(importDocumentUseCase.importDocument(descriptor.path()));
    }

    public ReadableDocument importBlockSource(Path sourceFile) throws IOException {
        ProjectDocumentSource source = importSource(sourceFile);
        if (source instanceof BlockDocumentSource block) return block.document();
        if (source instanceof PreparedPdfSource pdf) {
            createPreparedPdfWorkspace.closeIfSessionWorkspace(pdf);
        }
        throw new IOException("El PDF usa exclusivamente preparación V2 y no puede importarse como bloques.");
    }

    private static String titleFrom(Path source) {
        String name = source == null || source.getFileName() == null
                ? "Documento PDF" : source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
