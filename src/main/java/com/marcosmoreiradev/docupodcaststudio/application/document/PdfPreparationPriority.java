package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Queue priority; lower rank executes first. */
public enum PdfPreparationPriority {
    URGENT(0),
    HIGH(1),
    NORMAL(2),
    BACKGROUND(3);

    private final int rank;

    PdfPreparationPriority(int rank) {
        this.rank = rank;
    }

    int rank() {
        return rank;
    }
}
