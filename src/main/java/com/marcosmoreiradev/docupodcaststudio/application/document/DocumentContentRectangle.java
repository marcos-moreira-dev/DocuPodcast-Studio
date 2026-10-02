package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Rectangle in source-document points. */
public record DocumentContentRectangle(double xMin, double yMin, double xMax, double yMax) {
    public DocumentContentRectangle {
        if (!Double.isFinite(xMin) || !Double.isFinite(yMin)
                || !Double.isFinite(xMax) || !Double.isFinite(yMax)
                || xMax <= xMin || yMax <= yMin) {
            throw new IllegalArgumentException("content rectangle must be finite and positive");
        }
    }

    public static DocumentContentRectangle union(
            DocumentContentRectangle left, DocumentContentRectangle right) {
        return new DocumentContentRectangle(
                Math.min(left.xMin, right.xMin), Math.min(left.yMin, right.yMin),
                Math.max(left.xMax, right.xMax), Math.max(left.yMax, right.yMax));
    }
}
