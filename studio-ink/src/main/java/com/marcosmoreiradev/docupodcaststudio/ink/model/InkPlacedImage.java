package com.marcosmoreiradev.docupodcaststudio.ink.model;

/** Image object placed independently from the ink layer. */
public record InkPlacedImage(
        String id,
        String sourceAssetId,
        String sourcePath,
        String inlineImageData,
        String inlineOriginalImageData,
        double x,
        double y,
        double fitWidth,
        double height,
        double originalLayoutX,
        double originalLayoutY,
        double originalFitWidth,
        InkImageCrop crop) {

    public InkPlacedImage {
        id = id == null || id.isBlank() ? "IMG-" + Integer.toHexString(System.identityHashCode(new Object())) : id;
        sourceAssetId = sourceAssetId == null ? "" : sourceAssetId;
        sourcePath = sourcePath == null ? "" : sourcePath;
        inlineImageData = inlineImageData == null ? "" : inlineImageData;
        inlineOriginalImageData = inlineOriginalImageData == null ? "" : inlineOriginalImageData;
        x = finiteOrZero(x);
        y = finiteOrZero(y);
        fitWidth = Math.max(1.0, finiteOrZero(fitWidth));
        height = Math.max(1.0, finiteOrZero(height));
        originalLayoutX = finiteOrZero(originalLayoutX);
        originalLayoutY = finiteOrZero(originalLayoutY);
        originalFitWidth = Math.max(1.0, finiteOrZero(originalFitWidth));
        crop = crop == null ? InkImageCrop.none() : crop;
    }

    public double maxX() {
        return x + fitWidth;
    }

    public double maxY() {
        return y + height;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
