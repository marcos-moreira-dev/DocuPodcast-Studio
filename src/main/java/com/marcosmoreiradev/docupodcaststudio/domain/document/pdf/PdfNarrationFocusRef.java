package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Objects;

/** Geometry that must remain visibly focused for the complete duration of a narration cue. */
public record PdfNarrationFocusRef(
        int pageNumber,
        List<String> sourceRegionIds,
        List<FocusBox> boxes,
        String accessibleLabel
) {
    public PdfNarrationFocusRef {
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        sourceRegionIds = sourceRegionIds == null ? List.of() : sourceRegionIds.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        boxes = boxes == null ? List.of() : List.copyOf(boxes);
        if (boxes.isEmpty()) throw new IllegalArgumentException("At least one focus box is required");
        accessibleLabel = accessibleLabel == null || accessibleLabel.isBlank()
                ? "Zona narrada" : accessibleLabel.strip();
    }

    public record FocusBox(double xMin, double yMin, double xMax, double yMax) {
        public FocusBox {
            if (!Double.isFinite(xMin) || !Double.isFinite(yMin)
                    || !Double.isFinite(xMax) || !Double.isFinite(yMax)
                    || xMin < 0.0 || yMin < 0.0 || xMax <= xMin || yMax <= yMin) {
                throw new IllegalArgumentException("Invalid narration focus geometry");
            }
        }
    }
}
