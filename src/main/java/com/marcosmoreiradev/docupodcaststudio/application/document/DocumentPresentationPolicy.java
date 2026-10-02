package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfContentRoute;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Single authority for choosing the central presentation of documentary content. */
public final class DocumentPresentationPolicy {
    private DocumentPresentationPolicy() { }

    public static DocumentPresentationMode forWord(DocumentBlock block) {
        if (block == null) return DocumentPresentationMode.TEXT_RENDER;
        return switch (block.type()) {
            case TABLE_NOTICE, IMAGE_NOTICE -> DocumentPresentationMode.SOURCE_CAPTURE;
            // Word math currently has no lossless source-region capture contract. Keep
            // its established readable fallback until that source artifact exists.
            default -> DocumentPresentationMode.TEXT_RENDER;
        };
    }

    public static DocumentPresentationMode forPdf(
            DocumentContentKind kind, List<PdfRegion> sourceRegions,
            boolean specializedVisualAccepted) {
        if (specializedVisualAccepted || kind == DocumentContentKind.TABLE
                || kind == DocumentContentKind.EQUATION
                || kind == DocumentContentKind.IMAGE) {
            return DocumentPresentationMode.SOURCE_CAPTURE;
        }
        List<PdfRegion> regions = sourceRegions == null ? List.of() : sourceRegions;
        if (regions.isEmpty()) return DocumentPresentationMode.SOURCE_CAPTURE;
        return regions.stream().allMatch(DocumentPresentationPolicy::isPureTextPresentationCandidate)
                ? DocumentPresentationMode.TEXT_RENDER
                : DocumentPresentationMode.SOURCE_CAPTURE;
    }

    /** True only when the original PDF appearance adds no material semantics. */
    public static boolean isPureTextPresentationCandidate(PdfRegion region) {
        if (region == null || !pureTextType(region.effectiveType())) return false;
        Map<String, String> attributes = region.attributes();
        String explicitRoute = attributes.getOrDefault("contentRoute", "").strip();
        if (!explicitRoute.isBlank()) {
            try {
                if (PdfContentRoute.valueOf(explicitRoute) != PdfContentRoute.OCR_SAFE) return false;
            } catch (IllegalArgumentException invalid) {
                return false;
            }
        }
        String presentation = region.presentation().toLowerCase(Locale.ROOT);
        if (containsAny(presentation, "mixed", "visual", "structured", "source", "formula", "math")) {
            return false;
        }
        if (truthy(attributes, "hasMath") || truthy(attributes, "containsMath")
                || truthy(attributes, "structuredContent")
                || truthy(attributes, "visualContent")) return false;
        for (String key : List.of("mathSourceConvention", "formulaKind",
                "equationKind", "latex", "structuredContentKind",
                "visualObjectKind")) {
            String value = attributes.getOrDefault(key, "").strip();
            if (!value.isBlank() && !"none".equalsIgnoreCase(value)) return false;
        }
        return true;
    }

    private static boolean pureTextType(PdfRegionType type) {
        return switch (type) {
            case TITLE, HEADING, SUBHEADING, PARAGRAPH, LIST, SIDEBAR, CAPTION,
                    HEADER, FOOTER, PAGE_NUMBER -> true;
            case TABLE, MATH, IMAGE, CODE, UNKNOWN -> false;
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static boolean truthy(Map<String, String> attributes, String key) {
        return Boolean.parseBoolean(attributes.getOrDefault(key, "false"));
    }
}
