package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

/** Shared geometry between the documentary illustration editor and exported slides. */
public final class DocumentStudySlideLayout {
    public static final int ILLUSTRATION_WIDTH = 1344;
    public static final int ILLUSTRATION_HEIGHT = 432;
    public static final double ILLUSTRATION_ASPECT_RATIO = 28.0 / 9.0;
    public static final double ILLUSTRATION_WIDTH_FRACTION = 0.80;

    private DocumentStudySlideLayout() {
    }

    public static int illustrationWidth(int availableWidth) {
        return Math.max(1, (int) Math.round(Math.max(1, availableWidth) * ILLUSTRATION_WIDTH_FRACTION));
    }

    public static int illustrationHeight(int illustrationWidth, int slideHeight) {
        int byRatio = Math.max(1, (int) Math.round(illustrationWidth / ILLUSTRATION_ASPECT_RATIO));
        return Math.min(byRatio, Math.max(1, (int) Math.round(slideHeight * 0.45)));
    }
}
