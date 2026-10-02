package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Character selection inside a DOCX/TXT/Markdown block. */
public record BlockSelectionRef(String blockId, int startOffset, int endOffset)
        implements DocumentSelectionRef {
    public BlockSelectionRef {
        blockId = blockId == null ? "" : blockId.strip();
        if (blockId.isBlank()) throw new IllegalArgumentException("blockId is required");
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
    }
}
