package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import java.util.Locale;
import java.util.Optional;

/** PDF-space rectangle in points, using Poppler's top-left coordinate system. */
record PdfBox(double xMin, double yMin, double xMax, double yMax) {
    PdfBox {
        double left = Math.min(xMin, xMax);
        double right = Math.max(xMin, xMax);
        double top = Math.min(yMin, yMax);
        double bottom = Math.max(yMin, yMax);
        xMin = left;
        xMax = right;
        yMin = top;
        yMax = bottom;
    }

    boolean valid() {
        return Double.isFinite(xMin) && Double.isFinite(yMin) && Double.isFinite(xMax) && Double.isFinite(yMax)
                && xMax > xMin && yMax > yMin;
    }

    String compact() {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", xMin, yMin, xMax, yMax);
    }

    static Optional<PdfBox> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.strip().split(",");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            PdfBox box = new PdfBox(
                    Double.parseDouble(parts[0].strip()),
                    Double.parseDouble(parts[1].strip()),
                    Double.parseDouble(parts[2].strip()),
                    Double.parseDouble(parts[3].strip()));
            return box.valid() ? Optional.of(box) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
