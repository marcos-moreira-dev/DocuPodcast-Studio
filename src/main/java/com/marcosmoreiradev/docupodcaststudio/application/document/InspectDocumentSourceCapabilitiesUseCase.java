package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

/** Reports capabilities for the currently imported source document. */
public final class InspectDocumentSourceCapabilitiesUseCase {
    public DocumentSourceCapabilities inspect(ReadableDocument document) {
        return DocumentSourceCapabilities.forFormat(document == null ? SourceDocumentFormat.UNKNOWN : document.format());
    }

    public DocumentSourceCapabilities inspect(SourceDocumentFormat format) {
        return DocumentSourceCapabilities.forFormat(format);
    }
}
