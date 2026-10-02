package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves presentation-only text bounds without mutating canonical PDF geometry. */
public final class PdfTextVisualBoundsResolver {
    public static final String OCR_WORD_BBOXES_ATTRIBUTE = "textGeometryWordBboxes";
    public static final String OCR_WORD_COUNT_ATTRIBUTE = "textGeometryWordCount";
    public static final String TIGHT_BBOX_ATTRIBUTE = "textTightBBox";
    public static final String SOURCE_ATTRIBUTE = "textVisualBoundsSource";
    private static final Set<PdfRegionType> TEXTUAL_TYPES = Set.of(
            PdfRegionType.TITLE, PdfRegionType.HEADING, PdfRegionType.SUBHEADING,
            PdfRegionType.PARAGRAPH, PdfRegionType.LIST, PdfRegionType.SIDEBAR,
            PdfRegionType.CAPTION, PdfRegionType.HEADER, PdfRegionType.FOOTER,
            PdfRegionType.PAGE_NUMBER);

    public Resolution resolve(PdfRegion region, PdfPageRegion semanticBox) {
        if (region == null || semanticBox == null || !textual(region)) {
            return new Resolution(semanticBox, Source.SPECIALIZED, 0);
        }
        String encodedWords = region.attributes().getOrDefault(
                OCR_WORD_BBOXES_ATTRIBUTE, "");
        List<PdfPageRegion> words = parseBoxes(encodedWords, semanticBox);
        if (words.isEmpty()) {
            return new Resolution(semanticBox, Source.SEMANTIC_FALLBACK, 0);
        }
        return new Resolution(union(words), Source.OCR_WORDS, words.size());
    }

    public static boolean textual(PdfRegion region) {
        if (!textualType(region)) return false;
        String route = region.attributes().getOrDefault("contentRoute", "");
        return !"VLM_MIXED".equals(route) && !"VLM_STRUCTURED".equals(route);
    }

    public static boolean textualType(PdfRegion region) {
        return region != null && TEXTUAL_TYPES.contains(region.effectiveType());
    }

    static List<PdfPageRegion> parseBoxes(String encoded, PdfPageRegion pageReference) {
        if (encoded == null || encoded.isBlank() || pageReference == null) return List.of();
        java.util.ArrayList<PdfPageRegion> result = new java.util.ArrayList<>();
        try {
            for (String value : encoded.split(";")) {
                String[] coordinates = value.split(",", 4);
                if (coordinates.length != 4) return List.of();
                PdfPageRegion box = new PdfPageRegion(pageReference.pageNumber(),
                        Double.parseDouble(coordinates[0]),
                        Double.parseDouble(coordinates[1]),
                        Double.parseDouble(coordinates[2]),
                        Double.parseDouble(coordinates[3]),
                        pageReference.pageWidthPoints(),
                        pageReference.pageHeightPoints());
                if (box.xMinPoints() < 0 || box.yMinPoints() < 0
                        || box.xMaxPoints() > pageReference.pageWidthPoints()
                        || box.yMaxPoints() > pageReference.pageHeightPoints()) return List.of();
                result.add(box);
            }
            return List.copyOf(result);
        } catch (IllegalArgumentException malformed) {
            return List.of();
        }
    }

    static PdfPageRegion union(List<PdfPageRegion> boxes) {
        PdfPageRegion first = boxes.getFirst();
        return new PdfPageRegion(first.pageNumber(),
                boxes.stream().mapToDouble(PdfPageRegion::xMinPoints).min().orElse(first.xMinPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::yMinPoints).min().orElse(first.yMinPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::xMaxPoints).max().orElse(first.xMaxPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(first.yMaxPoints()),
                first.pageWidthPoints(), first.pageHeightPoints());
    }

    public enum Source {
        OCR_WORDS,
        SEMANTIC_FALLBACK,
        SPECIALIZED
    }

    public record Resolution(PdfPageRegion tightTextBBox, Source source,
                             int matchedWordCount) {
        public Resolution {
            source = source == null ? Source.SEMANTIC_FALLBACK : source;
            matchedWordCount = Math.max(0, matchedWordCount);
        }

        public String diagnostic() {
            return String.format(Locale.ROOT,
                    "TEXT_VISUAL_BOUNDS_SOURCE=%s matchedWords=%d tightBBox=%s",
                    source, matchedWordCount, tightTextBBox);
        }
    }
}
