package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;

/**
 * Raised when a source file is recognized but does not meet the V1 intake contract.
 * For example, a PDF without native/extractable text must be rejected without OCR.
 */
public final class SourceDocumentRequirementException extends IOException {
    public SourceDocumentRequirementException(String message) {
        super(message);
    }
}
