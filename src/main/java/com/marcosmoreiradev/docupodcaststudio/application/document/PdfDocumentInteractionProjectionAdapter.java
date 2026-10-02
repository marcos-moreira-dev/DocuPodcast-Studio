package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

/** PDF selection adapter. PreparedPdfPage/PdfRegion remain canonical upstream. */
public final class PdfDocumentInteractionProjectionAdapter
        implements DocumentInteractionProjectionAdapter<PreparedPdfSource, PdfRegionSelectionRef> {
    @Override
    public DocumentInteractionProjection project(
            PreparedPdfSource source,
            PdfRegionSelectionRef selectedRegion,
            String preferredNarrationSegmentId,
            DocumentProcessingScope requestedScope,
            boolean narrationAvailable,
            boolean audioCoverageAvailable) {
        boolean selected = source != null && selectedRegion != null;
        return new DocumentInteractionProjection(
                source == null ? DocumentInteractionProjection.SourceKind.NONE
                        : DocumentInteractionProjection.SourceKind.PDF,
                selected,
                selected ? preferredNarrationSegmentId : "",
                requestedScope,
                narrationAvailable,
                audioCoverageAvailable);
    }
}
