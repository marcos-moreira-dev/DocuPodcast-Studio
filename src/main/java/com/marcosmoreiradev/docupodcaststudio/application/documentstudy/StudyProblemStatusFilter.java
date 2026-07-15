package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

/** UI filter for saved technical-problem solution state. */
public enum StudyProblemStatusFilter {
    ALL("Todos"),
    UNSOLVED("Sin solucion"),
    WITH_TEXT("Con texto"),
    WITH_CANVAS("Con lienzo"),
    WITH_CROPS("Con crops");

    private final String displayName;

    StudyProblemStatusFilter(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
