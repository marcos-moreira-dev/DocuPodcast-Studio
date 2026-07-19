package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudySlideLayout;
import javafx.scene.paint.Color;

/** Product-level capabilities for a reusable ink and image composition workspace. */
public record InkCompositionProfile(
        double logicalWidth,
        double logicalHeight,
        Color initialBackground,
        boolean multipleImages,
        int undoLimit) {

    public InkCompositionProfile {
        logicalWidth = Math.max(1.0, logicalWidth);
        logicalHeight = Math.max(1.0, logicalHeight);
        initialBackground = initialBackground == null ? Color.WHITE : initialBackground;
        undoLimit = Math.max(1, undoLimit);
    }

    public static InkCompositionProfile documentaryIllustration() {
        return new InkCompositionProfile(DocumentStudySlideLayout.ILLUSTRATION_WIDTH,
                DocumentStudySlideLayout.ILLUSTRATION_HEIGHT, Color.WHITE, true, 20);
    }

    public double aspectRatio() {
        return logicalWidth / logicalHeight;
    }
}
