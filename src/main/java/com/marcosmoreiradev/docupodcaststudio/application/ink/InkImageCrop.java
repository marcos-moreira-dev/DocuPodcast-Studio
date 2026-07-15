package com.marcosmoreiradev.docupodcaststudio.application.ink;

/** Optional crop metadata for an image placed in an ink workspace. */
public record InkImageCrop(boolean active, double originalX, double originalY,
                           double originalWidth, double originalHeight) {
    public InkImageCrop {
        originalX = finiteOrZero(originalX);
        originalY = finiteOrZero(originalY);
        originalWidth = Math.max(0.0, finiteOrZero(originalWidth));
        originalHeight = Math.max(0.0, finiteOrZero(originalHeight));
    }

    public static InkImageCrop none() {
        return new InkImageCrop(false, 0, 0, 0, 0);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
