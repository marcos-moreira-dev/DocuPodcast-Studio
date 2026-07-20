package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
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
        DrawingProfile profile = DrawingFeatureCatalog.official()
                .require(DrawingFeatureCatalog.DOCUMENTARY_ILLUSTRATION);
        return from(profile, Color.WHITE, true);
    }

    public static InkCompositionProfile from(DrawingProfile profile, Color background, boolean multipleImages) {
        if (profile == null) throw new IllegalArgumentException("drawing profile is required");
        return new InkCompositionProfile(profile.logicalWidth(), profile.logicalHeight(), background,
                multipleImages, profile.historyLimit());
    }

    public double aspectRatio() {
        return logicalWidth / logicalHeight;
    }
}
