package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.Objects;
import java.util.stream.IntStream;

/** Inclusive, one-based interval over a real document unit. */
public record DocumentProcessingInterval(
        DocumentProcessingIntervalUnit unit,
        int start,
        int end
) {
    public DocumentProcessingInterval {
        unit = Objects.requireNonNull(unit, "unit");
        if (start < 1) {
            throw new IllegalArgumentException("interval start must be at least 1");
        }
        if (end < start) {
            throw new IllegalArgumentException("interval end must be greater than or equal to start");
        }
    }

    public static DocumentProcessingInterval pages(int start, int end, int pageCount) {
        if (pageCount < 1) {
            throw new IllegalArgumentException("PDF page count must be positive");
        }
        DocumentProcessingInterval interval = new DocumentProcessingInterval(
                DocumentProcessingIntervalUnit.PAGE, start, end);
        return interval.validatedAgainst(pageCount);
    }

    public static DocumentProcessingInterval blocks(int start, int end, int blockCount) {
        if (blockCount < 1) {
            throw new IllegalArgumentException("Word block count must be positive");
        }
        DocumentProcessingInterval interval = new DocumentProcessingInterval(
                DocumentProcessingIntervalUnit.BLOCK, start, end);
        return interval.validatedAgainst(blockCount);
    }

    public DocumentProcessingInterval validatedAgainst(int unitCount) {
        if (unitCount < 1) {
            throw new IllegalArgumentException("document unit count must be positive");
        }
        if (end > unitCount) {
            throw new IllegalArgumentException("interval end exceeds document unit count");
        }
        return this;
    }

    public java.util.List<Integer> inclusiveIndexes() {
        return IntStream.rangeClosed(start, end).boxed().toList();
    }

    public int size() {
        return end - start + 1;
    }
}
