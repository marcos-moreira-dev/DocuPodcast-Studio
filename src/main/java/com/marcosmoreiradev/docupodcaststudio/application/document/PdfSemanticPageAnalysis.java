package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validated, provider-neutral result of reading one complete page visually. */
public record PdfSemanticPageAnalysis(
        int reportedPageNumber,
        String language,
        PdfPageRole pageRole,
        List<Element> elements,
        double confidence,
        List<String> uncertainties
) {
    public PdfSemanticPageAnalysis {
        reportedPageNumber = Math.max(0, reportedPageNumber);
        language = language == null || language.isBlank() ? "und" : language.strip();
        pageRole = Objects.requireNonNullElse(pageRole, PdfPageRole.UNKNOWN);
        elements = elements == null ? List.of() : List.copyOf(elements);
        confidence = finiteUnit(confidence);
        uncertainties = normalizedStrings(uncertainties);
    }

    public record Element(
            String responseId,
            int readingOrder,
            PdfRegionType type,
            NormalizedBox box,
            String sourceText,
            String narrationText,
            PdfNarratability narratability,
            double confidence,
            List<String> uncertainties,
            Map<String, String> attributes
    ) {
        public Element {
            responseId = responseId == null ? "" : responseId.strip();
            readingOrder = Math.max(0, readingOrder);
            type = Objects.requireNonNullElse(type, PdfRegionType.UNKNOWN);
            box = Objects.requireNonNull(box, "box");
            sourceText = sourceText == null ? "" : sourceText.strip();
            narrationText = narrationText == null ? "" : narrationText.strip();
            narratability = Objects.requireNonNullElse(
                    narratability, PdfNarratability.UNCERTAIN);
            confidence = finiteUnit(confidence);
            uncertainties = normalizedStrings(uncertainties);
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }

    /** Coordinates normalized to the closed 0..1000 page raster space. */
    public record NormalizedBox(double xMin, double yMin,
                                double xMax, double yMax) {
        public NormalizedBox {
            if (!Double.isFinite(xMin) || !Double.isFinite(yMin)
                    || !Double.isFinite(xMax) || !Double.isFinite(yMax)) {
                throw new IllegalArgumentException("semantic bbox must be finite");
            }
        }

        public boolean positive() {
            return xMax > xMin && yMax > yMin;
        }

        public boolean insideCanonicalRange() {
            return xMin >= 0.0 && yMin >= 0.0
                    && xMax <= 1000.0 && yMax <= 1000.0;
        }

        public NormalizedBox clamped() {
            double left = clamp(xMin);
            double top = clamp(yMin);
            double right = clamp(xMax);
            double bottom = clamp(yMax);
            return new NormalizedBox(left, top, right, bottom);
        }

        private static double clamp(double value) {
            return Math.max(0.0, Math.min(1000.0, value));
        }
    }

    private static double finiteUnit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    private static List<String> normalizedStrings(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
    }
}
