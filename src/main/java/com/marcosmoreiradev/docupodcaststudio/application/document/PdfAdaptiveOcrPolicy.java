package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Selects Tesseract segmentation from observed page geometry without invoking another engine. */
public final class PdfAdaptiveOcrPolicy {
    public int pageSegmentationMode(PdfTextLayer nativeLayer) {
        if (nativeLayer == null || nativeLayer.lines().isEmpty()) return 3;
        List<PdfTextLine> lines = nativeLayer.lines();
        if (lines.size() <= 4) return 11;
        double pageWidth = lines.getFirst().region().pageWidthPoints();
        long left = lines.stream().filter(line -> line.region().xMinPoints() < pageWidth * 0.40).count();
        long right = lines.stream().filter(line -> line.region().xMinPoints() > pageWidth * 0.45).count();
        boolean columns = left >= 3 && right >= 3;
        if (columns) return 4;
        double occupied = lines.stream().mapToDouble(line ->
                (line.region().xMaxPoints() - line.region().xMinPoints())
                        * (line.region().yMaxPoints() - line.region().yMinPoints())).sum();
        double pageArea = lines.getFirst().region().pageWidthPoints()
                * lines.getFirst().region().pageHeightPoints();
        return occupied / Math.max(1.0, pageArea) < 0.08 ? 11 : 6;
    }
}
