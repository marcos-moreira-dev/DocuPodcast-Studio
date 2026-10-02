package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Literal text span attached to a PageMap node. */
public record PdfTextStructure(String literalText, int startOffset, int endOffset, int sentenceIndex) {
    public PdfTextStructure {
        literalText = literalText == null ? "" : literalText;
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        sentenceIndex = Math.max(-1, sentenceIndex);
    }
}
