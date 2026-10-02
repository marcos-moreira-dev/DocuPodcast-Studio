package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;

/** Parses and validates the compact semantic-page protocol. */
@FunctionalInterface
public interface PdfSemanticPageResponseParser {
    PdfSemanticPageAnalysis parse(String protocolOutput) throws IOException;
}
