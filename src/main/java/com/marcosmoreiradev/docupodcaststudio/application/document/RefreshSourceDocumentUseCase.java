package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Re-imports the read-only source document from disk and compares it with the imported snapshot.
 * It never edits or overwrites the external source file: no edita ni sobrescribe el documento fuente.
 *
 * <p>T71 moves refresh to the unified DocumentSource pipeline so DOCX, native-text PDF,
 * Markdown/MD and TXT follow the same rules. A scanned PDF or any source that does not meet
 * the V1 intake contract is reported as unsupported instead of creating a partial document.</p>
 */
public final class RefreshSourceDocumentUseCase {
    private final DocumentSourceImportService documentSourceImportService;

    public RefreshSourceDocumentUseCase(DocumentSourceImportService documentSourceImportService) {
        this.documentSourceImportService = Objects.requireNonNull(documentSourceImportService, "documentSourceImportService");
    }

    public RefreshSourceDocumentResult refresh(ReadableDocument currentDocument) throws IOException {
        Objects.requireNonNull(currentDocument, "currentDocument");
        SourceDocumentSnapshot previous = SourceDocumentSnapshot.from(currentDocument);
        Path sourcePath = currentDocument.sourcePath();
        if (!Files.exists(sourcePath)) {
            return new RefreshSourceDocumentResult(null, SourceDocumentChangeReport.missing(previous));
        }
        try {
            ReadableDocument refreshed = documentSourceImportService.importSource(sourcePath);
            SourceDocumentChangeReport report = SourceDocumentChangeReport.compare(previous, SourceDocumentSnapshot.from(refreshed));
            return new RefreshSourceDocumentResult(refreshed, report);
        } catch (SourceDocumentRequirementException ex) {
            // Contract: unsupported native-source requirements are reported as UNSUPPORTED;
            // the current document is preserved and no partial replacement is created.
            return new RefreshSourceDocumentResult(null, SourceDocumentChangeReport.unsupported(previous, ex.getMessage()));
        }
    }
}
